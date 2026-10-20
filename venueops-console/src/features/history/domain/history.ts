export const availabilityClassifications = [
  'OPERATING',
  'REDUCED_CAPACITY',
  'WEATHER_HOLD',
  'TECHNICAL_DELAY',
  'TESTING_RETURN_TO_SERVICE',
  'COMMANDED_CLOSURE',
  'CLOSED_NOT_OPEN',
] as const;

export type AvailabilityClassification =
  (typeof availabilityClassifications)[number];

export type HistoricalDataset = {
  id: string;
  version: string;
  seed: number;
  manifestChecksum: string;
  fromDate: string;
  toDate: string;
  state: 'LOADING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt: string | null;
};

export type Citation = {
  id: string;
  datasetId: string;
  attractionId: string;
  parkDate: string;
  activityId: string | null;
  commandId: string | null;
  controlTowerUrl: string;
  evidenceId: string | null;
};

export type OperatingCause = {
  id: string;
  datasetId: string;
  episodeId: string;
  attractionId: string;
  parkDate: string;
  classification: string;
  summary: string;
  occurredAt: string;
  segmentActivityIds: string[];
  citations: Citation[];
};

export type AttractionAvailability = {
  attractionId: string;
  attractionName: string;
  scheduledMinutes: number;
  operatingMinutes: number;
  uptimePercentage: number;
  degradedMinutes: number;
  downtimeMinutes: Record<AvailabilityClassification, number>;
  longestOutageMinutes: number;
  citations: Citation[];
};

export type AvailabilitySummary = {
  datasetId: string;
  from: string;
  to: string;
  zoneId: string;
  attractions: AttractionAvailability[];
  answers: AnalyticsAnswer[];
};

export type AvailabilitySegment = {
  citationId: string;
  status: string;
  classification: AvailabilityClassification;
  start: string;
  end: string;
  durationMinutes: number;
  reason: string | null;
  commandId: string | null;
  activityId: string | null;
  datasetId: string;
  controlTowerUrl: string;
};

export type AvailabilityDetail = {
  datasetId: string;
  attractionId: string;
  attractionName: string;
  parkDate: string;
  zoneId: string;
  expectedStart: string;
  expectedEnd: string;
  plannedClosure: boolean;
  plannedClosureReason: string | null;
  scheduledMinutes: number;
  operatingMinutes: number;
  uptimePercentage: number;
  degradedMinutes: number;
  downtimeMinutes: Record<AvailabilityClassification, number>;
  longestOutageMinutes: number;
  segments: AvailabilitySegment[];
  causes: OperatingCause[];
};

export type AnalyticsAnswer = {
  datasetId: string;
  question: string;
  answer: string;
  claims: Array<{ text: string; citations: Citation[] }>;
  provider: string;
};

export const evidenceLabels: Record<string, string> = {
  LIGHTNING_DETECTED: 'Lightning detected',
  WEATHER_HOLD: 'Weather hold',
  WEATHER_ALL_CLEAR: 'Weather all-clear',
  VIBRATION_RECOMMENDATION: 'Vibration recommendation',
  ACCEPTED_WORK: 'Accepted work',
  TECHNICAL_DELAY: 'Technical delay',
  REPAIR: 'Repair',
  TESTING: 'Testing',
  RETURN_TO_SERVICE: 'Return to service',
};

export const reducedCapacityRule =
  'A reduced-capacity hour stays in uptime and is also reported as degraded time, so a slow hour does not become downtime.';

export const bundledHistoryDatasetId = 'lumen-marsh-availability-demo-v1';

export const syntheticOctoberLabel = 'Synthetic October story';

export const syntheticOctoberNote =
  'This is the synthetic October story, separate from the live board.';

export type HistorySelection = {
  datasetId: string;
  from: string;
  to: string;
};

export const classificationSymbols: Record<AvailabilityClassification, string> =
  {
    OPERATING: '●',
    REDUCED_CAPACITY: '◐',
    WEATHER_HOLD: '≋',
    TECHNICAL_DELAY: '×',
    TESTING_RETURN_TO_SERVICE: '◆',
    COMMANDED_CLOSURE: '■',
    CLOSED_NOT_OPEN: '○',
  };

export const classificationLabels: Record<AvailabilityClassification, string> =
  {
    OPERATING: 'Operating',
    REDUCED_CAPACITY: 'Operating · reduced capacity',
    WEATHER_HOLD: 'Weather hold',
    TECHNICAL_DELAY: 'Technical delay',
    TESTING_RETURN_TO_SERVICE: 'Testing / return to service',
    COMMANDED_CLOSURE: 'Commanded closure',
    CLOSED_NOT_OPEN: 'Closed · did not open',
  };

export function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  if (hours === 0) return `${remainder} min`;
  if (remainder === 0) return `${hours} hr`;
  return `${hours} hr ${remainder} min`;
}

export function formatHistoryTime(instant: string, zoneId: string): string {
  return new Intl.DateTimeFormat('en-US', {
    timeZone: zoneId,
    hour: 'numeric',
    minute: '2-digit',
    timeZoneName: 'short',
  }).format(new Date(instant));
}

export function downtimeTotal(
  values: Record<AvailabilityClassification, number>,
): number {
  return Object.entries(values).reduce(
    (total, [classification, minutes]) =>
      classification === 'OPERATING' || classification === 'REDUCED_CAPACITY'
        ? total
        : total + minutes,
    0,
  );
}
