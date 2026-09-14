import type { QueryClient } from '@tanstack/react-query';

import type { WeatherRecommendation } from '@/features/weather/domain/recommendation';
import type { ApiClient } from '@/shared/api/client';

import { weatherRecommendationListSchema } from './weatherRecommendationSchema';

export const WeatherRecommendationQueries = {
  all: ['weather-recommendations'] as const,
  list: () => [...WeatherRecommendationQueries.all, 'list'] as const,
};

export function weatherRecommendationListQuery(client: ApiClient) {
  return {
    queryKey: WeatherRecommendationQueries.list(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        '/api/v1/operator/weather/recommendations',
        weatherRecommendationListSchema,
        { signal },
      ),
  };
}

export function replaceWeatherRecommendations(
  queryClient: QueryClient,
  recommendations: WeatherRecommendation[],
): void {
  queryClient.setQueryData(WeatherRecommendationQueries.list(), sortRecommendations(recommendations));
}

export function applyWeatherRecommendation(
  queryClient: QueryClient,
  recommendation: WeatherRecommendation,
): void {
  queryClient.setQueryData<WeatherRecommendation[]>(
    WeatherRecommendationQueries.list(),
    (current) => {
      const list = current ?? [];
      const existing = list.find((item) => item.id === recommendation.id);
      if (existing && isOlderThan(recommendation, existing)) {
        return list;
      }

      return sortRecommendations([
        ...list.filter((item) => item.id !== recommendation.id),
        recommendation,
      ]);
    },
  );
}

function isOlderThan(next: WeatherRecommendation, current: WeatherRecommendation): boolean {
  if (next.sourceVersion < current.sourceVersion) {
    return true;
  }
  return next.sourceVersion === current.sourceVersion && next.version < current.version;
}

function sortRecommendations(recommendations: WeatherRecommendation[]): WeatherRecommendation[] {
  return [...recommendations].sort((left, right) => {
    if (left.status !== right.status) {
      return left.status === 'ACTIVE' ? -1 : 1;
    }
    return Date.parse(right.updatedAt) - Date.parse(left.updatedAt);
  });
}
