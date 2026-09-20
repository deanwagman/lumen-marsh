import { describe, expect, it } from 'vitest';

import { venueOpsScopes, type AuthSession } from '@/auth/session';
import {
  canPublishFlow,
  forecastChartText,
  nextFlowCommands,
} from '@/features/flow/domain/flow';
import { mangroveFlowCard } from '@/features/flow/test/fixtures';

const operator: AuthSession = {
  accessToken: 'op',
  operatorId: 'op-1',
  displayName: 'Operator',
  role: 'operator',
  scopes: [venueOpsScopes.flowRead, venueOpsScopes.flowCommand],
};

const supervisor: AuthSession = {
  accessToken: 'sup',
  operatorId: 'sup-1',
  displayName: 'Supervisor',
  role: 'supervisor',
  scopes: [venueOpsScopes.flowRead, venueOpsScopes.flowCommand, venueOpsScopes.flowPublish],
};

describe('flow domain', () => {
  it('reserves publication for supervisors with flow.publish', () => {
    expect(canPublishFlow(operator)).toBe(false);
    expect(canPublishFlow(supervisor)).toBe(true);
    expect(nextFlowCommands('APPROVED', operator)).toEqual(['DISMISS']);
    expect(nextFlowCommands('APPROVED', supervisor)).toEqual(['PUBLISH', 'DISMISS']);
  });

  it('describes the forecast chart without relying on color', () => {
    expect(forecastChartText(mangroveFlowCard)).toContain('25 minutes posted');
    expect(forecastChartText(mangroveFlowCard)).toContain('30 minutes 35 minutes');
  });
});
