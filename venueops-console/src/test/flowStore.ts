import type { FlowOverview, FlowRecommendation } from '@/features/flow/domain/flow';
import { flowOverviewFixture, flowRecommendationFixture } from '@/features/flow/test/fixtures';

export const flowOverviewStore: { current: FlowOverview } = {
  current: structuredClone(flowOverviewFixture),
};

export const flowRecommendationStore: FlowRecommendation[] = [];
export const processedFlowCommands = new Map<string, { recommendationId: string; body: unknown }>();

export function resetFlowStores() {
  flowOverviewStore.current = structuredClone(flowOverviewFixture);
  flowRecommendationStore.splice(0, flowRecommendationStore.length, structuredClone(flowRecommendationFixture));
  processedFlowCommands.clear();
}

resetFlowStores();
