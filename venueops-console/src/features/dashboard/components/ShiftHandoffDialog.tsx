import { useEffect, useId, useRef, useState } from 'react';

import { Button } from '@/shared/ui/Button';

import styles from '@/features/dashboard/pages/DashboardPage.module.css';

export function ShiftHandoffDialog({
  summary,
  onClose,
}: {
  summary: string;
  onClose: () => void;
}) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle');

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const dialog = dialogRef.current;
    const closeButton = dialog?.querySelector<HTMLElement>('[data-handoff-close]');
    closeButton?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialog) {
        return;
      }
      const focusable = Array.from(
        dialog.querySelectorAll<HTMLElement>('button, [href], textarea, [tabindex]:not([tabindex="-1"])'),
      ).filter((element) => !element.hasAttribute('disabled'));
      if (focusable.length === 0) {
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
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
  }, [onClose]);

  async function copySummary() {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(summary);
      } else {
        throw new Error('Clipboard unavailable');
      }
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    }
  }

  function printSummary() {
    document.body.classList.add('printing-handoff');
    const cleanup = () => document.body.classList.remove('printing-handoff');
    window.addEventListener('afterprint', cleanup, { once: true });
    window.setTimeout(cleanup, 1_000);
    window.print();
  }

  return (
    <div className={styles.dialogBackdrop} role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id={titleId}>Shift handoff summary</h2>
        <p className={styles.muted}>
          Generated from the current dashboard snapshot. This summary is not stored.
        </p>
        <pre className={styles.handoffDocument} tabIndex={0}>
          {summary}
        </pre>
        <div className={`${styles.dialogActions} print-hide`}>
          <Button onClick={() => void copySummary()}>Copy summary</Button>
          <Button variant="ghost" onClick={printSummary}>
            Print summary
          </Button>
          <Button variant="ghost" data-handoff-close onClick={onClose}>
            Close
          </Button>
        </div>
        <p className={styles.liveRegion} aria-live="polite">
          {copyState === 'copied' ? 'Summary copied to clipboard.' : null}
          {copyState === 'failed' ? 'Unable to copy the summary.' : null}
        </p>
      </div>
    </div>
  );
}
