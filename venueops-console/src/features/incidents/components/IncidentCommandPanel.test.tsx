import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { localDevelopmentSession } from '@/auth/session';
import { mangroveRun } from '@/test/fixtures';
import type { Incident } from '@/features/incidents/domain/incident';

import { IncidentCommandPanel } from './IncidentCommandPanel';

const incident: Incident = {
  id: 'inc-1',
  title: 'Lightning',
  type: 'WEATHER',
  severity: 'MAJOR',
  status: 'REPORTED',
  internalDescription: null,
  assignedTo: null,
  guestAdvisoryPublished: false,
  guestTitle: null,
  guestMessage: null,
  attractionIds: [],
  createdAt: '2026-09-01T16:00:00Z',
  updatedAt: '2026-09-01T16:00:00Z',
  version: 1,
};

describe('IncidentCommandPanel', () => {
  it('clears the selected command and draft fields when the incident version changes', async () => {
    const user = userEvent.setup();
    const onCommand = vi.fn();
    const { rerender } = render(
      <MemoryRouter>
        <IncidentCommandPanel
          session={localDevelopmentSession}
          incident={incident}
          attractions={[mangroveRun]}
          pending={false}
          error={null}
          onCommand={onCommand}
        />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: 'Acknowledge' }));
    await user.type(screen.getByLabelText('Reason'), 'Reviewed by control tower');
    expect(screen.getByRole('button', { name: 'Confirm command' })).toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <IncidentCommandPanel
          session={localDevelopmentSession}
          incident={{ ...incident, status: 'ACKNOWLEDGED', version: 2 }}
          attractions={[mangroveRun]}
          pending={false}
          error={null}
          onCommand={onCommand}
        />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('button', { name: 'Confirm command' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Start mitigation' })).toBeInTheDocument();
  });
});
