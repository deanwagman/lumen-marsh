import { describe, expect, it, vi } from 'vitest';

import { attractionListSchema } from '@/features/attractions/api/attractionSchema';
import { ApiClient } from '@/shared/api/client';
import {
  DuplicateCommandError,
  ForbiddenError,
  InvalidTransitionError,
  MaintenancePrerequisiteError,
  NetworkError,
  NotFoundError,
  UnauthorizedError,
  ValidationError,
  VersionConflictError,
} from '@/shared/api/errors';
import { catalogAttractions } from '@/test/fixtures';

function jsonResponse(body: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('ApiClient', () => {
  it('validates successful JSON payloads', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(catalogAttractions));
    const client = new ApiClient({ baseUrl: '', fetchImpl });

    await expect(client.get('/api/v1/attractions', attractionListSchema)).resolves.toEqual(
      catalogAttractions,
    );
  });

  it('rejects payloads that do not match the attraction contract', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse([{ id: 'mangrove-run', name: 'Mangrove Run' }]),
    );
    const client = new ApiClient({ baseUrl: '', fetchImpl });

    await expect(client.get('/api/v1/attractions', attractionListSchema)).rejects.toBeInstanceOf(
      ValidationError,
    );
  });

  it('normalizes network failures', async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    const client = new ApiClient({ baseUrl: 'http://localhost:8080', fetchImpl });

    await expect(client.get('/api/v1/attractions', attractionListSchema)).rejects.toBeInstanceOf(
      NetworkError,
    );
  });

  it('parses a stale version conflict', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          title: 'Stale attraction version',
          status: 409,
          detail: 'Attraction mangrove-run is at version 3',
          code: 'STALE_VERSION',
          expectedVersion: 0,
          actualVersion: 3,
        },
        { status: 409 },
      ),
    );
    const client = new ApiClient({ baseUrl: '', fetchImpl });

    const error = await client
      .get('/api/v1/attractions', attractionListSchema)
      .catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(VersionConflictError);
    expect(error).toMatchObject({ expectedVersion: 0, actualVersion: 3 });
  });

  it('maps a stale version that reports currentVersion', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          title: 'Stale work-order version',
          status: 409,
          detail: 'Work order changed',
          code: 'STALE_VERSION',
          expectedVersion: 2,
          currentVersion: 4,
        },
        { status: 409 },
      ),
    );
    const client = new ApiClient({ baseUrl: '', fetchImpl });
    const error = await client
      .get('/api/v1/operator/maintenance/work-orders/x', attractionListSchema)
      .catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(VersionConflictError);
    expect(error).toMatchObject({ expectedVersion: 2, actualVersion: 4 });
  });

  it('parses an invalid transition conflict', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          title: 'Invalid attraction transition',
          status: 409,
          detail: 'Cannot apply APPROVE_RETURN_TO_SERVICE when attraction is WEATHER_HOLD',
          code: 'INVALID_TRANSITION',
          currentStatus: 'WEATHER_HOLD',
          command: 'APPROVE_RETURN_TO_SERVICE',
        },
        { status: 409 },
      ),
    );
    const client = new ApiClient({ baseUrl: '', fetchImpl });

    const error = await client
      .get('/api/v1/attractions', attractionListSchema)
      .catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(InvalidTransitionError);
    expect(error).toMatchObject({
      currentStatus: 'WEATHER_HOLD',
      command: 'APPROVE_RETURN_TO_SERVICE',
    });
  });

  it('maps duplicate command and maintenance prerequisite failures', async () => {
    const duplicateClient = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(
        jsonResponse(
          {
            code: 'DUPLICATE_COMMAND',
            detail: 'Command belongs to another resource',
            commandId: '11111111-1111-4111-8111-111111111111',
            existingAggregateId: 'aaa',
            requestedAggregateId: 'bbb',
          },
          { status: 409 },
        ),
      ),
    });
    const duplicate = await duplicateClient
      .get('/api/v1/operator/maintenance/work-orders/x', attractionListSchema)
      .catch((caught: unknown) => caught);
    expect(duplicate).toBeInstanceOf(DuplicateCommandError);

    const prerequisiteClient = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(
        jsonResponse(
          {
            code: 'MAINTENANCE_PREREQUISITE',
            detail: 'Required checklist items must be passed',
          },
          { status: 422 },
        ),
      ),
    });
    const prerequisite = await prerequisiteClient
      .get('/api/v1/operator/maintenance/work-orders/x', attractionListSchema)
      .catch((caught: unknown) => caught);
    expect(prerequisite).toBeInstanceOf(MaintenancePrerequisiteError);
  });

  it('maps not found and unauthorized statuses', async () => {
    const notFound = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(
        jsonResponse({ code: 'ATTRACTION_NOT_FOUND', detail: 'missing' }, { status: 404 }),
      ),
    });
    await expect(notFound.get('/api/v1/attractions/missing', attractionListSchema)).rejects.toBeInstanceOf(
      NotFoundError,
    );

    const unauthorized = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(jsonResponse({ detail: 'login' }, { status: 401 })),
    });
    await expect(
      unauthorized.get('/api/v1/attractions', attractionListSchema),
    ).rejects.toBeInstanceOf(UnauthorizedError);
  });

  it('sends the access token as a bearer credential', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(catalogAttractions));
    const client = new ApiClient({
      baseUrl: '',
      fetchImpl,
      getAccessToken: () => 'test-access-token',
    });

    await client.get('/api/v1/attractions', attractionListSchema);

    const headers = new Headers(fetchImpl.mock.calls[0][1].headers as HeadersInit);
    expect(headers.get('Authorization')).toBe('Bearer test-access-token');
    expect(headers.has('X-Actor')).toBe(false);
  });

  it('forwards abort signals for request cancellation', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(catalogAttractions));
    const client = new ApiClient({ baseUrl: '', fetchImpl });
    const controller = new AbortController();

    await client.get('/api/v1/attractions', attractionListSchema, {
      signal: controller.signal,
    });

    expect(fetchImpl.mock.calls[0][1].signal).toBe(controller.signal);
  });

  it('reports GET 403 as console access-denied but keeps command 403s inline', async () => {
    const onForbidden = vi.fn();
    const getClient = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(jsonResponse({ detail: 'Need inspect' }, { status: 403 })),
      onForbidden,
    });
    await expect(getClient.get('/api/v1/operator/maintenance/work-orders', attractionListSchema)).rejects.toBeInstanceOf(
      ForbiddenError,
    );
    expect(onForbidden).toHaveBeenCalledOnce();

    const postClient = new ApiClient({
      baseUrl: '',
      fetchImpl: vi.fn().mockResolvedValue(jsonResponse({ detail: 'Need inspect' }, { status: 403 })),
      onForbidden,
    });
    await expect(
      postClient.post('/api/v1/operator/maintenance/work-orders/x/commands', attractionListSchema, {}),
    ).rejects.toBeInstanceOf(ForbiddenError);
    expect(onForbidden).toHaveBeenCalledOnce();
  });
});
