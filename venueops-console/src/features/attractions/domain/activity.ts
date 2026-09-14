import type { AttractionStatus, CapacityMode } from './attraction';

export const attractionEventTypes = [
  'ATTRACTION_TESTING_STARTED',
  'ATTRACTION_TESTING_COMPLETED',
  'ATTRACTION_OPENED',
  'WEATHER_HOLD_PLACED',
  'WEATHER_HOLD_CLEARED',
  'TECHNICAL_FAULT_REPORTED',
  'REPAIR_COMPLETED',
  'ATTRACTION_CLOSED',
  'CAPACITY_REDUCED',
  'CAPACITY_RESTORED',
  'WAIT_TIME_UPDATED',
] as const;

export type AttractionEventType = (typeof attractionEventTypes)[number];

export type AttractionActivity = {
  id: string;
  attractionId: string;
  type: AttractionEventType;
  actor: string;
  reason: string | null;
  occurredAt: string;
  previousVersion: number;
  resultingVersion: number;
  previousStatus: AttractionStatus | null;
  newStatus: AttractionStatus | null;
  previousMode: CapacityMode | null;
  newMode: CapacityMode | null;
  previousMinutes: number | null;
  newMinutes: number | null;
};
