import type { OperatorAttraction } from './attraction';
import type { AttractionCommandType } from './commands';
import { formatCapacity, formatStatus, formatWaitTime } from './formatters';

export type CommandReceipt = {
  attractionId?: string;
  attractionName?: string;
  incidentId?: string;
  summary: string;
  version: number;
  activityId: string | null;
};

export function commandReceiptSummary(
  attraction: OperatorAttraction,
  type: AttractionCommandType,
): string {
  if (type === 'UPDATE_WAIT_TIME') {
    return `${attraction.name} wait time set to ${formatWaitTime(attraction.waitMinutes)} · version ${attraction.version}`;
  }

  if (type === 'REDUCE_CAPACITY' || type === 'RESTORE_CAPACITY') {
    return `${attraction.name} updated to ${formatCapacity(attraction.capacityMode)} capacity · version ${attraction.version}`;
  }

  return `${attraction.name} updated to ${formatStatus(attraction.status)} · version ${attraction.version}`;
}
