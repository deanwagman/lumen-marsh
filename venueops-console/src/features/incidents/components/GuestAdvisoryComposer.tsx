import { useId, useRef, useState, type FormEvent } from 'react';

import type { Incident } from '@/features/incidents/domain/incident';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';

import styles from './GuestAdvisoryComposer.module.css';

export function GuestAdvisoryComposer({
  incident,
  pending,
  onPublish,
  onWithdraw,
}: {
  incident: Incident;
  pending: boolean;
  onPublish: (input: { guestTitle: string; guestMessage: string }) => void;
  onWithdraw: (reason: string) => void;
}) {
  const titleId = useId();
  const messageId = useId();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [title, setTitle] = useState(incident.guestTitle ?? '');
  const [message, setMessage] = useState(incident.guestMessage ?? '');
  const [reason, setReason] = useState('');
  const [mode, setMode] = useState<'publish' | 'withdraw' | null>(null);

  function open(next: 'publish' | 'withdraw') {
    setMode(next);
    dialogRef.current?.showModal();
  }

  function close() {
    dialogRef.current?.close();
    setMode(null);
  }

  function confirm(event: FormEvent) {
    event.preventDefault();
    if (mode === 'publish') {
      onPublish({ guestTitle: title.trim(), guestMessage: message.trim() });
    } else if (mode === 'withdraw') {
      onWithdraw(reason.trim());
    }
    close();
  }

  return (
    <section className={styles.panel} aria-labelledby="guest-advisory-heading">
      <HelpHeading
        id="guest-advisory-heading"
        article="incidents"
        section="advisories"
        label="Help: guest advisories"
      >
        Guest advisory
      </HelpHeading>
      <p>
        {incident.guestAdvisoryPublished
          ? 'Published. Guests see only the title and message below.'
          : 'Not published. Internal notes stay off the guest feed.'}
      </p>
      {incident.guestAdvisoryPublished ? (
        <blockquote>
          <p>{incident.guestTitle}</p>
          <p>{incident.guestMessage}</p>
        </blockquote>
      ) : null}
      {!incident.guestAdvisoryPublished ? (
        <>
          <label htmlFor={titleId}>Guest title</label>
          <input id={titleId} value={title} onChange={(event) => setTitle(event.target.value)} />
          <label htmlFor={messageId}>Guest message</label>
          <textarea
            id={messageId}
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
          <Button
            disabled={pending || title.trim().length === 0 || message.trim().length === 0}
            onClick={() => open('publish')}
          >
            Preview and publish
          </Button>
        </>
      ) : (
        <>
          <label htmlFor={`${titleId}-reason`}>Withdrawal reason</label>
          <input
            id={`${titleId}-reason`}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <Button disabled={pending || reason.trim().length === 0} onClick={() => open('withdraw')}>
            Withdraw advisory
          </Button>
        </>
      )}
      <dialog ref={dialogRef} className={styles.dialog} onClose={close}>
        <form onSubmit={confirm}>
          <h3>{mode === 'withdraw' ? 'Withdraw guest advisory' : 'Publish guest advisory'}</h3>
          <p>This preview contains no internal description, evidence, operator identity, or activity.</p>
          {mode === 'publish' ? (
            <blockquote>
              <p>{title}</p>
              <p>{message}</p>
            </blockquote>
          ) : (
            <p>Reason: {reason}</p>
          )}
          <div className={styles.actions}>
            <Button type="submit" disabled={pending}>
              Confirm
            </Button>
            <Button type="button" variant="ghost" onClick={close}>
              Cancel
            </Button>
          </div>
        </form>
      </dialog>
    </section>
  );
}
