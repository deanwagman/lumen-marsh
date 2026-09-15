import { describe, expect, it } from 'vitest';

import { workOrderIntentKey } from '@/features/maintenance/hooks/useMaintenanceCommand';

describe('workOrderIntentKey', () => {
  it('treats checklist items as distinct intents', () => {
    expect(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'RECORD_CHECKLIST_RESULT',
        expectedVersion: 4,
        data: { checklistItemId: 'item-1', result: 'PASSED' },
      }),
    ).not.toBe(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'RECORD_CHECKLIST_RESULT',
        expectedVersion: 5,
        data: { checklistItemId: 'item-2', result: 'PASSED' },
      }),
    );
  });

  it('reuses the same key when retrying the same checklist item', () => {
    const first = workOrderIntentKey({
      workOrderId: 'wo-1',
      type: 'RECORD_CHECKLIST_RESULT',
      expectedVersion: 4,
      data: { checklistItemId: 'item-1', result: 'PASSED' },
    });
    expect(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'RECORD_CHECKLIST_RESULT',
        expectedVersion: 4,
        data: { checklistItemId: 'item-1', result: 'PASSED' },
      }),
    ).toBe(first);
  });

  it('treats notes and evidence URIs as distinct intents', () => {
    expect(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'ADD_NOTE',
        expectedVersion: 7,
        data: { note: 'First note' },
      }),
    ).not.toBe(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'ADD_NOTE',
        expectedVersion: 8,
        data: { note: 'Second note' },
      }),
    );
    expect(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'ADD_EVIDENCE',
        expectedVersion: 8,
        data: { uri: 'https://lumenmarsh.example/a' },
      }),
    ).not.toBe(
      workOrderIntentKey({
        workOrderId: 'wo-1',
        type: 'ADD_EVIDENCE',
        expectedVersion: 9,
        data: { uri: 'https://lumenmarsh.example/b' },
      }),
    );
  });
});
