import type { AttractionStatus, CapacityMode } from '@/features/attractions/domain/attraction';
import type { IncidentSeverity, IncidentStatus } from '@/features/incidents/domain/incident';
import type {
  WeatherRecommendationOperatorStatus,
  WeatherRecommendationSeverity,
} from '@/features/weather/domain/recommendation';

export const dashboardFreshnessStatuses = ['LIVE', 'STALE', 'UNAVAILABLE'] as const;
export type DashboardFreshnessStatus = (typeof dashboardFreshnessStatuses)[number];

export const dashboardAttentionKinds = [
  'CRITICAL_INCIDENT',
  'MAJOR_INCIDENT',
  'WEATHER_HAZARD',
  'WEATHER_HOLD',
  'ABNORMAL_ATTRACTION',
  'UNASSIGNED_INCIDENT',
  'UNACKNOWLEDGED_INCIDENT',
  'ORPHAN_ADVISORY',
  'STALE_DATA',
] as const;
export type DashboardAttentionKind = (typeof dashboardAttentionKinds)[number];

export const dashboardActivityDomains = ['ATTRACTION', 'INCIDENT', 'WEATHER'] as const;
export type DashboardActivityDomain = (typeof dashboardActivityDomains)[number];

export type DashboardSummary = {
  operatingAttractions: number;
  attractionsNeedingAttention: number;
  closedAttractions: number;
  weatherHoldAttractions: number;
  openIncidents: number;
  majorOrCriticalIncidents: number;
  pendingWeatherRecommendations: number;
  publishedGuestAdvisories: number;
};

export type DashboardAttentionItem = {
  kind: DashboardAttentionKind;
  reason: string;
  href: string;
  subjectId: string;
  subjectLabel: string;
  updatedAt: string | null;
};

export type DashboardIncident = {
  id: string;
  title: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  assignedTo: string | null;
  attractionIds: string[];
  guestAdvisoryPublished: boolean;
  updatedAt: string;
  version: number;
};

export type DashboardAttraction = {
  id: string;
  name: string;
  area: string;
  status: AttractionStatus;
  capacityMode: CapacityMode;
  waitMinutes: number | null;
  statusMessage: string | null;
  updatedAt: string;
  relatedOpenIncidentCount: number;
};

export type DashboardWeatherRecommendation = {
  id: string;
  severity: WeatherRecommendationSeverity;
  recommendedAction: string;
  evidence: string;
  affectedAttractionIds: string[];
  operatorStatus: WeatherRecommendationOperatorStatus;
  linkedIncidentId: string | null;
  observedAt: string;
  updatedAt: string;
  simulated: boolean;
};

export type DashboardAdvisory = {
  id: string;
  title: string;
  message: string;
  severity: IncidentSeverity;
  affectedAttractionIds: string[];
  updatedAt: string;
  incidentId: string;
};

export type DashboardActivity = {
  occurredAt: string;
  actor: string;
  domain: DashboardActivityDomain;
  action: string;
  subject: string;
  reason: string | null;
  resultingVersion: number;
  href: string;
};

export type DashboardSourceFreshness = {
  status: DashboardFreshnessStatus;
  lastUpdatedAt: string | null;
};

export type DashboardFreshness = {
  venueOps: DashboardSourceFreshness;
  environmentalData: DashboardSourceFreshness;
};

export type OperatorDashboard = {
  generatedAt: string;
  summary: DashboardSummary;
  needsAttention: DashboardAttentionItem[];
  openIncidents: DashboardIncident[];
  attractionsNeedingAttention: DashboardAttraction[];
  pendingWeatherRecommendations: DashboardWeatherRecommendation[];
  publishedGuestAdvisories: DashboardAdvisory[];
  recentActivity: DashboardActivity[];
  freshness: DashboardFreshness;
};

export const attentionKindLabels: Record<DashboardAttentionKind, string> = {
  CRITICAL_INCIDENT: 'Critical incident',
  MAJOR_INCIDENT: 'Major incident',
  WEATHER_HAZARD: 'Weather hazard',
  WEATHER_HOLD: 'Weather hold',
  ABNORMAL_ATTRACTION: 'Attraction condition',
  UNASSIGNED_INCIDENT: 'Unassigned incident',
  UNACKNOWLEDGED_INCIDENT: 'Unacknowledged incident',
  ORPHAN_ADVISORY: 'Advisory inconsistency',
  STALE_DATA: 'Stale data',
};

export const activityDomainLabels: Record<DashboardActivityDomain, string> = {
  ATTRACTION: 'Attraction',
  INCIDENT: 'Incident',
  WEATHER: 'Weather',
};

export type DashboardMetricKey = keyof DashboardSummary;

export const dashboardMetrics: Array<{
  key: DashboardMetricKey;
  label: string;
  href: string;
  accessibleName: (count: number) => string;
}> = [
  {
    key: 'operatingAttractions',
    label: 'Operating',
    href: '/attractions?filter=operating',
    accessibleName: (count) =>
      `${count} ${count === 1 ? 'attraction' : 'attractions'} operating`,
  },
  {
    key: 'attractionsNeedingAttention',
    label: 'Needs attention',
    href: '/attractions?filter=attention',
    accessibleName: (count) =>
      `${count} ${count === 1 ? 'attraction' : 'attractions'} needing attention`,
  },
  {
    key: 'closedAttractions',
    label: 'Closed',
    href: '/attractions?filter=closed',
    accessibleName: (count) =>
      `${count} ${count === 1 ? 'attraction' : 'attractions'} closed`,
  },
  {
    key: 'weatherHoldAttractions',
    label: 'Weather hold',
    href: '/attractions?filter=holds',
    accessibleName: (count) =>
      `${count} ${count === 1 ? 'attraction' : 'attractions'} on weather hold`,
  },
  {
    key: 'openIncidents',
    label: 'Open incidents',
    href: '/incidents?status=open',
    accessibleName: (count) => `${count} open ${count === 1 ? 'incident' : 'incidents'}`,
  },
  {
    key: 'majorOrCriticalIncidents',
    label: 'Major or critical',
    href: '/incidents?status=open',
    accessibleName: (count) =>
      `${count} major or critical ${count === 1 ? 'incident' : 'incidents'}`,
  },
  {
    key: 'pendingWeatherRecommendations',
    label: 'Pending weather',
    href: '/attractions#weather-recommendations-heading',
    accessibleName: (count) =>
      `${count} pending weather ${count === 1 ? 'recommendation' : 'recommendations'}`,
  },
  {
    key: 'publishedGuestAdvisories',
    label: 'Published advisories',
    href: '/incidents',
    accessibleName: (count) =>
      `${count} published guest ${count === 1 ? 'advisory' : 'advisories'}`,
  },
];
