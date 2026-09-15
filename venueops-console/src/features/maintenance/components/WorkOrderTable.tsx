import { Link } from 'react-router-dom';

import { formatLabel } from '@/features/attractions/domain/formatters';
import type { MaintenanceWorkOrder } from '@/features/maintenance/domain/maintenance';
import {
  assignmentLabel,
  formatEstimatedRestore,
  formatMaintenanceTime,
} from '@/features/maintenance/domain/maintenancePresentation';
import { MaintenancePriorityBadge } from '@/features/maintenance/components/MaintenancePriorityBadge';
import { WorkOrderStatusBadge } from '@/features/maintenance/components/WorkOrderStatusBadge';

import styles from './WorkOrderTable.module.css';

export function WorkOrderTable({
  workOrders,
  attractionNames,
}: {
  workOrders: MaintenanceWorkOrder[];
  attractionNames: Map<string, string>;
}) {
  if (workOrders.length === 0) {
    return (
      <div className={styles.empty}>
        <h2>No work orders match this view</h2>
        <p>Adjust filters or review the reliability inbox.</p>
      </div>
    );
  }

  return (
    <div className={styles.wrap}>
      <table className={styles.table}>
        <caption className={styles.caption}>Maintenance work orders</caption>
        <thead>
          <tr>
            <th scope="col">Work order</th>
            <th scope="col">Summary</th>
            <th scope="col">Priority</th>
            <th scope="col">Status</th>
            <th scope="col">Classification</th>
            <th scope="col">Attraction</th>
            <th scope="col">Asset</th>
            <th scope="col">Assignment</th>
            <th scope="col">Estimated restore</th>
            <th scope="col">Updated</th>
          </tr>
        </thead>
        <tbody>
          {workOrders.map((workOrder) => {
            const accessibleName = [
              workOrder.workOrderNumber,
              workOrder.summary,
              workOrder.priority,
              workOrder.status.replaceAll('_', ' '),
            ].join(', ');
            return (
              <tr key={workOrder.id}>
                <th scope="row">
                  <Link
                    className={styles.number}
                    to={`/maintenance/work-orders/${workOrder.id}`}
                    aria-label={accessibleName}
                  >
                    {workOrder.workOrderNumber}
                  </Link>
                </th>
                <td>{workOrder.summary}</td>
                <td>
                  <MaintenancePriorityBadge compact priority={workOrder.priority} />
                </td>
                <td>
                  <WorkOrderStatusBadge status={workOrder.status} />
                </td>
                <td>{formatLabel(workOrder.classification)}</td>
                <td>{attractionNames.get(workOrder.attractionId) ?? workOrder.attractionId}</td>
                <td className={styles.number}>{workOrder.asset.assetCode}</td>
                <td>{assignmentLabel(workOrder)}</td>
                <td>{formatEstimatedRestore(workOrder.estimatedRestoreAt)}</td>
                <td>{formatMaintenanceTime(workOrder.updatedAt)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
