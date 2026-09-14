import { useMutation, useQueryClient } from '@tanstack/react-query';

import {
  IncidentCommands,
  WeatherRecommendationCommands,
  type ReportWeatherIncidentInput,
  type WeatherRecommendationCommandInput,
} from '@/features/weather/api/WeatherRecommendationCommands';
import { applyWeatherRecommendation } from '@/features/weather/api/WeatherRecommendationQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useWeatherRecommendationCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();

  const command = useMutation({
    mutationFn: (input: WeatherRecommendationCommandInput) =>
      WeatherRecommendationCommands.execute(client, input),
    onSuccess: (recommendation) => {
      applyWeatherRecommendation(queryClient, recommendation);
    },
  });

  const createIncident = useMutation({
    mutationFn: async ({
      recommendationId,
      expectedVersion,
      incident,
    }: {
      recommendationId: string;
      expectedVersion: number;
      incident: ReportWeatherIncidentInput;
    }) => {
      const created = await IncidentCommands.reportWeather(client, incident);
      return WeatherRecommendationCommands.execute(client, {
        recommendationId,
        type: 'LINK_INCIDENT',
        expectedVersion,
        incidentId: created.id,
        reason: 'Opened weather incident from Control Tower',
      });
    },
    onSuccess: (recommendation) => {
      applyWeatherRecommendation(queryClient, recommendation);
    },
  });

  return { command, createIncident };
}
