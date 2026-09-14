import type { AttractionActivity } from '@/features/attractions/domain/activity';
import type { AttractionSummary, OperatorAttraction } from '@/features/attractions/domain/attraction';
import type { Incident, IncidentActivity } from '@/features/incidents/domain/incident';

export const mangroveRun: AttractionSummary = {
  id: 'mangrove-run',
  name: 'Mangrove Run',
  area: 'Luminous Wetlands',
  type: 'BOAT_EXPEDITION',
  status: 'OPERATING',
  capacityMode: 'NORMAL',
  waitMinutes: 25,
  statusMessage: null,
  updatedAt: '2026-08-28T16:00:00Z',
  version: 0,
  thumbnailUrl: '/media/attractions/mangrove-run/thumbnail.webp',
  thumbnailAltText: 'An expedition boat moving through a glowing mangrove forest.',
};

export const stormglassStation: AttractionSummary = {
  id: 'stormglass-station',
  name: 'Stormglass Station',
  area: 'Research Quarter',
  type: 'INDOOR_DARK_RIDE',
  status: 'CLOSED',
  capacityMode: 'NOT_APPLICABLE',
  waitMinutes: null,
  statusMessage: 'Currently closed.',
  updatedAt: '2026-08-28T16:00:00Z',
  version: 0,
  thumbnailUrl: '/media/attractions/stormglass-station/thumbnail.webp',
  thumbnailAltText: 'A research outpost of stormglass instruments.',
};

export const cypressCoil: AttractionSummary = {
  id: 'cypress-coil',
  name: 'Cypress Coil',
  area: 'Cypress Basin',
  type: 'LAUNCH_COASTER',
  status: 'CLOSED',
  capacityMode: 'NOT_APPLICABLE',
  waitMinutes: null,
  statusMessage: 'Currently closed.',
  updatedAt: '2026-08-28T16:00:00Z',
  version: 0,
  thumbnailUrl: '/media/attractions/cypress-coil/thumbnail.webp',
  thumbnailAltText: 'A launch coaster threading through cypress groves.',
};

export const catalogAttractions: AttractionSummary[] = [
  mangroveRun,
  stormglassStation,
  cypressCoil,
];

export function toOperatorAttraction(attraction: AttractionSummary): OperatorAttraction {
  return {
    id: attraction.id,
    name: attraction.name,
    area: attraction.area,
    type: attraction.type,
    status: attraction.status,
    capacityMode: attraction.capacityMode,
    waitMinutes: attraction.waitMinutes,
    updatedAt: attraction.updatedAt,
    version: attraction.version,
  };
}

export const mangroveWaitTimeActivity: AttractionActivity = {
  id: 'activity-wait-1',
  attractionId: 'mangrove-run',
  type: 'WAIT_TIME_UPDATED',
  actor: 'control-tower',
  reason: null,
  occurredAt: '2026-08-28T16:05:00Z',
  previousVersion: 0,
  resultingVersion: 1,
  previousStatus: null,
  newStatus: null,
  previousMode: null,
  newMode: null,
  previousMinutes: 25,
  newMinutes: 35,
};

export const lightningHoldRecommendation = {
  id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
  ruleId: 'simulated-lightning-hold',
  status: 'ACTIVE' as const,
  operatorStatus: 'PENDING' as const,
  severity: 'WARNING' as const,
  summary: 'Place Mangrove Run and Cypress Coil on weather hold',
  evidence:
    'Simulated lightning strike 1.2 miles from the western basin; this is a demonstration signal, not an NWS observation.',
  recommendedAction: 'Place Mangrove Run and Cypress Coil on weather hold',
  affectedAttractionIds: ['mangrove-run', 'cypress-coil'],
  observedAt: '2026-09-01T16:00:00Z',
  receivedAt: '2026-09-01T16:00:00Z',
  updatedAt: '2026-09-01T16:00:00Z',
  sourceVersion: 1,
  version: 1,
  simulated: true,
  linkedIncidentId: null,
};

export const lightningIncident: Incident = {
  id: 'inc-lightning-1',
  title: 'Lightning activity near western basin',
  type: 'WEATHER',
  severity: 'MAJOR',
  status: 'REPORTED',
  internalDescription: 'Repeated strikes detected within the hold radius.',
  assignedTo: null,
  guestAdvisoryPublished: false,
  guestTitle: null,
  guestMessage: null,
  attractionIds: ['mangrove-run'],
  createdAt: '2026-09-01T16:10:00Z',
  updatedAt: '2026-09-01T16:10:00Z',
  version: 1,
};

export const lightningIncidentActivity: IncidentActivity = {
  id: 'activity-incident-1',
  incidentId: 'inc-lightning-1',
  type: 'INCIDENT_REPORTED',
  actor: 'operator-sub-1',
  reason: null,
  occurredAt: '2026-09-01T16:10:00Z',
  previousVersion: 0,
  resultingVersion: 1,
};


