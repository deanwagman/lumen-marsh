import { StatusBadge } from '@/shared/ui/StatusBadge';

import {
  formatWorkOrderStatus,
  workOrderStatusTone,
} from '@/features/maintenance/domain/maintenancePresentation';
import type { MaintenanceWorkOrderStatus } from '@/features/maintenance/domain/maintenance';

export function WorkOrderStatusBadge({ status }: { status: MaintenanceWorkOrderStatus }) {
  return <StatusBadge tone={workOrderStatusTone(status)} label={formatWorkOrderStatus(status)} />;
}
