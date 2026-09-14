import { environment } from '@/config/environment';

import { ApiClient } from './client';

export const defaultApiClient = new ApiClient({
  baseUrl: environment.apiBaseUrl,
});
