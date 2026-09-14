import { useEffect, useState } from 'react';

import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import { formatCapacity, formatStatus } from '@/features/attractions/domain/formatters';

export type LiveAttractionFlash = {
  id: string;
  version: number;
};

function snapshot(attractions: AttractionSummary[]) {
  return new Map(
    attractions.map((attraction) => [
      attraction.id,
      {
        version: attraction.version,
        status: attraction.status,
        capacityMode: attraction.capacityMode,
        name: attraction.name,
      },
    ]),
  );
}

export function useLiveAttractionChanges(attractions: AttractionSummary[] | undefined) {
  const items = attractions ?? [];
  const signature = items.map((attraction) => `${attraction.id}:${attraction.version}`).join('|');
  const [state, setState] = useState(() => ({
    signature,
    seen: snapshot(items),
    flashes: [] as LiveAttractionFlash[],
    announcement: '',
  }));

  if (signature !== state.signature) {
    const flashes: LiveAttractionFlash[] = [];
    const messages: string[] = [];
    const seen = new Map(state.seen);

    for (const attraction of items) {
      const previous = seen.get(attraction.id);
      if (previous && previous.version !== attraction.version) {
        flashes.push({ id: attraction.id, version: attraction.version });
        if (previous.status !== attraction.status) {
          messages.push(`${attraction.name} is now ${formatStatus(attraction.status)}`);
        } else if (previous.capacityMode !== attraction.capacityMode) {
          messages.push(
            `${attraction.name} is now ${formatCapacity(attraction.capacityMode)} capacity`,
          );
        }
      }
      seen.set(attraction.id, {
        version: attraction.version,
        status: attraction.status,
        capacityMode: attraction.capacityMode,
        name: attraction.name,
      });
    }

    setState({
      signature,
      seen,
      flashes,
      announcement: messages.join('. '),
    });
  }

  useEffect(() => {
    if (state.flashes.length === 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setState((current) =>
        current.flashes.length === 0 ? current : { ...current, flashes: [] },
      );
    }, 8_000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [state.flashes, state.signature]);

  return {
    flashes: state.flashes,
    announcement: state.announcement,
  };
}
