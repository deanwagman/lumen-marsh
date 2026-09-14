export const PARK_TIME_ZONE = 'America/New_York';

export function formatParkDate(value: Date, timeZone = PARK_TIME_ZONE): string {
  return new Intl.DateTimeFormat('en-US', {
    timeZone,
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(value);
}

export function formatParkTime(value: Date, timeZone = PARK_TIME_ZONE): string {
  return new Intl.DateTimeFormat('en-US', {
    timeZone,
    hour: 'numeric',
    minute: '2-digit',
  }).format(value);
}

export function formatParkDateTime(value: string | Date, timeZone = PARK_TIME_ZONE): string {
  const date = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value : '';
  }
  return `${formatParkDate(date, timeZone)} at ${formatParkTime(date, timeZone)}`;
}

export function formatSyncAge(iso: string | null, now = Date.now()): string | null {
  if (!iso) {
    return null;
  }
  const then = Date.parse(iso);
  if (Number.isNaN(then)) {
    return null;
  }
  const seconds = Math.max(0, Math.round((now - then) / 1000));
  if (seconds < 45) {
    return `Last update received ${seconds} ${seconds === 1 ? 'second' : 'seconds'} ago`;
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `Last synchronized ${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago.`;
  }
  const hours = Math.round(minutes / 60);
  return `Last synchronized ${hours} ${hours === 1 ? 'hour' : 'hours'} ago.`;
}
