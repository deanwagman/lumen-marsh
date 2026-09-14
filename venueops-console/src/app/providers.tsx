import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { useMemo, useState, type ReactNode } from 'react';
import type { UserManager } from 'oidc-client-ts';

import { AuthProvider, useAuth } from '@/auth/AuthContext';
import type { AuthSession } from '@/auth/session';
import { environment } from '@/config/environment';
import { ApiClientProvider } from '@/shared/api/ApiClientContext';
import { ApiClient } from '@/shared/api/client';

import { createQueryClient } from './queryClient';

export function AppProviders({
  children,
  queryClient,
  apiClient,
  initialSession,
  authManager,
}: {
  children: ReactNode;
  queryClient?: QueryClient;
  apiClient?: ApiClient;
  initialSession?: AuthSession | null;
  authManager?: UserManager;
}) {
  return (
    <AuthProvider initialSession={initialSession} manager={authManager}>
      <AuthenticatedProviders queryClient={queryClient} apiClient={apiClient}>
        {children}
      </AuthenticatedProviders>
    </AuthProvider>
  );
}

function AuthenticatedProviders({
  children,
  queryClient,
  apiClient,
}: {
  children: ReactNode;
  queryClient?: QueryClient;
  apiClient?: ApiClient;
}) {
  const auth = useAuth();
  const [defaultQueryClient] = useState(() => createQueryClient());
  const authenticatedClient = useMemo(
    () =>
      apiClient ??
      new ApiClient({
        baseUrl: environment.apiBaseUrl,
        getAccessToken: auth.getAccessToken,
        onUnauthorized: auth.expireSession,
        onForbidden: auth.reportAccessDenied,
      }),
    [apiClient, auth.getAccessToken, auth.expireSession, auth.reportAccessDenied],
  );

  return (
    <ApiClientProvider client={authenticatedClient}>
      <QueryClientProvider client={queryClient ?? defaultQueryClient}>
        {children}
      </QueryClientProvider>
    </ApiClientProvider>
  );
}
