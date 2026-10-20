import { useMutation, useQuery } from '@tanstack/react-query';

import {
  askHistoryQuestion,
  availabilityDetailQuery,
  availabilitySummaryQuery,
  historyDatasetsQuery,
} from '@/features/history/api/HistoryQueries';
import type { HistorySelection } from '@/features/history/domain/history';
import { useApiClient } from '@/shared/api/useApiClient';

export function useHistoryDatasets() {
  const client = useApiClient();
  return useQuery(historyDatasetsQuery(client));
}

export function useAvailabilitySummary(selection: HistorySelection | null) {
  const client = useApiClient();
  return useQuery({
    ...availabilitySummaryQuery(
      client,
      selection ?? { datasetId: '', from: '', to: '' },
    ),
    enabled: selection !== null,
  });
}

export function useAvailabilityDetail(
  datasetId: string,
  attractionId: string,
  date: string,
) {
  const client = useApiClient();
  return useQuery({
    ...availabilityDetailQuery(client, datasetId, attractionId, date),
    enabled: Boolean(datasetId && attractionId && date),
  });
}

export function useHistoryQuestion() {
  const client = useApiClient();
  return useMutation({
    mutationFn: (input: HistorySelection & { question: string }) =>
      askHistoryQuestion(client, input),
  });
}
