export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly body: unknown;

  constructor(
    message: string,
    status: number,
    options?: { code?: string; body?: unknown; cause?: unknown },
  ) {
    super(message, options?.cause !== undefined ? { cause: options.cause } : undefined);
    this.name = new.target.name;
    this.status = status;
    this.code = options?.code;
    this.body = options?.body;
  }
}

export class NetworkError extends Error {
  constructor(message = 'Unable to reach the VenueOps API.', cause?: unknown) {
    super(message, cause !== undefined ? { cause } : undefined);
    this.name = 'NetworkError';
  }
}

export class ValidationError extends Error {
  readonly issues: unknown;

  constructor(message: string, issues?: unknown) {
    super(message);
    this.name = 'ValidationError';
    this.issues = issues;
  }
}

export class UnauthorizedError extends ApiError {
  constructor(message = 'Authentication is required.', body?: unknown) {
    super(message, 401, { body });
  }
}

export class ForbiddenError extends ApiError {
  constructor(message = 'You are not allowed to perform this action.', body?: unknown) {
    super(message, 403, { body });
  }
}

export class NotFoundError extends ApiError {
  constructor(message = 'The requested resource was not found.', body?: unknown) {
    super(message, 404, { body });
  }
}

export class VersionConflictError extends ApiError {
  readonly expectedVersion: number | undefined;
  readonly actualVersion: number | undefined;

  constructor(
    message = 'The attraction changed since it was last read.',
    options?: {
      expectedVersion?: number;
      actualVersion?: number;
      body?: unknown;
    },
  ) {
    super(message, 409, {
      code: 'STALE_VERSION',
      body: options?.body,
    });
    this.expectedVersion = options?.expectedVersion;
    this.actualVersion = options?.actualVersion;
  }
}

export class InvalidTransitionError extends ApiError {
  readonly currentStatus: string | undefined;
  readonly command: string | undefined;

  constructor(
    message = 'That command is not valid for the current attraction state.',
    options?: { currentStatus?: string; command?: string; body?: unknown },
  ) {
    super(message, 409, {
      code: 'INVALID_TRANSITION',
      body: options?.body,
    });
    this.currentStatus = options?.currentStatus;
    this.command = options?.command;
  }
}

export class DuplicateCommandError extends ApiError {
  readonly commandId: string | undefined;
  readonly existingAggregateId: string | undefined;
  readonly requestedAggregateId: string | undefined;

  constructor(
    message = 'That command identifier already belongs to another resource.',
    options?: {
      commandId?: string;
      existingAggregateId?: string;
      requestedAggregateId?: string;
      body?: unknown;
    },
  ) {
    super(message, 409, {
      code: 'DUPLICATE_COMMAND',
      body: options?.body,
    });
    this.commandId = options?.commandId;
    this.existingAggregateId = options?.existingAggregateId;
    this.requestedAggregateId = options?.requestedAggregateId;
  }
}

export class MaintenancePrerequisiteError extends ApiError {
  constructor(
    message = 'A maintenance prerequisite has not been met.',
    body?: unknown,
  ) {
    super(message, 422, {
      code: 'MAINTENANCE_PREREQUISITE',
      body,
    });
  }
}

export class ServerError extends ApiError {
  constructor(
    message = 'The VenueOps API failed.',
    status = 500,
    options?: { code?: string; body?: unknown },
  ) {
    super(message, status, options);
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === 'AbortError'
    : error instanceof Error && error.name === 'AbortError';
}
