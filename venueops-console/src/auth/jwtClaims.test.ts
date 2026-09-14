import { describe, expect, it } from 'vitest';

import { readAccessTokenClaims } from './jwtClaims';

describe('readAccessTokenClaims', () => {
  it('returns issuer, audience, subject, groups, and scopes without the token', () => {
    const payload = {
      iss: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example',
      aud: 'venueops',
      client_id: 'venueops-console',
      sub: 'operator-sub-1',
      token_use: 'access',
      scope: 'openid venueops/attractions.command',
      'cognito:groups': ['operators'],
      exp: 1_704_000_000,
    };
    const token = `header.${btoa(JSON.stringify(payload))}.signature`;

    expect(readAccessTokenClaims(token)).toEqual({
      issuer: payload.iss,
      audience: 'venueops',
      clientId: 'venueops-console',
      subject: 'operator-sub-1',
      tokenUse: 'access',
      scope: 'openid venueops/attractions.command',
      groups: ['operators'],
      expiresAt: 1_704_000_000,
    });
  });

  it('returns null for opaque local-development tokens', () => {
    expect(readAccessTokenClaims('local-development-token')).toBeNull();
  });
});
