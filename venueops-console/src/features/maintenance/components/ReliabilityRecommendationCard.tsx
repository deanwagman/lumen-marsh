import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { formatLabel } from '@/features/attractions/domain/formatters';
import {
  canCommandMaintenance,
} from '@/features/maintenance/domain/maintenance';
import { recommendationNextAction } from '@/features/maintenance/domain/maintenanceActions';
import {
  formatMaintenanceTime,
  formatObservationAge,
  formatRecommendationStatus,
  formatSignalValue,
  recommendationSeverityTone,
} from '@/features/maintenance/domain/maintenancePresentation';
import type { ReliabilityRecommendation } from '@/features/maintenance/domain/maintenance';
import { HelpLink } from '@/shared/ui/HelpLink';
import { StatusBadge } from '@/shared/ui/StatusBadge';
import { Button } from '@/shared/ui/Button';

import styles from './ReliabilityRecommendationCard.module.css';

export function ReliabilityRecommendationCard({
  recommendation,
  pending = false,
  error = null,
  onAccept,
  onDismiss,
}: {
  recommendation: ReliabilityRecommendation;
  pending?: boolean;
  error?: Error | null;
  onAccept?: (reason: string) => void;
  onDismiss?: (reason: string) => void;
}) {
  const { session } = useAuth();
  const canCommand = canCommandMaintenance(session);
  const pendingReview = recommendation.status === 'PENDING_REVIEW';
  const [mode, setMode] = useState<'idle' | 'accept' | 'dismiss'>('idle');
  const [reason, setReason] = useState('');
  const observed = formatSignalValue(recommendation);

  function submit(event: FormEvent) {
    event.preventDefault();
    if (mode === 'accept') {
      onAccept?.(reason.trim() || 'Inspection required based on simulated vibration evidence.');
    }
    if (mode === 'dismiss') {
      onDismiss?.(reason.trim());
    }
  }

  return (
    <article className={styles.card} aria-labelledby={`rec-${recommendation.recommendationId}`}>
      <header className={styles.header}>
        <div>
          <p className={styles.signal}>{formatLabel(recommendation.signalType)}</p>
          <h3 id={`rec-${recommendation.recommendationId}`} className={styles.asset}>
            {recommendation.assetCode}
          </h3>
        </div>
        <div className={styles.badges}>
          <StatusBadge
            tone={recommendationSeverityTone(recommendation.severity)}
            label={formatLabel(recommendation.severity)}
          />
          <StatusBadge tone="closed" label={formatRecommendationStatus(recommendation.status)} />
        </div>
      </header>
      <dl className={styles.readout}>
        <div>
          <dt>Observed</dt>
          <dd>{formatMaintenanceTime(recommendation.observedAt)}</dd>
        </div>
        <div>
          <dt>Data age</dt>
          <dd>{formatObservationAge(recommendation.observedAt)}</dd>
        </div>
        {observed ? (
          <div>
            <dt>Value</dt>
            <dd className={styles.mono}>{observed}</dd>
          </div>
        ) : null}
      </dl>
      {recommendation.evidence ? <p className={styles.evidence}>{recommendation.evidence}</p> : null}
      <p>
        <strong>Recommended action.</strong> {recommendation.recommendedAction}
      </p>
      <p className={styles.next}>{recommendationNextAction(recommendation.status)}</p>
      {recommendation.workOrderId ? (
        <p>
          <Link to={`/maintenance/work-orders/${recommendation.workOrderId}`}>Open work order</Link>
        </p>
      ) : null}
      <div className={styles.help}>
        <HelpLink
          article="maintenance"
          section="reliability"
          label="Help: reliability recommendation review"
        />
      </div>
      {pendingReview && canCommand ? (
        <div className={styles.actions}>
          <Button
            aria-pressed={mode === 'accept'}
            disabled={pending}
            onClick={() => {
              setMode('accept');
              setReason('Inspection required based on simulated vibration evidence.');
            }}
          >
            Accept recommendation
          </Button>
          <Button
            variant="ghost"
            aria-pressed={mode === 'dismiss'}
            disabled={pending}
            onClick={() => {
              setMode('dismiss');
              setReason('');
            }}
          >
            Dismiss
          </Button>
        </div>
      ) : null}
      {pendingReview && !canCommand ? (
        <p className={styles.muted}>Read-only. Maintenance command scope is required to accept or dismiss.</p>
      ) : null}
      {mode !== 'idle' && pendingReview ? (
        <form className={styles.form} onSubmit={submit}>
          <p>
            {mode === 'accept'
              ? 'Acceptance creates a corrective work order. Maintenance never reopens an attraction.'
              : 'Dismissal requires a short reason so the trail explains why no work order was created.'}
          </p>
          <label>
            {mode === 'accept' ? 'Reason' : 'Dismissal reason'}
            <textarea
              required={mode === 'dismiss'}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
          <div className={styles.actions}>
            <Button type="submit" disabled={pending || (mode === 'dismiss' && reason.trim().length < 3)}>
              {pending ? 'Sending…' : mode === 'accept' ? 'Create work order' : 'Confirm dismissal'}
            </Button>
            <Button type="button" variant="ghost" onClick={() => setMode('idle')}>
              Cancel
            </Button>
          </div>
        </form>
      ) : null}
      {error ? (
        <p className={styles.error} role="alert">
          {error.message}
        </p>
      ) : null}
    </article>
  );
}
