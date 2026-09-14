import type { StatusTone } from '@/features/attractions/domain/attraction';

import type { IncidentSeverity, IncidentStatus } from './incident';

export function severityTone(severity: IncidentSeverity): StatusTone {
  if (severity === 'CRITICAL' || severity === 'MAJOR') {
    return 'hold';
  }
  if (severity === 'MODERATE') {
    return 'warning';
  }
  return 'operating';
}

export function statusTone(status: IncidentStatus): StatusTone {
  if (status === 'RESOLVED') {
    return 'closed';
  }
  if (status === 'MITIGATING') {
    return 'warning';
  }
  if (status === 'ACKNOWLEDGED') {
    return 'operating';
  }
  return 'warning';
}

export function advisoryLabel(published: boolean): string {
  return published ? 'Guest advisory published' : 'Guest advisory not published';
}

export function advisoryStateLabel(published: boolean): string {
  return published ? 'Published' : 'Not published';
}
