import { describe, expect, it, vi } from 'vitest';

import { attractionListSchema } from '@/features/attractions/api/attractionSchema';
import { ApiClient } from '@/shared/api/client';
import {
  InvalidTransitionError,
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
});
