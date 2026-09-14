export function formatDataAge(iso: string, now = Date.now()): string {
  const then = Date.parse(iso);
  if (Number.isNaN(then)) {
    return iso;
  }

  const seconds = Math.max(0, Math.round((now - then) / 1000));
  if (seconds < 45) {
    return `${seconds}s ago`;
  }

  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min ago`;
  }

  const hours = Math.round(minutes / 60);
  return `${hours} hr ago`;
}
