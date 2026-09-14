import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { AttractionEventSource } from '@/features/attractions/api/AttractionEventSource';

export function AttractionLiveUpdates() {
  const queryClient = useQueryClient();
  const auth = useAuth();

  useEffect(
    () => {
      if (auth.status !== 'authenticated') return;
      return AttractionEventSource.connect(queryClient, {
        getAccessToken: auth.getAccessToken,
        onUnauthorized: auth.expireSession,
        onForbidden: auth.reportAccessDenied,
      });
    },
    [
      queryClient,
      auth.status,
      auth.getAccessToken,
      auth.expireSession,
      auth.reportAccessDenied,
    ],
  );

  return null;
}
