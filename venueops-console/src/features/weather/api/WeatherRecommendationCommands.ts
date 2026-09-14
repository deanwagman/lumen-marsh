import type { WeatherRecommendationCommandType } from '@/features/weather/domain/recommendation';
import type { ApiClient } from '@/shared/api/client';

import { createdIncidentSchema, weatherRecommendationSchema } from './weatherRecommendationSchema';

export type WeatherRecommendationCommandInput = {
  recommendationId: string;
  type: WeatherRecommendationCommandType;
  expectedVersion: number;
  reason?: string;
  incidentId?: string;
};

export const WeatherRecommendationCommands = {
  execute(client: ApiClient, input: WeatherRecommendationCommandInput) {
    const body: Record<string, unknown> = {
      type: input.type,
      expectedVersion: input.expectedVersion,
    };
    if (input.reason) {
      body.reason = input.reason;
    }
    if (input.incidentId) {
      body.incidentId = input.incidentId;
    }

    return client.post(
      `/api/v1/operator/weather/recommendations/${encodeURIComponent(input.recommendationId)}/commands`,
      weatherRecommendationSchema,
      body,
    );
  },
};

export type ReportWeatherIncidentInput = {
  title: string;
  severity: 'MINOR' | 'MODERATE' | 'MAJOR' | 'CRITICAL';
  internalDescription: string;
  attractionIds: string[];
};

export const IncidentCommands = {
  reportWeather(client: ApiClient, input: ReportWeatherIncidentInput) {
    return client.post(
      '/api/v1/operator/incidents',
      createdIncidentSchema,
      {
        title: input.title,
        type: 'WEATHER',
        severity: input.severity,
        internalDescription: input.internalDescription,
        attractionIds: input.attractionIds,
      },
    );
  },
};
