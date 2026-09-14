export const weatherRecommendationStatuses = ['ACTIVE', 'CLEARED'] as const;
export type WeatherRecommendationStatus = (typeof weatherRecommendationStatuses)[number];

export const weatherRecommendationOperatorStatuses = [
  'PENDING',
  'ACKNOWLEDGED',
  'DISMISSED',
  'LINKED',
] as const;
export type WeatherRecommendationOperatorStatus =
  (typeof weatherRecommendationOperatorStatuses)[number];

export const weatherRecommendationSeverities = ['INFO', 'WATCH', 'WARNING'] as const;
export type WeatherRecommendationSeverity = (typeof weatherRecommendationSeverities)[number];

export const weatherRecommendationCommands = ['ACKNOWLEDGE', 'DISMISS', 'LINK_INCIDENT'] as const;
export type WeatherRecommendationCommandType = (typeof weatherRecommendationCommands)[number];

export type WeatherRecommendation = {
  id: string;
  ruleId: string;
  status: WeatherRecommendationStatus;
  operatorStatus: WeatherRecommendationOperatorStatus;
  severity: WeatherRecommendationSeverity;
  summary: string;
  evidence: string;
  recommendedAction: string;
  affectedAttractionIds: string[];
  observedAt: string;
  receivedAt: string;
  updatedAt: string;
  sourceVersion: number;
  version: number;
  simulated: boolean;
  linkedIncidentId: string | null;
};

export type WeatherRecommendationSseUpdate = {
  eventId: string;
  eventType: 'UPDATED' | 'CLEARED';
  occurredAt: string;
  recommendation: WeatherRecommendation;
};

export function severityTone(
  severity: WeatherRecommendationSeverity,
): 'operating' | 'warning' | 'hold' {
  switch (severity) {
    case 'INFO':
      return 'operating';
    case 'WATCH':
      return 'warning';
    case 'WARNING':
      return 'hold';
  }
}

export function canHandleRecommendation(recommendation: WeatherRecommendation): boolean {
  return (
    recommendation.status === 'ACTIVE' &&
    (recommendation.operatorStatus === 'PENDING' || recommendation.operatorStatus === 'ACKNOWLEDGED')
  );
}

export function incidentSeverityFromRecommendation(
  severity: WeatherRecommendationSeverity,
): 'MINOR' | 'MODERATE' | 'MAJOR' {
  switch (severity) {
    case 'INFO':
      return 'MINOR';
    case 'WATCH':
      return 'MODERATE';
    case 'WARNING':
      return 'MAJOR';
  }
}

export function incidentPrefill(recommendation: WeatherRecommendation): {
  title: string;
  type: 'WEATHER';
  severity: 'MINOR' | 'MODERATE' | 'MAJOR';
  internalDescription: string;
  attractionIds: string[];
} {
  return {
    title: recommendation.summary,
    type: 'WEATHER',
    severity: incidentSeverityFromRecommendation(recommendation.severity),
    internalDescription: `${recommendation.evidence}\n\nRecommended action: ${recommendation.recommendedAction}`,
    attractionIds: [...recommendation.affectedAttractionIds],
  };
}
