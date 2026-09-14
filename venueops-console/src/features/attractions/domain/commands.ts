import type { AttractionStatus, CapacityMode } from './attraction';

export const MAX_WAIT_MINUTES = 300;

export const attractionCommands = [
  'START_TESTING',
  'COMPLETE_TESTING',
  'APPROVE_RETURN_TO_SERVICE',
  'PLACE_WEATHER_HOLD',
  'CLEAR_WEATHER_HOLD',
  'REPORT_TECHNICAL_FAULT',
  'COMPLETE_REPAIR',
  'CLOSE_FOR_DAY',
  'REDUCE_CAPACITY',
  'RESTORE_CAPACITY',
  'UPDATE_WAIT_TIME',
] as const;

export type AttractionCommandType = (typeof attractionCommands)[number];

export type CommandTone = 'default' | 'danger' | 'hold';

export type CommandDefinition = {
  type: AttractionCommandType;
  label: string;
  description: string;
  requiresReason: boolean;
  tone: CommandTone;
};

export const commandDefinitions: Record<AttractionCommandType, CommandDefinition> = {
  START_TESTING: {
    type: 'START_TESTING',
    label: 'Start testing',
    description: 'Begin the return-to-service testing cycle.',
    requiresReason: false,
    tone: 'default',
  },
  COMPLETE_TESTING: {
    type: 'COMPLETE_TESTING',
    label: 'Complete testing',
    description: 'Testing finished. Ready for return-to-service approval.',
    requiresReason: false,
    tone: 'default',
  },
  APPROVE_RETURN_TO_SERVICE: {
    type: 'APPROVE_RETURN_TO_SERVICE',
    label: 'Approve return to service',
    description: 'Open the attraction. Capacity returns to normal with a zero wait.',
    requiresReason: false,
    tone: 'default',
  },
  PLACE_WEATHER_HOLD: {
    type: 'PLACE_WEATHER_HOLD',
    label: 'Place weather hold',
    description: 'Temporarily stop operations due to nearby weather.',
    requiresReason: true,
    tone: 'hold',
  },
  CLEAR_WEATHER_HOLD: {
    type: 'CLEAR_WEATHER_HOLD',
    label: 'Clear weather hold',
    description: 'Weather has cleared. Attraction returns to testing.',
    requiresReason: false,
    tone: 'default',
  },
  REPORT_TECHNICAL_FAULT: {
    type: 'REPORT_TECHNICAL_FAULT',
    label: 'Report technical fault',
    description: 'Place the attraction into a technical delay.',
    requiresReason: true,
    tone: 'danger',
  },
  COMPLETE_REPAIR: {
    type: 'COMPLETE_REPAIR',
    label: 'Complete repair',
    description: 'Repair finished. Attraction returns to testing.',
    requiresReason: false,
    tone: 'default',
  },
  CLOSE_FOR_DAY: {
    type: 'CLOSE_FOR_DAY',
    label: 'Close for day',
    description: 'End operations for the remainder of the park day.',
    requiresReason: true,
    tone: 'danger',
  },
  REDUCE_CAPACITY: {
    type: 'REDUCE_CAPACITY',
    label: 'Reduce capacity',
    description: 'Operate at reduced throughput while remaining open.',
    requiresReason: true,
    tone: 'hold',
  },
  RESTORE_CAPACITY: {
    type: 'RESTORE_CAPACITY',
    label: 'Restore capacity',
    description: 'Return from reduced capacity to normal.',
    requiresReason: false,
    tone: 'default',
  },
  UPDATE_WAIT_TIME: {
    type: 'UPDATE_WAIT_TIME',
    label: 'Update wait time',
    description: 'Publish a new posted wait while operating.',
    requiresReason: false,
    tone: 'default',
  },
};

const statusTransitions: Record<AttractionStatus, AttractionCommandType[]> = {
  CLOSED: ['START_TESTING'],
  TESTING: ['COMPLETE_TESTING', 'CLOSE_FOR_DAY'],
  RETURNING_TO_SERVICE: ['APPROVE_RETURN_TO_SERVICE', 'CLOSE_FOR_DAY'],
  OPERATING: ['PLACE_WEATHER_HOLD', 'REPORT_TECHNICAL_FAULT', 'CLOSE_FOR_DAY'],
  WEATHER_HOLD: ['CLEAR_WEATHER_HOLD', 'CLOSE_FOR_DAY'],
  TECHNICAL_DELAY: ['COMPLETE_REPAIR', 'CLOSE_FOR_DAY'],
};

export function validCommands(
  status: AttractionStatus,
  capacityMode: CapacityMode,
): AttractionCommandType[] {
  const commands = [...statusTransitions[status]];

  if (status === 'OPERATING' && capacityMode === 'NORMAL') {
    commands.push('REDUCE_CAPACITY');
  }

  if (status === 'OPERATING' && capacityMode === 'REDUCED') {
    commands.push('RESTORE_CAPACITY');
  }

  if (status === 'OPERATING') {
    commands.push('UPDATE_WAIT_TIME');
  }

  return commands;
}

export function statusCommands(
  status: AttractionStatus,
  capacityMode: CapacityMode,
): AttractionCommandType[] {
  return validCommands(status, capacityMode).filter((type) => type !== 'UPDATE_WAIT_TIME');
}

export function canUpdateWaitTime(status: AttractionStatus): boolean {
  return status === 'OPERATING';
}

export const guestStatusMessages: Record<AttractionStatus, string | null> = {
  CLOSED: 'Currently closed.',
  TESTING: 'Preparing to welcome explorers.',
  RETURNING_TO_SERVICE: 'Expected to reopen soon.',
  OPERATING: null,
  WEATHER_HOLD: 'Temporarily unavailable due to nearby weather.',
  TECHNICAL_DELAY: 'Temporarily unavailable.',
};
