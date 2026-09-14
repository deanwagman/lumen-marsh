import type { ReactNode } from 'react';

import { ApiClient } from './client';
import { defaultApiClient } from './defaultApiClient';
import { ApiClientContext } from './useApiClient';

export function ApiClientProvider({
  client = defaultApiClient,
  children,
}: {
  client?: ApiClient;
  children: ReactNode;
}) {
  return <ApiClientContext.Provider value={client}>{children}</ApiClientContext.Provider>;
}
