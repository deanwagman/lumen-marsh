import '@testing-library/jest-dom/vitest';

import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { resetDashboardInvalidation } from '@/features/dashboard/api/dashboardInvalidation';
import { server, resetIncidents, resetWeatherInbox } from './server';

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  resetWeatherInbox();
  resetIncidents();
  resetDashboardInvalidation();
  server.resetHandlers();
  cleanup();
});

afterAll(() => {
  server.close();
});
