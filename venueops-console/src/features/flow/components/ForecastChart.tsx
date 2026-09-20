import type { FlowAttractionCard } from '@/features/flow/domain/flow';
import { forecastChartText } from '@/features/flow/domain/flow';

import styles from './ForecastChart.module.css';

export function ForecastChart({ card }: { card: FlowAttractionCard }) {
  const points = [
    { label: 'Now', value: card.calculatedWaitMinutes },
    { label: '15 min', value: card.predictedWaitMinutes15 },
    { label: '30 min', value: card.predictedWaitMinutes30 },
    { label: '60 min', value: card.predictedWaitMinutes60 },
  ];
  const numeric = points.map((point) => point.value).filter((value): value is number => value != null);
  const posted = card.postedWaitMinutes;
  const max = Math.max(5, ...(posted == null ? numeric : [...numeric, posted]));
  const width = 220;
  const height = 88;
  const pad = 16;
  const chartWidth = width - pad * 2;
  const chartHeight = height - pad * 2;

  function x(index: number): number {
    return pad + (chartWidth * index) / Math.max(1, points.length - 1);
  }

  function y(value: number): number {
    return pad + chartHeight - (value / max) * chartHeight;
  }

  const path = points
    .map((point, index) =>
      point.value == null ? null : `${index === 0 || points[index - 1]?.value == null ? 'M' : 'L'} ${x(index)} ${y(point.value)}`,
    )
    .filter(Boolean)
    .join(' ');

  return (
    <figure className={styles.chart}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label={forecastChartText(card)}
      >
        {posted != null ? (
          <line
            x1={pad}
            x2={width - pad}
            y1={y(posted)}
            y2={y(posted)}
            className={styles.posted}
          />
        ) : null}
        {path ? <path d={path} className={styles.line} fill="none" /> : null}
        {points.map((point, index) =>
          point.value == null ? null : (
            <g key={point.label}>
              <circle cx={x(index)} cy={y(point.value)} r={3.5} className={styles.point} />
              <text x={x(index)} y={height - 2} textAnchor="middle" className={styles.tick}>
                {point.label}
              </text>
              <text x={x(index)} y={y(point.value) - 6} textAnchor="middle" className={styles.value}>
                {point.value}
              </text>
            </g>
          ),
        )}
      </svg>
      <figcaption className={styles.caption}>{forecastChartText(card)}</figcaption>
    </figure>
  );
}
