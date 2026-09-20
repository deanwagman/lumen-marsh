import type { ApiClient } from '@/shared/api/client';
import type {
  FlowAttractionDetail,
  FlowOverview,
  FlowRecommendation,
  FlowRecommendationStatus,
  FlowRecommendationSeverity,
  FlowRecommendationType,
  PageResult,
} from '@/features/flow/domain/flow';

import {
  flowActivityListSchema,
  flowAttractionDetailSchema,
  flowOverviewSchema,
  flowRecommendationPageSchema,
  flowRecommendationSchema,
} from './flowSchemas';

export type FlowRecommendationFilters = {
  status?: FlowRecommendationStatus;
  severity?: FlowRecommendationSeverity;
  type?: FlowRecommendationType;
  attractionId?: string;
  page: number;
  size: number;
};

export const FlowQueries = {
  all: ['flow'] as const,
  overview: () => [...FlowQueries.all, 'overview'] as const,
  attraction: (id: string) => [...FlowQueries.all, 'attractions', id] as const,
  recommendations: (filters: FlowRecommendationFilters) =>
    [...FlowQueries.all, 'recommendations', filters] as const,
  recommendation: (id: string) => [...FlowQueries.all, 'recommendations', 'detail', id] as const,
  activity: (id: string) => [...FlowQueries.all, 'recommendations', id, 'activity'] as const,
};

function searchParams(filters: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value === undefined || value === '') {
      continue;
    }
    params.set(key, String(value));
  }
  const encoded = params.toString();
  return encoded ? `?${encoded}` : '';
}

export function flowOverviewQuery(client: ApiClient) {
  return {
    queryKey: FlowQueries.overview(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get('/api/v1/operator/flow/overview', flowOverviewSchema, { signal }),
  };
}

export function flowAttractionQuery(client: ApiClient, attractionId: string) {
  return {
    queryKey: FlowQueries.attraction(attractionId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/flow/attractions/${encodeURIComponent(attractionId)}`,
        flowAttractionDetailSchema,
        { signal },
      ),
  };
}

export function flowRecommendationListQuery(client: ApiClient, filters: FlowRecommendationFilters) {
  return {
    queryKey: FlowQueries.recommendations(filters),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/flow/recommendations${searchParams(filters)}`,
        flowRecommendationPageSchema,
        { signal },
      ),
  };
}

export function flowRecommendationQuery(client: ApiClient, recommendationId: string) {
  return {
    queryKey: FlowQueries.recommendation(recommendationId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/flow/recommendations/${encodeURIComponent(recommendationId)}`,
        flowRecommendationSchema,
        { signal },
      ),
  };
}

export function flowActivityQuery(client: ApiClient, recommendationId: string) {
  return {
    queryKey: FlowQueries.activity(recommendationId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/flow/recommendations/${encodeURIComponent(recommendationId)}/activity`,
        flowActivityListSchema,
        { signal },
      ),
  };
}

export type { FlowOverview, FlowAttractionDetail, FlowRecommendation, PageResult };
