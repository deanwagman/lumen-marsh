import { hasVenueOpsScope, venueOpsScopes, type AuthSession } from '@/auth/session';

export const queueTrends = ['FALLING', 'STABLE', 'RISING'] as const;
export type QueueTrend = (typeof queueTrends)[number];

export const queueFreshness = ['FRESH', 'DELAYED', 'STALE'] as const;
export type QueueFreshness = (typeof queueFreshness)[number];

export const forecastConfidences = ['LOW', 'MEDIUM', 'HIGH'] as const;
export type ForecastConfidence = (typeof forecastConfidences)[number];

export const flowRecommendationTypes = [
  'CONGESTION_EXPECTED',
  'DEMAND_SHIFT_EXPECTED',
  'CAPACITY_REVIEW',
  'POSTED_WAIT_REVIEW',
  'GUEST_REDIRECTION',
  'DATA_QUALITY',
] as const;
export type FlowRecommendationType = (typeof flowRecommendationTypes)[number];

export const flowRecommendationStatuses = [
  'PENDING_REVIEW',
  'APPROVED',
  'PUBLISHED',
  'DISMISSED',
  'WITHDRAWN',
  'EXPIRED',
] as const;
export type FlowRecommendationStatus = (typeof flowRecommendationStatuses)[number];

export const flowRecommendationSeverities = ['INFO', 'WARNING', 'CRITICAL'] as const;
export type FlowRecommendationSeverity = (typeof flowRecommendationSeverities)[number];

export const flowRecommendationCommands = ['APPROVE', 'DISMISS', 'PUBLISH', 'WITHDRAW'] as const;
export type FlowRecommendationCommand = (typeof flowRecommendationCommands)[number];

export type FlowAttractionCard = {
  attractionId: string;
  displayName: string;
  status: string;
  postedWaitMinutes: number | null;
  calculatedWaitMinutes: number | null;
  predictedWaitMinutes15: number | null;
  predictedWaitMinutes30: number | null;
  predictedWaitMinutes60: number | null;
  queueLength: number | null;
  arrivalsPerMinute: number | null;
  throughputPerMinute: number | null;
  operatingCapacityPercent: number | null;
  trend: QueueTrend | null;
  freshness: QueueFreshness | null;
  observedAt: string | null;
  simulated: boolean;
};

export type FlowOverview = {
  guestsInQueues: number;
  risingWaitCount: number;
  staleAttractionCount: number;
  pendingRecommendationCount: number;
  parkCapacityPercent: number;
  lastSynchronizedAt: string | null;
  attractions: FlowAttractionCard[];
  simulated: boolean;
};

export type QueueProjection = {
  attractionId: string;
  observationId: string;
  queueLength: number;
  arrivalsPerMinute: number;
  throughputPerMinute: number;
  calculatedWaitMinutes: number;
  postedWaitMinutes: number;
  operatingCapacityPercent: number;
  trend: QueueTrend;
  freshness: QueueFreshness;
  observedAt: string;
  receivedAt: string;
  updatedAt: string;
  version: number;
  simulated: boolean;
};

export type QueueForecast = {
  forecastId: string;
  attractionId: string;
  generatedAt: string;
  basedOnObservationId: string;
  horizonMinutes: number;
  predictedQueueLength: number;
  predictedWaitMinutes: number;
  confidence: ForecastConfidence;
  assumptions: string[];
  explanation: string;
  simulated: boolean;
};

export type FlowRecommendation = {
  recommendationId: string;
  type: FlowRecommendationType;
  status: FlowRecommendationStatus;
  severity: FlowRecommendationSeverity;
  sourceAttractionId: string | null;
  affectedAttractionIds: string[];
  recommendedDestinationIds: string[];
  summary: string;
  explanation: string;
  guestMessage: string | null;
  expiresAt: string;
  relatedIncidentId: string | null;
  relatedWorkOrderId: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  simulated: boolean;
};

export type FlowActivity = {
  id: string;
  sequence: number;
  eventType: string;
  fromStatus: string | null;
  toStatus: string;
  actor: string;
  actorType: string;
  reason: string | null;
  commandId: string | null;
  correlationId: string | null;
  occurredAt: string;
  resultingVersion: number;
};

export type FlowAttractionDetail = {
  attractionId: string;
  displayName: string;
  status: string;
  postedWaitMinutes: number | null;
  projection: QueueProjection | null;
  forecasts: QueueForecast[];
  simulated: boolean;
};

export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};

export function canReadFlow(session: AuthSession | null | undefined): boolean {
  return hasVenueOpsScope(session, venueOpsScopes.flowRead);
}

export function canCommandFlow(session: AuthSession | null | undefined): boolean {
  return hasVenueOpsScope(session, venueOpsScopes.flowCommand);
}

export function canPublishFlow(session: AuthSession | null | undefined): boolean {
  return hasVenueOpsScope(session, venueOpsScopes.flowPublish) && session?.role === 'supervisor';
}

export function nextFlowCommands(
  status: FlowRecommendationStatus,
  session: AuthSession | null | undefined,
): FlowRecommendationCommand[] {
  const commands: FlowRecommendationCommand[] = [];
  if (status === 'PENDING_REVIEW' && canCommandFlow(session)) {
    commands.push('APPROVE', 'DISMISS');
  }
  if (status === 'APPROVED') {
    if (canPublishFlow(session)) {
      commands.push('PUBLISH');
    }
    if (canCommandFlow(session)) {
      commands.push('DISMISS');
    }
  }
  if (status === 'PUBLISHED' && canPublishFlow(session)) {
    commands.push('WITHDRAW');
  }
  return commands;
}

export function isOpenFlowRecommendation(status: FlowRecommendationStatus): boolean {
  return status === 'PENDING_REVIEW' || status === 'APPROVED' || status === 'PUBLISHED';
}

export function newCommandId(): string {
  return crypto.randomUUID();
}

export function newCorrelationId(): string {
  return crypto.randomUUID();
}

export function forecastChartText(card: FlowAttractionCard): string {
  const posted = card.postedWaitMinutes == null ? 'unposted' : `${card.postedWaitMinutes} minutes posted`;
  const calculated =
    card.calculatedWaitMinutes == null ? 'no calculated wait' : `${card.calculatedWaitMinutes} minutes calculated`;
  const fifteen = card.predictedWaitMinutes15 == null ? 'unavailable' : `${card.predictedWaitMinutes15} minutes`;
  const thirty = card.predictedWaitMinutes30 == null ? 'unavailable' : `${card.predictedWaitMinutes30} minutes`;
  const sixty = card.predictedWaitMinutes60 == null ? 'unavailable' : `${card.predictedWaitMinutes60} minutes`;
  return `${card.displayName}: ${posted}, ${calculated}. Forecast 15 minutes ${fifteen}, 30 minutes ${thirty}, 60 minutes ${sixty}.`;
}
