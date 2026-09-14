export type AccessTokenClaims = {
  issuer?: string;
  audience?: string | string[];
  clientId?: string;
  subject?: string;
  tokenUse?: string;
  scope?: string;
  groups: string[];
  expiresAt?: number;
};

/**
 * Reads JWT payload claims for the local login inspector.
 * Does not return the raw token or signature.
 */
export function readAccessTokenClaims(accessToken: string): AccessTokenClaims | null {
  const parts = accessToken.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const json = decodeBase64Url(parts[1]);
    const payload = JSON.parse(json) as Record<string, unknown>;
    return {
      issuer: typeof payload.iss === 'string' ? payload.iss : undefined,
      audience: audience(payload.aud),
      clientId: typeof payload.client_id === 'string' ? payload.client_id : undefined,
      subject: typeof payload.sub === 'string' ? payload.sub : undefined,
      tokenUse: typeof payload.token_use === 'string' ? payload.token_use : undefined,
      scope: typeof payload.scope === 'string' ? payload.scope : undefined,
      groups: groups(payload['cognito:groups']),
      expiresAt: typeof payload.exp === 'number' ? payload.exp : undefined,
    };
  } catch {
    return null;
  }
}

function audience(value: unknown): string | string[] | undefined {
  if (typeof value === 'string') {
    return value;
  }
  if (Array.isArray(value) && value.every((item) => typeof item === 'string')) {
    return value;
  }
  return undefined;
}

function groups(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === 'string');
}

function decodeBase64Url(value: string): string {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  return atob(padded);
}
