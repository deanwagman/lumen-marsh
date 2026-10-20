import type {
  AnalyticsAnswer,
  AvailabilityDetail,
  AvailabilitySummary,
  Citation,
  HistoricalDataset,
} from '@/features/history/domain/history';

export const historyDataset: HistoricalDataset = {
  id: 'lumen-marsh-availability-demo-v1',
  version: '1',
  seed: 20261013,
  manifestChecksum: 'fixture-checksum',
  fromDate: '2026-10-13',
  toDate: '2026-10-15',
  state: 'COMPLETED',
  createdAt: '2026-10-13T00:00:00Z',
  completedAt: '2026-10-15T23:00:00Z',
};

const cypressCitation: Citation = {
  id: 'segment-cypress-technical',
  datasetId: historyDataset.id,
  attractionId: 'cypress-coil',
  parkDate: '2026-10-15',
  activityId: 'activity-cypress-fault',
  commandId: 'command-cypress-fault',
  controlTowerUrl:
    '/history?datasetId=lumen-marsh-availability-demo-v1&date=2026-10-15&attractionId=cypress-coil&citation=segment-cypress-technical',
};

const emptyDowntime = {
  OPERATING: 0,
  REDUCED_CAPACITY: 0,
  WEATHER_HOLD: 0,
  TECHNICAL_DELAY: 0,
  TESTING_RETURN_TO_SERVICE: 0,
  COMMANDED_CLOSURE: 0,
  CLOSED_NOT_OPEN: 0,
};

export const availabilitySummaryFixture: AvailabilitySummary = {
  datasetId: historyDataset.id,
  from: historyDataset.fromDate,
  to: historyDataset.toDate,
  zoneId: 'America/New_York',
  attractions: [
    {
      attractionId: 'cypress-coil',
      attractionName: 'Cypress Coil',
      scheduledMinutes: 1440,
      operatingMinutes: 1250,
      uptimePercentage: 86.81,
      degradedMinutes: 0,
      downtimeMinutes: {
        ...emptyDowntime,
        TECHNICAL_DELAY: 150,
        TESTING_RETURN_TO_SERVICE: 40,
      },
      longestOutageMinutes: 190,
      citations: [cypressCitation],
    },
    {
      attractionId: 'stormglass-station',
      attractionName: 'Stormglass Station',
      scheduledMinutes: 1440,
      operatingMinutes: 1360,
      uptimePercentage: 94.44,
      degradedMinutes: 0,
      downtimeMinutes: {
        ...emptyDowntime,
        WEATHER_HOLD: 60,
        TESTING_RETURN_TO_SERVICE: 20,
      },
      longestOutageMinutes: 80,
      citations: [],
    },
    {
      attractionId: 'mangrove-run',
      attractionName: 'Mangrove Run',
      scheduledMinutes: 1440,
      operatingMinutes: 1440,
      uptimePercentage: 100,
      degradedMinutes: 60,
      downtimeMinutes: emptyDowntime,
      longestOutageMinutes: 0,
      citations: [],
    },
  ],
};

export const availabilityDetailFixture: AvailabilityDetail = {
  datasetId: historyDataset.id,
  attractionId: 'cypress-coil',
  attractionName: 'Cypress Coil',
  parkDate: '2026-10-15',
  zoneId: 'America/New_York',
  expectedStart: '2026-10-15T14:00:00Z',
  expectedEnd: '2026-10-15T22:00:00Z',
  plannedClosure: false,
  plannedClosureReason: null,
  scheduledMinutes: 480,
  operatingMinutes: 290,
  uptimePercentage: 60.42,
  degradedMinutes: 0,
  downtimeMinutes: {
    ...emptyDowntime,
    TECHNICAL_DELAY: 150,
    TESTING_RETURN_TO_SERVICE: 40,
  },
  longestOutageMinutes: 190,
  segments: [
    {
      citationId: 'segment-cypress-operating',
      status: 'OPERATING',
      classification: 'OPERATING',
      start: '2026-10-15T14:00:00Z',
      end: '2026-10-15T16:00:00Z',
      durationMinutes: 120,
      reason: 'Opened for service',
      commandId: 'command-open',
      activityId: 'activity-open',
      datasetId: historyDataset.id,
      controlTowerUrl: '/history?citation=segment-cypress-operating',
    },
    {
      citationId: cypressCitation.id,
      status: 'TECHNICAL_DELAY',
      classification: 'TECHNICAL_DELAY',
      start: '2026-10-15T16:00:00Z',
      end: '2026-10-15T18:30:00Z',
      durationMinutes: 150,
      reason: 'Drive fault',
      commandId: cypressCitation.commandId,
      activityId: cypressCitation.activityId,
      datasetId: historyDataset.id,
      controlTowerUrl: cypressCitation.controlTowerUrl,
    },
    {
      citationId: 'segment-cypress-testing',
      status: 'TESTING',
      classification: 'TESTING_RETURN_TO_SERVICE',
      start: '2026-10-15T18:30:00Z',
      end: '2026-10-15T19:10:00Z',
      durationMinutes: 40,
      reason: 'Return-to-service testing',
      commandId: 'command-testing',
      activityId: 'activity-testing',
      datasetId: historyDataset.id,
      controlTowerUrl: '/history?citation=segment-cypress-testing',
    },
    {
      citationId: 'segment-cypress-restored',
      status: 'OPERATING',
      classification: 'OPERATING',
      start: '2026-10-15T19:10:00Z',
      end: '2026-10-15T22:00:00Z',
      durationMinutes: 170,
      reason: 'Restored',
      commandId: 'command-restored',
      activityId: 'activity-restored',
      datasetId: historyDataset.id,
      controlTowerUrl: '/history?citation=segment-cypress-restored',
    },
  ],
};

export const analyticsAnswerFixture: AnalyticsAnswer = {
  datasetId: historyDataset.id,
  answer:
    'Cypress Coil had the worst uptime at 86.81% (1250 of 1440 scheduled minutes operating). Longest outage: 190 minutes.',
  claims: [
    {
      text: 'Cypress Coil had the worst uptime at 77.08%.',
      citations: [cypressCitation],
    },
  ],
  provider: 'deterministic-curated-availability',
};
