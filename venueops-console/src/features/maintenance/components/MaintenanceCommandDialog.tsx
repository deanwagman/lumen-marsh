import { useEffect, useId, useRef, type FormEvent, type ReactNode } from 'react';

import { Button } from '@/shared/ui/Button';

import styles from './MaintenanceCommandDialog.module.css';

export function MaintenanceCommandDialog({
  title,
  description,
  currentState,
  resultingState,
  attractionName,
  warning,
  stale,
  pending,
  error,
  submitLabel,
  children,
  onClose,
  onSubmit,
}: {
  title: string;
  description: string;
  currentState: string;
  resultingState?: string | null;
  attractionName?: string;
  warning?: string | null;
  stale?: boolean;
  pending?: boolean;
  error?: string | null;
  submitLabel: string;
  children?: ReactNode;
  onClose: () => void;
  onSubmit: () => void;
}) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const dialog = dialogRef.current;
    const focusable = dialog?.querySelector<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    focusable?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        if (!pending) {
          onClose();
        }
        return;
      }
      if (event.key !== 'Tab' || !dialog) {
        return;
      }
      const nodes = Array.from(
        dialog.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'),
      ).filter((element) => !element.hasAttribute('disabled'));
      if (nodes.length === 0) {
        return;
      }
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      if (previouslyFocused instanceof HTMLElement) {
        previouslyFocused.focus();
      }
    };
  }, [onClose, pending]);

  function submit(event: FormEvent) {
    event.preventDefault();
    if (stale || pending) {
      return;
    }
    onSubmit();
  }

  return (
    <div className={styles.backdrop} role="presentation" onClick={pending ? undefined : onClose}>
      <div
        ref={dialogRef}
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id={titleId}>{title}</h2>
        <p id={descriptionId}>{description}</p>
        <dl className={styles.facts}>
          <div>
            <dt>Current state</dt>
            <dd>{currentState}</dd>
          </div>
          {resultingState ? (
            <div>
              <dt>Resulting state</dt>
              <dd>{resultingState}</dd>
            </div>
          ) : null}
          {attractionName ? (
            <div>
              <dt>Affected attraction</dt>
              <dd>{attractionName}</dd>
            </div>
          ) : null}
        </dl>
        {warning ? <p className={styles.warning}>{warning}</p> : null}
        {stale ? (
          <p className={styles.warning} role="alert">
            This record changed while you were reviewing it. The latest state has been loaded.
            Close this dialog and review the current server state before submitting.
          </p>
        ) : null}
        {error ? (
          <p className={styles.warning} role="alert">
            {error}
          </p>
        ) : null}
        <form className={styles.form} onSubmit={submit}>
          {children}
          <div className={styles.actions}>
            <Button type="submit" disabled={pending || stale}>
              {pending ? 'Sending…' : submitLabel}
            </Button>
            <Button type="button" variant="ghost" onClick={onClose} disabled={pending}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
