import type {
  MaintenanceAsset,
  MaintenanceWorkOrder,
  ReliabilityRecommendation,
} from '@/features/maintenance/domain/maintenance';

export const CYPRESS_COIL_ATTRACTION_ASSET_ID = 'a1111111-1111-4111-8111-111111111111';
export const CYPRESS_COIL_RIDE_SYSTEM_ID = 'a2222222-2222-4222-8222-222222222222';
export const CYPRESS_COIL_TRAIN_ID = 'a3333333-3333-4333-8333-333333333333';
export const CYPRESS_COIL_WHEEL_ID = '0fd7c7ce-f7af-4b65-8789-27679ca40303';
export const CYPRESS_COIL_SENSOR_ID = 'a5555555-5555-4555-8555-555555555555';
export const VIBRATION_RECOMMENDATION_ID = 'b7e2c1a0-4c11-4c11-8c11-27679ca40303';

const now = '2026-09-14T18:25:00Z';

export const cypressCoilAssets: MaintenanceAsset[] = [
  {
    id: CYPRESS_COIL_ATTRACTION_ASSET_ID,
    assetCode: 'CC-ATTRACTION',
    name: 'Cypress Coil',
    assetType: 'ATTRACTION',
    attractionId: 'cypress-coil',
    parentAssetId: null,
    criticality: 'CRITICAL',
    serviceStatus: 'IN_SERVICE',
    manufacturer: 'Lumen Marsh Ride Systems',
    model: 'Coil',
    installedAt: '2024-03-01T00:00:00Z',
    version: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: CYPRESS_COIL_RIDE_SYSTEM_ID,
    assetCode: 'CC-RIDE-SYSTEM',
    name: 'Cypress Coil Ride System',
    assetType: 'SYSTEM',
    attractionId: 'cypress-coil',
    parentAssetId: CYPRESS_COIL_ATTRACTION_ASSET_ID,
    criticality: 'CRITICAL',
    serviceStatus: 'IN_SERVICE',
    manufacturer: 'Lumen Marsh Ride Systems',
    model: 'Launch',
    installedAt: '2024-03-01T00:00:00Z',
    version: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: CYPRESS_COIL_TRAIN_ID,
    assetCode: 'CC-TRAIN-01',
    name: 'Cypress Coil Train 1',
    assetType: 'VEHICLE',
    attractionId: 'cypress-coil',
    parentAssetId: CYPRESS_COIL_RIDE_SYSTEM_ID,
    criticality: 'HIGH',
    serviceStatus: 'IN_SERVICE',
    manufacturer: 'Lumen Marsh Ride Systems',
    model: 'Train-01',
    installedAt: '2024-03-01T00:00:00Z',
    version: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: CYPRESS_COIL_WHEEL_ID,
    assetCode: 'CC-TRAIN-01-WHEEL-A',
    name: 'Cypress Coil Train 1 Wheel Assembly A',
    assetType: 'COMPONENT',
    attractionId: 'cypress-coil',
    parentAssetId: CYPRESS_COIL_TRAIN_ID,
    criticality: 'HIGH',
    serviceStatus: 'IN_SERVICE',
    manufacturer: 'Lumen Marsh Ride Systems',
    model: 'Wheel-A',
    installedAt: '2024-03-01T00:00:00Z',
    version: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: CYPRESS_COIL_SENSOR_ID,
    assetCode: 'CC-TRAIN-01-VIB-01',
    name: 'Cypress Coil Train 1 Vibration Sensor',
    assetType: 'SENSOR',
    attractionId: 'cypress-coil',
    parentAssetId: CYPRESS_COIL_TRAIN_ID,
    criticality: 'HIGH',
    serviceStatus: 'IN_SERVICE',
    manufacturer: 'Marsh Instruments',
    model: 'VIB-01',
    installedAt: '2024-03-01T00:00:00Z',
    version: 1,
    createdAt: now,
    updatedAt: now,
  },
];

export const vibrationRecommendation: ReliabilityRecommendation = {
  recommendationId: VIBRATION_RECOMMENDATION_ID,
  observationId: 'vibration-cc-train-01-20260914T182500Z',
  status: 'PENDING_REVIEW',
  assetCode: 'CC-TRAIN-01-WHEEL-A',
  assetId: CYPRESS_COIL_WHEEL_ID,
  signalType: 'VIBRATION',
  severity: 'CRITICAL',
  value: 18.4,
  unit: 'mm/s',
  evidence:
    'Simulated vibration peak on Cypress Coil Train 1 Wheel Assembly A. Demonstration telemetry, not a live ride sensor.',
  recommendedAction: 'Inspect wheel assembly and verify sensor calibration before return to service.',
  workOrderId: null,
  observedAt: now,
  receivedAt: now,
  updatedAt: now,
  version: 1,
};

export function emptyChecklist() {
  return [
    {
      id: 'c1111111-1111-4111-8111-111111111111',
      sequence: 1,
      label: 'Inspect wheel assembly',
      instructions: 'Inspect bearings, fasteners, and visible wear on Wheel Assembly A.',
      required: true,
      result: 'PENDING' as const,
      notes: null,
      completedByDisplayName: null,
      completedAt: null,
      version: 1,
    },
    {
      id: 'c2222222-2222-4222-8222-222222222222',
      sequence: 2,
      label: 'Verify sensor calibration',
      instructions: 'Confirm the vibration sensor reports within expected idle range.',
      required: true,
      result: 'PENDING' as const,
      notes: null,
      completedByDisplayName: null,
      completedAt: null,
      version: 1,
    },
  ];
}

export function draftCorrectiveWorkOrder(
  overrides: Partial<MaintenanceWorkOrder> = {},
): MaintenanceWorkOrder {
  return {
    id: 'd1111111-1111-4111-8111-111111111111',
    workOrderNumber: 'LM-2026-0001',
    status: 'DRAFT',
    priority: 'P1',
    classification: 'CORRECTIVE',
    sourceType: 'TELEMETRY',
    sourceReferenceId: vibrationRecommendation.observationId,
    asset: {
      id: CYPRESS_COIL_WHEEL_ID,
      assetCode: 'CC-TRAIN-01-WHEEL-A',
      name: 'Cypress Coil Train 1 Wheel Assembly A',
    },
    attractionId: 'cypress-coil',
    incident: null,
    summary: vibrationRecommendation.recommendedAction,
    description: vibrationRecommendation.evidence,
    assignedTeam: null,
    assignedActorSubject: null,
    estimatedRestoreAt: null,
    openedAt: null,
    workStartedAt: null,
    readyForTestingAt: null,
    completedAt: null,
    canceledAt: null,
    recommendedAttractionAction: {
      command: 'REPORT_TECHNICAL_FAULT',
      reason: 'P1 corrective work order LM-2026-0001 is active.',
    },
    checklist: emptyChecklist(),
    evidence: [],
    version: 1,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}
