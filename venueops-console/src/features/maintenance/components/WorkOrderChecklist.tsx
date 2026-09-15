import { useState } from 'react';

import {
  canCommandMaintenance,
  type ChecklistResult,
} from '@/features/maintenance/domain/maintenance';
import type { MaintenanceWorkOrder } from '@/features/maintenance/domain/maintenance';
import { formatChecklistResult, formatMaintenanceTime } from '@/features/maintenance/domain/maintenancePresentation';
import { checklistProgress, incompleteRequiredChecklistItems } from '@/features/maintenance/domain/maintenance';
import type { AuthSession } from '@/auth/session';
import { HelpHeading } from '@/shared/ui/HelpLink';
import { Button } from '@/shared/ui/Button';

import styles from './WorkOrderChecklist.module.css';

const results: ChecklistResult[] = ['PENDING', 'PASSED', 'FAILED', 'NOT_APPLICABLE'];

function checklistCompletionHint(status: MaintenanceWorkOrder['status']): string {
  if (status === 'IN_PROGRESS') {
    return 'Required items are resolved. Inspection can be requested.';
  }
  if (status === 'AWAITING_INSPECTION') {
    return 'Required items are complete. Supervisor inspection is the next step.';
  }
  return 'Required items are complete.';
}

export function WorkOrderChecklist({
  workOrder,
  session,
  pending,
  onRecord,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null;
  pending: boolean;
  onRecord: (input: { checklistItemId: string; result: ChecklistResult; notes?: string }) => void;
}) {
  const canCommand = canCommandMaintenance(session);
  const progress = checklistProgress(workOrder.checklist);
  const blockers = incompleteRequiredChecklistItems(workOrder.checklist);
  const [drafts, setDrafts] = useState<Record<string, { result: ChecklistResult; notes: string }>>({});

  return (
    <section className={styles.panel} aria-labelledby="checklist-heading">
      <HelpHeading
        id="checklist-heading"
        article="maintenance"
        section="checklists"
        label="Help: maintenance checklists"
      >
        Checklist
      </HelpHeading>
      <p className={styles.progress}>
        Required items complete {progress.resolved} of {progress.total}
      </p>
      <div className={styles.track} aria-hidden="true">
        <span
          className={styles.fill}
          style={{ width: progress.total === 0 ? '0%' : `${(progress.resolved / progress.total) * 100}%` }}
        />
      </div>
      {blockers.length > 0 ? (
        <p>
          Incomplete required items prevent inspection: {blockers.map((item) => item.label).join(', ')}.
        </p>
      ) : (
        <p className={styles.muted}>{checklistCompletionHint(workOrder.status)}</p>
      )}
      <ol className={styles.list}>
        {workOrder.checklist.map((item) => {
          const draft = drafts[item.id] ?? {
            result: item.result,
            notes: item.notes ?? '',
          };
          return (
            <li key={item.id} className={item.required ? styles.required : undefined}>
              <p>
                <span className={styles.sequence}>{item.sequence}</span> {item.label}
                {item.required ? <span className={styles.marker}> Required</span> : null}
              </p>
              {item.instructions ? <p className={styles.muted}>{item.instructions}</p> : null}
              <p>Result {formatChecklistResult(item.result)}</p>
              {item.notes ? <p>Notes {item.notes}</p> : null}
              {item.completedByDisplayName ? (
                <p className={styles.muted}>
                  {item.completedByDisplayName} · {formatMaintenanceTime(item.completedAt)}
                </p>
              ) : null}
              {canCommand && workOrder.status !== 'COMPLETED' && workOrder.status !== 'CANCELED' ? (
                <div className={styles.controls}>
                  <label>
                    Result
                    <select
                      aria-label={`${item.label} result`}
                      value={draft.result}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [item.id]: { ...draft, result: event.target.value as ChecklistResult },
                        }))
                      }
                      disabled={pending}
                    >
                      {results.map((result) => (
                        <option key={result} value={result}>
                          {formatChecklistResult(result)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Notes
                    <input
                      value={draft.notes}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [item.id]: { ...draft, notes: event.target.value },
                        }))
                      }
                    />
                  </label>
                  <Button
                    disabled={pending}
                    onClick={() =>
                      onRecord({
                        checklistItemId: item.id,
                        result: draft.result,
                        notes: draft.notes || undefined,
                      })
                    }
                  >
                    Save checklist result
                  </Button>
                </div>
              ) : null}
            </li>
          );
        })}
      </ol>
    </section>
  );
}
