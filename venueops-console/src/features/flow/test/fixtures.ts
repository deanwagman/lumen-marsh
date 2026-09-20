import type { FlowOverview, FlowRecommendation } from '@/features/flow/domain/flow';

export const mangroveFlowCard = {
  attractionId: 'mangrove-run',
  displayName: 'Mangrove Run',
  status: 'OPEN',
  postedWaitMinutes: 25,
  calculatedWaitMinutes: 20,
  predictedWaitMinutes15: 25,
  predictedWaitMinutes30: 35,
  predictedWaitMinutes60: 45,
  queueLength: 225,
  arrivalsPerMinute: 18,
  throughputPerMinute: 12,
  operatingCapacityPercent: 75,
  trend: 'RISING' as const,
  freshness: 'FRESH' as const,
  observedAt: '2026-09-15T18:30:00Z',
  simulated: true,
};

export const flowOverviewFixture: FlowOverview = {
  guestsInQueues: 355,
  risingWaitCount: 1,
  staleAttractionCount: 0,
  pendingRecommendationCount: 1,
  parkCapacityPercent: 82,
  lastSynchronizedAt: '2026-09-15T18:30:00Z',
  attractions: [
    mangroveFlowCard,
    {
      attractionId: 'cypress-coil',
      displayName: 'Cypress Coil',
      status: 'OPEN',
      postedWaitMinutes: 15,
      calculatedWaitMinutes: 15,
      predictedWaitMinutes15: 20,
      predictedWaitMinutes30: 30,
      predictedWaitMinutes60: 35,
      queueLength: 90,
      arrivalsPerMinute: 10,
      throughputPerMinute: 10,
      operatingCapacityPercent: 100,
      trend: 'STABLE',
      freshness: 'FRESH',
      observedAt: '2026-09-15T18:30:00Z',
      simulated: true,
    },
  ],
  simulated: true,
};

export const flowRecommendationFixture: FlowRecommendation = {
  recommendationId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  type: 'GUEST_REDIRECTION',
  status: 'PENDING_REVIEW',
  severity: 'WARNING',
  sourceAttractionId: 'mangrove-run',
  affectedAttractionIds: ['mangrove-run', 'cypress-coil'],
  recommendedDestinationIds: ['cypress-coil'],
  summary: 'Mangrove Run capacity is down; expect demand to shift to Cypress Coil.',
  explanation:
    'Simulated telemetry shows Mangrove Run boarding no guests. Forecasts rise at Cypress Coil before posted waits catch up.',
  guestMessage:
    'Mangrove Run is temporarily unavailable. Cypress Coil currently has a shorter wait.',
  expiresAt: '2026-09-15T20:30:00Z',
  relatedIncidentId: null,
  relatedWorkOrderId: null,
  version: 1,
  createdAt: '2026-09-15T18:30:00Z',
  updatedAt: '2026-09-15T18:30:00Z',
  simulated: true,
};

export function toApiRecommendation(recommendation: FlowRecommendation) {
  return {
    recommendationId: recommendation.recommendationId,
    recommendation,
    simulated: recommendation.simulated,
  };
}
