import { describe, expect, it } from 'vitest';

import { parseEnvironment } from './environment';

describe('parseEnvironment', () => {
  it('accepts an empty value for same-origin development', () => {
    expect(parseEnvironment({ VITE_API_BASE_URL: '', VITE_AUTH_MODE: 'local' })).toMatchObject({
      apiBaseUrl: '',
      usesSameOrigin: true,
      authMode: 'local',
    });
  });

  it('accepts a Spring Boot origin', () => {
    expect(
      parseEnvironment({
        VITE_API_BASE_URL: 'http://localhost:8080/',
        VITE_AUTH_MODE: 'local',
      }),
    ).toMatchObject({
      apiBaseUrl: 'http://localhost:8080',
      usesSameOrigin: false,
    });
  });

  it('rejects invalid values', () => {
    expect(() =>
      parseEnvironment({ VITE_API_BASE_URL: 'not-a-url', VITE_AUTH_MODE: 'local' }),
    ).toThrow(
      /Invalid environment configuration/,
    );
    expect(() =>
      parseEnvironment({ VITE_API_BASE_URL: 'localhost:8080', VITE_AUTH_MODE: 'local' }),
    ).toThrow(
      /Invalid environment configuration/,
    );
    expect(() =>
      parseEnvironment({ VITE_API_BASE_URL: 'ftp://localhost:8080', VITE_AUTH_MODE: 'local' }),
    ).toThrow(
      /Invalid environment configuration/,
    );
  });

  it('accepts Cognito public-client aliases', () => {
    expect(
      parseEnvironment({
        VITE_API_BASE_URL: 'http://localhost:8080',
        VITE_AUTH_MODE: 'oidc',
        VITE_OIDC_AUTHORITY: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example',
        VITE_COGNITO_CLIENT_ID: 'public-client',
        VITE_COGNITO_REDIRECT_URI: 'http://localhost:5173/auth/callback',
        VITE_COGNITO_LOGOUT_URI: 'http://localhost:5173/',
      }),
    ).toMatchObject({
      oidc: {
        clientId: 'public-client',
        redirectUri: 'http://localhost:5173/auth/callback',
        logoutUri: 'http://localhost:5173/',
      },
    });
  });
});
