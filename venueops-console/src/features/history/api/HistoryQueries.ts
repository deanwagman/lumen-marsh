import type { ApiClient } from '@/shared/api/client';
import type { HistorySelection } from '@/features/history/domain/history';

import {
  analyticsAnswerSchema,
  availabilityDetailSchema,
  availabilitySummarySchema,
  historicalDatasetListSchema,
} from './historySchemas';

export const HistoryQueries = {
  all: ['history'] as const,
  datasets: () => [...HistoryQueries.all, 'datasets'] as const,
  summary: (selection: HistorySelection) =>
    [...HistoryQueries.all, 'availability', selection] as const,
  detail: (datasetId: string, attractionId: string, date: string) =>
    [...HistoryQueries.all, 'detail', datasetId, attractionId, date] as const,
};

function queryString(values: Record<string, string>): string {
  return new URLSearchParams(values).toString();
}

export function historyDatasetsQuery(client: ApiClient) {
  return {
    queryKey: HistoryQueries.datasets(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        '/api/v1/operator/analytics/datasets',
        historicalDatasetListSchema,
        { signal },
      ),
  };
}

export function availabilitySummaryQuery(
  client: ApiClient,
  selection: HistorySelection,
) {
  return {
    queryKey: HistoryQueries.summary(selection),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/analytics/availability?${queryString(selection)}`,
        availabilitySummarySchema,
        { signal },
      ),
  };
}

export function availabilityDetailQuery(
  client: ApiClient,
  datasetId: string,
  attractionId: string,
  date: string,
) {
  return {
    queryKey: HistoryQueries.detail(datasetId, attractionId, date),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/analytics/availability/attractions/${encodeURIComponent(attractionId)}?${queryString({ datasetId, date })}`,
        availabilityDetailSchema,
        { signal },
      ),
  };
}

export function askHistoryQuestion(
  client: ApiClient,
  input: HistorySelection & { question: string },
) {
  return client.post(
    '/api/v1/operator/analytics/questions',
    analyticsAnswerSchema,
    input,
  );
}
