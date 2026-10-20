import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import {
  availabilityClassifications,
  classificationLabels,
  classificationSymbols,
  downtimeTotal,
  formatHistoryTime,
  formatMinutes,
  type AttractionAvailability,
  type AvailabilityDetail,
  type AvailabilitySegment,
  type HistoricalDataset,
} from '@/features/history/domain/history';
import {
  useAvailabilityDetail,
  useAvailabilitySummary,
  useHistoryDatasets,
  useHistoryQuestion,
} from '@/features/history/hooks/useHistory';
import { Button } from '@/shared/ui/Button';

import styles from './HistoryPage.module.css';

const defaultQuestion = 'Which attraction had the worst uptime?';

export default function HistoryPage() {
  const datasets = useHistoryDatasets();
  const [params, setParams] = useSearchParams();
  const dataset = datasets.data?.find(
    (item) => item.id === params.get('datasetId'),
  );
  const selection = dataset
    ? {
        datasetId: dataset.id,
        from: params.get('from') ?? dataset.fromDate,
        to: params.get('to') ?? dataset.toDate,
      }
    : null;
  const summary = useAvailabilitySummary(selection);
  const attractionId =
    params.get('attractionId') ??
    summary.data?.attractions[0]?.attractionId ??
    '';
  const date = params.get('date') ?? selection?.to ?? '';
  const detail = useAvailabilityDetail(
    selection?.datasetId ?? '',
    attractionId,
    date,
  );
  const question = useHistoryQuestion();
  const [questionText, setQuestionText] = useState(defaultQuestion);

  useEffect(() => {
    if (params.get('datasetId') || !datasets.data?.length) return;
    const first =
      datasets.data.find((item) => item.state === 'COMPLETED') ??
      datasets.data[0];
    setParams(
      {
        datasetId: first.id,
        from: first.fromDate,
        to: first.toDate,
        date: first.toDate,
      },
      { replace: true },
    );
  }, [datasets.data, params, setParams]);

  useEffect(() => {
    const citation = params.get('citation');
    if (!citation || !detail.data) return;
    document.getElementById(citation)?.scrollIntoView({ block: 'center' });
  }, [detail.data, params]);

  function updateParams(updates: Record<string, string>) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      for (const [key, value] of Object.entries(updates)) next.set(key, value);
      next.delete('citation');
      return next;
    });
  }

  function chooseDataset(next: HistoricalDataset) {
    setParams({
      datasetId: next.id,
      from: next.fromDate,
      to: next.toDate,
      date: next.toDate,
    });
    question.reset();
  }

  function submitQuestion(event: FormEvent) {
    event.preventDefault();
    if (!selection || !questionText.trim()) return;
    question.mutate({ ...selection, question: questionText.trim() });
  }

  return (
    <section className={styles.page} aria-labelledby="history-heading">
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>
            Control Tower · Historical intelligence
          </p>
          <h1 id="history-heading">Operating history</h1>
          <p className={styles.lede}>
            Inspect server-calculated attraction availability without changing
            the live workspace.
          </p>
        </div>
        <Link className={styles.docsLink} to="/docs/operating-history">
          Definitions and citation guide
        </Link>
      </header>

      <div className={styles.syntheticNotice} role="note">
        <strong>Synthetic historical dataset</strong>
        <span>
          Replay data for demonstration and inspection. It is never activated as
          live.
        </span>
      </div>

      {datasets.isPending ? (
        <p role="status">Loading historical datasets</p>
      ) : null}
      {datasets.isError ? (
        <ErrorState
          label="Unable to load historical datasets."
          retry={() => datasets.refetch()}
        />
      ) : null}

      {dataset && selection ? (
        <>
          <section
            className={styles.controls}
            aria-labelledby="history-filters-heading"
          >
            <h2 id="history-filters-heading">Dataset and date range</h2>
            <div className={styles.controlGrid}>
              <label>
                Dataset
                <select
                  value={dataset.id}
                  onChange={(event) => {
                    const next = datasets.data?.find(
                      (item) => item.id === event.target.value,
                    );
                    if (next) chooseDataset(next);
                  }}
                >
                  {datasets.data?.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.id} · v{item.version} · {item.state.toLowerCase()}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                From
                <input
                  type="date"
                  min={dataset.fromDate}
                  max={selection.to}
                  value={selection.from}
                  onChange={(event) =>
                    updateParams({ from: event.target.value })
                  }
                />
              </label>
              <label>
                To
                <input
                  type="date"
                  min={selection.from}
                  max={dataset.toDate}
                  value={selection.to}
                  onChange={(event) => updateParams({ to: event.target.value })}
                />
              </label>
            </div>
            <p className={styles.meta}>
              Available {dataset.fromDate}–{dataset.toDate} · seed{' '}
              {dataset.seed} · calculations use the park timezone returned by
              the API.
            </p>
          </section>

          {summary.isPending ? (
            <p role="status">Calculating availability comparison</p>
          ) : null}
          {summary.isError ? (
            <ErrorState
              label="Unable to load availability."
              retry={() => summary.refetch()}
            />
          ) : null}
          {summary.data ? (
            <>
              <Comparison
                attractions={summary.data.attractions}
                selectedId={attractionId}
                onSelect={(id) => updateParams({ attractionId: id })}
              />
              <DetailControls
                attractions={summary.data.attractions}
                attractionId={attractionId}
                date={date}
                dataset={dataset}
                onChange={updateParams}
              />
            </>
          ) : null}

          {detail.isPending ? (
            <p role="status">Loading daily attraction timeline</p>
          ) : null}
          {detail.isError ? (
            <ErrorState
              label="Unable to load the daily timeline."
              retry={() => detail.refetch()}
            />
          ) : null}
          {detail.data ? <DailyDetail detail={detail.data} /> : null}

          <section
            className={styles.questionPanel}
            aria-labelledby="question-heading"
          >
            <div>
              <h2 id="question-heading">Ask the curated history</h2>
              <p className={styles.meta}>
                Answers are deterministic, use only this availability
                projection, and cite source segments. No external model is used.
              </p>
            </div>
            <form className={styles.questionForm} onSubmit={submitQuestion}>
              <label htmlFor="history-question">Question</label>
              <div className={styles.questionRow}>
                <input
                  id="history-question"
                  value={questionText}
                  onChange={(event) => setQuestionText(event.target.value)}
                  required
                />
                <Button type="submit" disabled={question.isPending}>
                  {question.isPending ? 'Answering…' : 'Ask'}
                </Button>
              </div>
            </form>
            {question.isError ? (
              <p role="alert">Unable to answer this question.</p>
            ) : null}
            {question.data ? (
              <div
                className={styles.answer}
                role="status"
                aria-label="Historical answer"
              >
                <p>
                  <strong>Answer:</strong> {question.data.answer}
                </p>
                {question.data.claims.map((claim) => (
                  <div key={claim.text}>
                    <p>{claim.text}</p>
                    <ul className={styles.citationLinks}>
                      {claim.citations.map((citation) => (
                        <li key={citation.id}>
                          <Link to={citation.controlTowerUrl}>
                            {citation.parkDate} · {citation.id}
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
                <p className={styles.meta}>
                  Provider: {question.data.provider}
                </p>
              </div>
            ) : null}
          </section>
        </>
      ) : null}
    </section>
  );
}

function Comparison({
  attractions,
  selectedId,
  onSelect,
}: {
  attractions: AttractionAvailability[];
  selectedId: string;
  onSelect: (id: string) => void;
}) {
  return (
    <section className={styles.panel} aria-labelledby="comparison-heading">
      <div className={styles.sectionHeading}>
        <div>
          <h2 id="comparison-heading">Ranked uptime</h2>
          <p className={styles.meta}>Lowest uptime is ranked first.</p>
        </div>
        <span className={styles.zoneLabel}>Server calculated</span>
      </div>
      <ol className={styles.bars}>
        {attractions.map((attraction, index) => (
          <li key={attraction.attractionId}>
            <button
              className={
                attraction.attractionId === selectedId
                  ? styles.selectedBar
                  : styles.bar
              }
              onClick={() => onSelect(attraction.attractionId)}
              aria-label={`Rank ${index + 1}, ${attraction.attractionName}, ${attraction.uptimePercentage.toFixed(2)} percent uptime`}
            >
              <span className={styles.barLabel}>
                <span>
                  {index + 1}. {attraction.attractionName}
                </span>
                <strong>{attraction.uptimePercentage.toFixed(2)}%</strong>
              </span>
              <svg
                viewBox="0 0 100 10"
                role="img"
                aria-hidden="true"
                preserveAspectRatio="none"
              >
                <rect
                  className={styles.barTrack}
                  width="100"
                  height="10"
                  rx="2"
                />
                <rect
                  className={styles.barFill}
                  width={Math.max(
                    0,
                    Math.min(100, attraction.uptimePercentage),
                  )}
                  height="10"
                  rx="2"
                />
              </svg>
            </button>
          </li>
        ))}
      </ol>
      <div className={styles.tableScroll}>
        <table>
          <caption>Accessible ranked uptime comparison</caption>
          <thead>
            <tr>
              <th scope="col">Rank</th>
              <th scope="col">Attraction</th>
              <th scope="col">Uptime</th>
              <th scope="col">Operating / scheduled</th>
              <th scope="col">Degraded</th>
              <th scope="col">Downtime</th>
              <th scope="col">Longest outage</th>
            </tr>
          </thead>
          <tbody>
            {attractions.map((attraction, index) => (
              <tr key={attraction.attractionId}>
                <td>{index + 1}</td>
                <th scope="row">{attraction.attractionName}</th>
                <td>{attraction.uptimePercentage.toFixed(2)}%</td>
                <td>
                  {formatMinutes(attraction.operatingMinutes)} /{' '}
                  {formatMinutes(attraction.scheduledMinutes)}
                </td>
                <td>{formatMinutes(attraction.degradedMinutes)}</td>
                <td>
                  {formatMinutes(downtimeTotal(attraction.downtimeMinutes))}
                </td>
                <td>{formatMinutes(attraction.longestOutageMinutes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function DetailControls({
  attractions,
  attractionId,
  date,
  dataset,
  onChange,
}: {
  attractions: AttractionAvailability[];
  attractionId: string;
  date: string;
  dataset: HistoricalDataset;
  onChange: (updates: Record<string, string>) => void;
}) {
  return (
    <section className={styles.controls} aria-labelledby="daily-view-heading">
      <h2 id="daily-view-heading">Daily timeline selection</h2>
      <div className={styles.controlGrid}>
        <label>
          Attraction
          <select
            value={attractionId}
            onChange={(event) => onChange({ attractionId: event.target.value })}
          >
            {attractions.map((attraction) => (
              <option
                key={attraction.attractionId}
                value={attraction.attractionId}
              >
                {attraction.attractionName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Park date
          <input
            type="date"
            min={dataset.fromDate}
            max={dataset.toDate}
            value={date}
            onChange={(event) => onChange({ date: event.target.value })}
          />
        </label>
      </div>
    </section>
  );
}

function DailyDetail({ detail }: { detail: AvailabilityDetail }) {
  const total = Math.max(detail.scheduledMinutes, 1);
  const downtime = availabilityClassifications.filter(
    (classification) =>
      classification !== 'OPERATING' &&
      classification !== 'REDUCED_CAPACITY' &&
      detail.downtimeMinutes[classification] > 0,
  );

  return (
    <section className={styles.panel} aria-labelledby="timeline-heading">
      <div className={styles.sectionHeading}>
        <div>
          <h2 id="timeline-heading">
            {detail.attractionName} · {detail.parkDate}
          </h2>
          <p className={styles.meta}>
            Expected service{' '}
            {formatHistoryTime(detail.expectedStart, detail.zoneId)}–
            {formatHistoryTime(detail.expectedEnd, detail.zoneId)} ·{' '}
            {detail.zoneId}
          </p>
        </div>
        <strong>{detail.uptimePercentage.toFixed(2)}% uptime</strong>
      </div>

      {detail.plannedClosure ? (
        <p className={styles.plannedClosure}>
          Planned closure ·{' '}
          {detail.plannedClosureReason ?? 'No reason supplied'} · excluded from
          rankings
        </p>
      ) : (
        <>
          <div
            className={styles.timeline}
            role="group"
            aria-label={`Status timeline for ${detail.attractionName}. Each segment links to its citation row. Exact values are in the segment table.`}
          >
            {detail.segments.map((segment) => (
              <a
                key={segment.citationId}
                href={`#${segment.citationId}`}
                className={`${styles.timelineSegment} ${styles[`segment_${segment.classification}`]}`}
                style={{
                  flexBasis: `${(segment.durationMinutes / total) * 100}%`,
                }}
                aria-label={`${classificationLabels[segment.classification]}, ${formatMinutes(segment.durationMinutes)}, citation ${segment.citationId}`}
              >
                <span aria-hidden="true">
                  {classificationSymbols[segment.classification]}
                </span>
              </a>
            ))}
          </div>
          <ul className={styles.legend} aria-label="Timeline key">
            {[
              ...new Set(
                detail.segments.map((segment) => segment.classification),
              ),
            ].map((classification) => (
              <li key={classification}>
                <span aria-hidden="true">
                  {classificationSymbols[classification]}
                </span>{' '}
                {classificationLabels[classification]}
              </li>
            ))}
          </ul>
        </>
      )}

      <dl className={styles.totals}>
        <div>
          <dt>Operating</dt>
          <dd>{formatMinutes(detail.operatingMinutes)}</dd>
        </div>
        <div>
          <dt>Degraded operation</dt>
          <dd>{formatMinutes(detail.degradedMinutes)}</dd>
        </div>
        <div>
          <dt>Total downtime</dt>
          <dd>{formatMinutes(downtimeTotal(detail.downtimeMinutes))}</dd>
        </div>
        <div>
          <dt>Longest outage</dt>
          <dd>{formatMinutes(detail.longestOutageMinutes)}</dd>
        </div>
      </dl>

      {downtime.length > 0 ? (
        <section aria-labelledby="downtime-causes-heading">
          <h3 id="downtime-causes-heading">Downtime by cause</h3>
          <ul className={styles.causeList}>
            {downtime.map((classification) => (
              <li key={classification}>
                <span>{classificationLabels[classification]}</span>
                <strong>
                  {formatMinutes(detail.downtimeMinutes[classification])}
                </strong>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <div className={styles.tableScroll}>
        <table>
          <caption>Timeline segments and stable citations</caption>
          <thead>
            <tr>
              <th scope="col">State</th>
              <th scope="col">Start</th>
              <th scope="col">End</th>
              <th scope="col">Duration</th>
              <th scope="col">Reason</th>
              <th scope="col">Citation</th>
            </tr>
          </thead>
          <tbody>
            {detail.segments.map((segment) => (
              <SegmentRow
                key={segment.citationId}
                segment={segment}
                zoneId={detail.zoneId}
              />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function SegmentRow({
  segment,
  zoneId,
}: {
  segment: AvailabilitySegment;
  zoneId: string;
}) {
  return (
    <tr id={segment.citationId} className={styles.citationTarget}>
      <th scope="row">
        <span aria-hidden="true">
          {classificationSymbols[segment.classification]}
        </span>{' '}
        {classificationLabels[segment.classification]}
      </th>
      <td>{formatHistoryTime(segment.start, zoneId)}</td>
      <td>{formatHistoryTime(segment.end, zoneId)}</td>
      <td>{formatMinutes(segment.durationMinutes)}</td>
      <td>{segment.reason ?? '—'}</td>
      <td>
        <a href={`#${segment.citationId}`}>{segment.citationId}</a>
      </td>
    </tr>
  );
}

function ErrorState({ label, retry }: { label: string; retry: () => unknown }) {
  return (
    <div className={styles.error} role="alert">
      <p>{label}</p>
      <Button onClick={() => void retry()}>Retry</Button>
    </div>
  );
}
