import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import { AttractionEventSource } from '@/features/attractions/api/AttractionEventSource';

export function AttractionLiveUpdates() {
  const queryClient = useQueryClient();
  const auth = useAuth();
  const maintenanceRead = canReadMaintenance(auth.session);

  useEffect(
    () => {
      if (auth.status !== 'authenticated') return;
      return AttractionEventSource.connect(queryClient, {
        getAccessToken: auth.getAccessToken,
        onUnauthorized: auth.expireSession,
        onForbidden: auth.reportAccessDenied,
        includeMaintenanceEvents: maintenanceRead,
      });
    },
    [
      queryClient,
      auth.status,
      auth.getAccessToken,
      auth.expireSession,
      auth.reportAccessDenied,
      maintenanceRead,
    ],
  );

  return null;
}
