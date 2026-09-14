import { useQuery } from '@tanstack/react-query';

import { weatherRecommendationListQuery } from '@/features/weather/api/WeatherRecommendationQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useWeatherRecommendations() {
  const client = useApiClient();
  return useQuery(weatherRecommendationListQuery(client));
}
