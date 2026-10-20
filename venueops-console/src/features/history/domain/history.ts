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
};

export type AnalyticsAnswer = {
  datasetId: string;
  answer: string;
  claims: Array<{ text: string; citations: Citation[] }>;
  provider: string;
};

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
