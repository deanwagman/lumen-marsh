import type { QueryClient } from '@tanstack/react-query';

import {
  applyNewerIncident,
  fromOperationalSnapshot,
  type Incident,
  type IncidentOperationalSnapshot,
} from '@/features/incidents/domain/incident';
import type { ApiClient } from '@/shared/api/client';

import {
  incidentActivityListSchema,
  incidentListSchema,
  incidentSchema,
} from './incidentSchema';

export const IncidentQueries = {
  all: ['incidents'] as const,
  list: () => [...IncidentQueries.all, 'list'] as const,
  detail: (id: string) => [...IncidentQueries.all, 'detail', id] as const,
  activity: (id: string) => [...IncidentQueries.all, 'activity', id] as const,
};

export function incidentListQuery(client: ApiClient) {
  return {
    queryKey: IncidentQueries.list(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get('/api/v1/operator/incidents', incidentListSchema, { signal }),
  };
}

export function incidentDetailQuery(client: ApiClient, incidentId: string) {
  return {
    queryKey: IncidentQueries.detail(incidentId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/incidents/${encodeURIComponent(incidentId)}`,
        incidentSchema,
        { signal },
      ),
  };
}

export function incidentActivityQuery(client: ApiClient, incidentId: string) {
  return {
    queryKey: IncidentQueries.activity(incidentId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/incidents/${encodeURIComponent(incidentId)}/activity`,
        incidentActivityListSchema,
        { signal },
      ),
  };
}

export function replaceIncidentList(queryClient: QueryClient, snapshots: IncidentOperationalSnapshot[]) {
  const previous = queryClient.getQueryData<Incident[]>(IncidentQueries.list()) ?? [];
  const previousById = new Map(previous.map((incident) => [incident.id, incident]));
  const next = snapshots.map((snapshot) => {
    const current = previousById.get(snapshot.id);
    const candidate = fromOperationalSnapshot(snapshot, current);
    return applyNewerIncident(current, candidate) ?? candidate;
  });
  queryClient.setQueryData(IncidentQueries.list(), next);
  for (const incident of next) {
    const current = queryClient.getQueryData<Incident>(IncidentQueries.detail(incident.id));
    const merged = applyNewerIncident(current, incident);
    if (merged) {
      queryClient.setQueryData(IncidentQueries.detail(incident.id), merged);
    }
  }
}

export function applyIncidentUpdate(queryClient: QueryClient, snapshot: IncidentOperationalSnapshot) {
  const list = queryClient.getQueryData<Incident[]>(IncidentQueries.list()) ?? [];
  const previous = list.find((incident) => incident.id === snapshot.id);
  const detail = queryClient.getQueryData<Incident>(IncidentQueries.detail(snapshot.id));
  const next = fromOperationalSnapshot(snapshot, detail ?? previous);
  const mergedList = applyNewerIncident(previous, next) ?? next;
  const without = list.filter((incident) => incident.id !== snapshot.id);
  queryClient.setQueryData(IncidentQueries.list(), [...without, mergedList]);

  const mergedDetail = applyNewerIncident(detail, next);
  if (mergedDetail && (detail || snapshot.version >= 1)) {
    queryClient.setQueryData(IncidentQueries.detail(snapshot.id), mergedDetail);
  }

  void queryClient.invalidateQueries({
    queryKey: IncidentQueries.activity(snapshot.id),
  });
}

export function applyOperatorIncident(queryClient: QueryClient, incident: Incident) {
  const list = queryClient.getQueryData<Incident[]>(IncidentQueries.list()) ?? [];
  const without = list.filter((item) => item.id !== incident.id);
  queryClient.setQueryData(IncidentQueries.list(), [...without, incident]);
  queryClient.setQueryData(IncidentQueries.detail(incident.id), incident);
}
