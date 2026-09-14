import { RouterProvider } from 'react-router-dom';

import { AttractionLiveUpdates } from '@/features/attractions/components/AttractionLiveUpdates';

import { AppProviders } from './providers';
import { router } from './router';

export function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
      <AttractionLiveUpdates />
    </AppProviders>
  );
}
