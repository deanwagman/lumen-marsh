import { createBrowserRouter, Navigate, type RouteObject } from 'react-router-dom';

import { defaultAuthenticatedRoute } from '@/app/consoleRoutes';
import {
  AccessDeniedPage,
  AuthSessionPage,
  LoginPage,
  OAuthCallbackPage,
  ProtectedRoute,
  SessionExpiredPage,
} from '@/auth/AuthPages';
import AttractionDetailPage from '@/features/attractions/pages/AttractionDetailPage';
import AttractionsPage from '@/features/attractions/pages/AttractionsPage';
import DashboardPage from '@/features/dashboard/pages/DashboardPage';
import DocArticlePage from '@/features/docs/pages/DocArticlePage';
import DocsIndexPage from '@/features/docs/pages/DocsIndexPage';
import IncidentDetailPage from '@/features/incidents/pages/IncidentDetailPage';
import IncidentsPage from '@/features/incidents/pages/IncidentsPage';
import MaintenanceAssetPage from '@/features/maintenance/pages/MaintenanceAssetPage';
import MaintenancePage from '@/features/maintenance/pages/MaintenancePage';
import MaintenanceWorkOrderPage from '@/features/maintenance/pages/MaintenanceWorkOrderPage';
import { AppShell } from '@/shared/layout/AppShell';

import NotFoundPage from './NotFoundPage';

export const appRoutes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/auth/callback', element: <OAuthCallbackPage /> },
  { path: '/auth/session', element: <AuthSessionPage /> },
  { path: '/session-expired', element: <SessionExpiredPage /> },
  { path: '/access-denied', element: <AccessDeniedPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to={defaultAuthenticatedRoute} replace /> },
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'attractions', element: <AttractionsPage /> },
          { path: 'attractions/:id', element: <AttractionDetailPage /> },
          { path: 'incidents', element: <IncidentsPage /> },
          { path: 'incidents/:incidentId', element: <IncidentDetailPage /> },
          { path: 'maintenance', element: <MaintenancePage /> },
          { path: 'maintenance/work-orders/:workOrderId', element: <MaintenanceWorkOrderPage /> },
          { path: 'maintenance/assets/:assetId', element: <MaintenanceAssetPage /> },
          { path: 'docs', element: <DocsIndexPage /> },
          { path: 'docs/:slug', element: <DocArticlePage /> },
          { path: 'not-found', element: <NotFoundPage /> },
          { path: '*', element: <Navigate to="/not-found" replace /> },
        ],
      },
    ],
  },
];

export const router = createBrowserRouter(appRoutes);
