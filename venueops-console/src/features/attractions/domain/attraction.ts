export const attractionStatuses = [
  'CLOSED',
  'TESTING',
  'RETURNING_TO_SERVICE',
  'OPERATING',
  'WEATHER_HOLD',
  'TECHNICAL_DELAY',
] as const;

export type AttractionStatus = (typeof attractionStatuses)[number];

export const capacityModes = ['NOT_APPLICABLE', 'NORMAL', 'REDUCED'] as const;

export type CapacityMode = (typeof capacityModes)[number];

export const attractionTypes = [
  'BOAT_EXPEDITION',
  'INDOOR_DARK_RIDE',
  'LAUNCH_COASTER',
] as const;

export type AttractionType = (typeof attractionTypes)[number];

export type AttractionSummary = {
  id: string;
  name: string;
  area: string;
  type: AttractionType;
  status: AttractionStatus;
  capacityMode: CapacityMode;
  waitMinutes: number | null;
  statusMessage: string | null;
  updatedAt: string;
  version: number;
  thumbnailUrl: string;
  thumbnailAltText: string;
};

export type OperatorAttraction = {
  id: string;
  name: string;
  area: string;
  type: AttractionType;
  status: AttractionStatus;
  capacityMode: CapacityMode;
  waitMinutes: number | null;
  updatedAt: string;
  version: number;
};

export type AttractionOperationalUpdate = {
  id: string;
  status: AttractionStatus;
  capacityMode: CapacityMode;
  waitMinutes: number | null;
  statusMessage: string | null;
  updatedAt: string;
  version: number;
};

export type StatusTone = 'operating' | 'warning' | 'hold' | 'closed';

export function statusTone(status: AttractionStatus): StatusTone {
  switch (status) {
    case 'OPERATING':
      return 'operating';
    case 'TESTING':
    case 'RETURNING_TO_SERVICE':
      return 'warning';
    case 'WEATHER_HOLD':
    case 'TECHNICAL_DELAY':
      return 'hold';
    case 'CLOSED':
      return 'closed';
  }
}
