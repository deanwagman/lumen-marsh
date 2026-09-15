import { StatusBadge } from '@/shared/ui/StatusBadge';

import { formatPriority, priorityTone } from '@/features/maintenance/domain/maintenancePresentation';
import type { MaintenancePriority } from '@/features/maintenance/domain/maintenance';

const meanings: Record<MaintenancePriority, string> = {
  P1: 'Immediate safety or ride-stopping fault',
  P2: 'Urgent degradation',
  P3: 'Scheduled corrective work',
  P4: 'Routine follow-up',
};

export function MaintenancePriorityBadge({
  priority,
  compact = false,
}: {
  priority: MaintenancePriority;
  compact?: boolean;
}) {
  return (
    <StatusBadge
      tone={priorityTone(priority)}
      label={compact ? formatPriority(priority) : `${formatPriority(priority)} · ${meanings[priority]}`}
    />
  );
}
