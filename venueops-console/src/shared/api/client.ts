import { z } from 'zod';

import {
  ForbiddenError,
  InvalidTransitionError,
  isAbortError,
  NetworkError,
  NotFoundError,
  ServerError,
  UnauthorizedError,
  ValidationError,
  VersionConflictError,
} from './errors';

const problemDetailSchema = z
  .object({
    title: z.string().optional(),
    detail: z.string().optional(),
    status: z.number().optional(),
    code: z.string().optional(),
    expectedVersion: z.number().optional(),
    actualVersion: z.number().optional(),
    currentStatus: z.string().optional(),
    command: z.string().optional(),
  })
  .loose();

export type ProblemDetail = z.infer<typeof problemDetailSchema>;

export function resolveApiUrl(baseUrl: string, path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (!baseUrl) {
    return normalizedPath;
  }
  return `${baseUrl.replace(/\/$/, '')}${normalizedPath}`;
}

export type ApiRequestOptions = {
  signal?: AbortSignal;
  headers?: HeadersInit;
};

export type ApiClientOptions = {
  baseUrl: string;
  fetchImpl?: typeof fetch;
  getAccessToken?: () => string | undefined;
  onUnauthorized?: () => void;
  onForbidden?: () => void;
};

export class ApiClient {
  readonly baseUrl: string;
  private readonly fetchImpl: typeof fetch;
  private readonly getAccessToken: (() => string | undefined) | undefined;
  private readonly onUnauthorized: (() => void) | undefined;
  private readonly onForbidden: (() => void) | undefined;

  constructor(options: ApiClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/$/, '');
    this.fetchImpl = options.fetchImpl ?? ((...args) => fetch(...args));
    this.getAccessToken = options.getAccessToken;
    this.onUnauthorized = options.onUnauthorized;
    this.onForbidden = options.onForbidden;
  }

  get<T>(path: string, schema: z.ZodType<T>, options: ApiRequestOptions = {}): Promise<T> {
    return this.request(path, schema, { ...options, method: 'GET' });
  }

  post<T>(
    path: string,
    schema: z.ZodType<T>,
    body: unknown,
    options: ApiRequestOptions = {},
  ): Promise<T> {
    return this.request(path, schema, { ...options, method: 'POST', body });
  }

  private async request<T>(
    path: string,
    schema: z.ZodType<T>,
    options: ApiRequestOptions & { method: string; body?: unknown },
  ): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');

    const accessToken = this.getAccessToken?.();
    if (accessToken) {
      headers.set('Authorization', `Bearer ${accessToken}`);
    }

    if (options.body !== undefined) {
      headers.set('Content-Type', 'application/json');
    }

    let response: Response;
    try {
      response = await this.fetchImpl(resolveApiUrl(this.baseUrl, path), {
        method: options.method,
        headers,
        signal: options.signal,
        body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      });
    } catch (error) {
      if (isAbortError(error)) {
        throw error;
      }
      throw new NetworkError('Unable to reach the VenueOps API.', error);
    }

    if (!response.ok) {
      if (response.status === 401) this.onUnauthorized?.();
      if (response.status === 403) this.onForbidden?.();
      throw await errorFromResponse(response);
    }

    const text = await response.text();
    let payload: unknown;
    if (text.length > 0) {
      try {
        payload = JSON.parse(text) as unknown;
      } catch {
        throw new ValidationError('Response was not valid JSON.', text);
      }
    }

    const parsed = schema.safeParse(payload);
    if (!parsed.success) {
      throw new ValidationError(
        'Response did not match the expected VenueOps contract.',
        parsed.error,
      );
    }

    return parsed.data;
  }
}

async function errorFromResponse(response: Response): Promise<Error> {
  const problem = await readProblemDetail(response);
  const detail = problem?.detail;

  switch (response.status) {
    case 401:
      return new UnauthorizedError(detail ?? 'Authentication is required.', problem);
    case 403:
      return new ForbiddenError(
        detail ?? 'You are not allowed to perform this action.',
        problem,
      );
    case 404:
      return new NotFoundError(detail ?? 'The requested resource was not found.', problem);
    case 409:
      if (problem?.code === 'STALE_VERSION') {
        return new VersionConflictError(detail, {
          expectedVersion: problem.expectedVersion,
          actualVersion: problem.actualVersion,
          body: problem,
        });
      }
      if (problem?.code === 'INVALID_TRANSITION') {
        return new InvalidTransitionError(detail, {
          currentStatus: problem.currentStatus,
          command: problem.command,
          body: problem,
        });
      }
      return new ServerError(
        detail ?? 'The request conflicted with current attraction state.',
        409,
        { code: problem?.code, body: problem },
      );
    default:
      return new ServerError(
        detail ?? `Request failed with status ${response.status}.`,
        response.status,
        { code: problem?.code, body: problem },
      );
  }
}

async function readProblemDetail(response: Response): Promise<ProblemDetail | undefined> {
  const text = await response.text();
  if (!text) {
    return undefined;
  }

  try {
    const parsed = problemDetailSchema.safeParse(JSON.parse(text) as unknown);
    return parsed.success ? parsed.data : undefined;
  } catch {
    return undefined;
  }
}
