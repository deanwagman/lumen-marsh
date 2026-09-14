export const STREAM_STALE_AFTER_MS = 10_000;

export const streamStatuses = ['connecting', 'live', 'reconnecting', 'stale'] as const;

export type StreamStatus = (typeof streamStatuses)[number];

export type AttractionStreamHealth = {
  status: StreamStatus;
  lastEventAt: number | null;
};

export const defaultStreamHealth: AttractionStreamHealth = {
  status: 'connecting',
  lastEventAt: null,
};

export type StreamHealthEvent =
  | { type: 'connect' }
  | { type: 'open'; at?: number }
  | { type: 'message'; at: number }
  | { type: 'error' }
  | { type: 'stale' };

export function reduceStreamHealth(
  current: AttractionStreamHealth,
  event: StreamHealthEvent,
): AttractionStreamHealth {
  switch (event.type) {
    case 'connect':
      return {
        ...current,
        status: current.lastEventAt === null ? 'connecting' : 'reconnecting',
      };
    case 'open':
      return {
        status: 'live',
        lastEventAt: current.lastEventAt,
      };
    case 'message':
      return {
        status: 'live',
        lastEventAt: event.at,
      };
    case 'error':
      return {
        ...current,
        status: 'reconnecting',
      };
    case 'stale':
      return {
        ...current,
        status: 'stale',
      };
  }
}

export function streamStatusLabel(status: StreamStatus): string {
  switch (status) {
    case 'connecting':
      return 'Connecting';
    case 'live':
      return 'Live';
    case 'reconnecting':
      return 'Reconnecting';
    case 'stale':
      return 'Showing last-known conditions';
  }
}
