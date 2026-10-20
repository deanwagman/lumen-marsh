import { http, HttpResponse } from 'msw';

import {
  analyticsAnswerFixture,
  availabilityDetailFixture,
  availabilitySummaryFixture,
  historyDataset,
} from '@/features/history/test/fixtures';

export const historyHandlers = [
  http.get('/api/v1/operator/analytics/datasets', () =>
    HttpResponse.json([historyDataset]),
  ),
  http.get('/api/v1/operator/analytics/availability', ({ request }) => {
    const url = new URL(request.url);
    return HttpResponse.json({
      ...availabilitySummaryFixture,
      from: url.searchParams.get('from') ?? availabilitySummaryFixture.from,
      to: url.searchParams.get('to') ?? availabilitySummaryFixture.to,
    });
  }),
  http.get(
    '/api/v1/operator/analytics/availability/attractions/:attractionId',
    ({ params, request }) => {
      const url = new URL(request.url);
      const attraction = availabilitySummaryFixture.attractions.find(
        (item) => item.attractionId === params.attractionId,
      );
      return HttpResponse.json({
        ...availabilityDetailFixture,
        attractionId: String(params.attractionId),
        attractionName:
          attraction?.attractionName ?? String(params.attractionId),
        parkDate:
          url.searchParams.get('date') ?? availabilityDetailFixture.parkDate,
      });
    },
  ),
  http.post('/api/v1/operator/analytics/questions', () =>
    HttpResponse.json(analyticsAnswerFixture),
  ),
];
