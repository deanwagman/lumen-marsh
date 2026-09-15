import { useEffect } from 'react';
import { Link } from 'react-router-dom';

import { useCommandReceipt } from '@/features/attractions/hooks/useCommandReceipt';
import { Button } from '@/shared/ui/Button';

import styles from './CommandReceiptBanner.module.css';

export function CommandReceiptBanner() {
  const { receipt, dismiss } = useCommandReceipt();

  useEffect(() => {
    if (!receipt) {
      return;
    }

    const timer = window.setTimeout(() => {
      dismiss();
    }, 12_000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [receipt, dismiss]);

  if (!receipt) {
    return null;
  }

  const activityHref = receipt.workOrderId
    ? receipt.activityId
      ? `/maintenance/work-orders/${receipt.workOrderId}#activity-${receipt.activityId}`
      : `/maintenance/work-orders/${receipt.workOrderId}`
    : receipt.incidentId
    ? receipt.activityId
      ? `/incidents/${receipt.incidentId}#activity-${receipt.activityId}`
      : `/incidents/${receipt.incidentId}`
    : receipt.activityId
      ? `/attractions/${receipt.attractionId}#activity-${receipt.activityId}`
      : `/attractions/${receipt.attractionId}#activity`;

  return (
    <div className={styles.banner} role="status" aria-label="Command accepted">
      <p>{receipt.summary}</p>
      <div className={styles.actions}>
        {receipt.workOrderId ? (
          <Link className={styles.link} to={`/maintenance/work-orders/${receipt.workOrderId}`}>
            Open work order
          </Link>
        ) : null}
        <Link className={styles.link} to={activityHref}>
          View activity
        </Link>
        <Button variant="ghost" onClick={dismiss}>
          Dismiss
        </Button>
      </div>
    </div>
  );
}
