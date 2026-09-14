import { z } from 'zod';

const envSchema = z.object({
  VITE_API_BASE_URL: z
    .string()
    .trim()
    .refine((value) => {
      if (value === '') {
        return true;
      }

      try {
        const url = new URL(value);
        return url.protocol === 'http:' || url.protocol === 'https:';
      } catch {
        return false;
      }
    }, 'VITE_API_BASE_URL must be an http(s) URL or empty for same-origin development'),
  VITE_AUTH_MODE: z.enum(['oidc', 'local']),
  VITE_OIDC_AUTHORITY: z.string().trim(),
  VITE_OIDC_CLIENT_ID: z.string().trim(),
  VITE_OIDC_REDIRECT_URI: z.string().url().or(z.literal('')),
  VITE_OIDC_LOGOUT_URI: z.string().url().or(z.literal('')),
  VITE_OIDC_SCOPES: z.string().trim(),
});

export type AppEnvironment = {
  apiBaseUrl: string;
  usesSameOrigin: boolean;
  authMode: 'oidc' | 'local';
  oidc: {
    authority: string;
    clientId: string;
    redirectUri: string;
    logoutUri: string;
    scopes: string;
  };
};

export type EnvironmentInput = {
  VITE_API_BASE_URL?: string;
  VITE_AUTH_MODE?: string;
  VITE_OIDC_AUTHORITY?: string;
  VITE_OIDC_CLIENT_ID?: string;
  VITE_OIDC_REDIRECT_URI?: string;
  VITE_OIDC_LOGOUT_URI?: string;
  VITE_OIDC_SCOPES?: string;
  VITE_COGNITO_CLIENT_ID?: string;
  VITE_COGNITO_REDIRECT_URI?: string;
  VITE_COGNITO_LOGOUT_URI?: string;
};

export function parseEnvironment(env: EnvironmentInput): AppEnvironment {
  const parsed = envSchema.safeParse({
    VITE_API_BASE_URL: env.VITE_API_BASE_URL ?? '',
    VITE_AUTH_MODE: env.VITE_AUTH_MODE ?? 'oidc',
    VITE_OIDC_AUTHORITY: env.VITE_OIDC_AUTHORITY ?? '',
    VITE_OIDC_CLIENT_ID: env.VITE_OIDC_CLIENT_ID || env.VITE_COGNITO_CLIENT_ID || '',
    VITE_OIDC_REDIRECT_URI: env.VITE_OIDC_REDIRECT_URI || env.VITE_COGNITO_REDIRECT_URI || '',
    VITE_OIDC_LOGOUT_URI: env.VITE_OIDC_LOGOUT_URI || env.VITE_COGNITO_LOGOUT_URI || '',
    VITE_OIDC_SCOPES: env.VITE_OIDC_SCOPES ?? 'openid profile email',
  });

  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join('; ');
    throw new Error(`Invalid environment configuration: ${message}`);
  }

  const apiBaseUrl = parsed.data.VITE_API_BASE_URL.replace(/\/$/, '');
  if (
    parsed.data.VITE_AUTH_MODE === 'oidc' &&
    (!parsed.data.VITE_OIDC_AUTHORITY ||
      !parsed.data.VITE_OIDC_CLIENT_ID ||
      !parsed.data.VITE_OIDC_REDIRECT_URI ||
      !parsed.data.VITE_OIDC_LOGOUT_URI)
  ) {
    throw new Error(
      'Invalid environment configuration: OIDC authority, client ID, redirect URI, and logout URI are required',
    );
  }

  return {
    apiBaseUrl,
    usesSameOrigin: apiBaseUrl === '',
    authMode: parsed.data.VITE_AUTH_MODE,
    oidc: {
      authority: parsed.data.VITE_OIDC_AUTHORITY.replace(/\/$/, ''),
      clientId: parsed.data.VITE_OIDC_CLIENT_ID,
      redirectUri: parsed.data.VITE_OIDC_REDIRECT_URI,
      logoutUri: parsed.data.VITE_OIDC_LOGOUT_URI,
      scopes: parsed.data.VITE_OIDC_SCOPES,
    },
  };
}

export const environment = parseEnvironment({
  VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
  VITE_AUTH_MODE:
    import.meta.env.VITE_AUTH_MODE ?? (import.meta.env.MODE === 'test' ? 'local' : undefined),
  VITE_OIDC_AUTHORITY: import.meta.env.VITE_OIDC_AUTHORITY,
  VITE_OIDC_CLIENT_ID: import.meta.env.VITE_OIDC_CLIENT_ID,
  VITE_OIDC_REDIRECT_URI: import.meta.env.VITE_OIDC_REDIRECT_URI,
  VITE_OIDC_LOGOUT_URI: import.meta.env.VITE_OIDC_LOGOUT_URI,
  VITE_OIDC_SCOPES: import.meta.env.VITE_OIDC_SCOPES,
  VITE_COGNITO_CLIENT_ID: import.meta.env.VITE_COGNITO_CLIENT_ID,
  VITE_COGNITO_REDIRECT_URI: import.meta.env.VITE_COGNITO_REDIRECT_URI,
  VITE_COGNITO_LOGOUT_URI: import.meta.env.VITE_COGNITO_LOGOUT_URI,
});
