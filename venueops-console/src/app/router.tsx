import { lazy, Suspense, type ReactNode } from 'react';
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
import { AppShell } from '@/shared/layout/AppShell';

import NotFoundPage from './NotFoundPage';

const FlowPage = lazy(() => import('@/features/flow/pages/FlowPage'));
const FlowAttractionPage = lazy(() => import('@/features/flow/pages/FlowAttractionPage'));
const MaintenancePage = lazy(() => import('@/features/maintenance/pages/MaintenancePage'));
const MaintenanceWorkOrderPage = lazy(
  () => import('@/features/maintenance/pages/MaintenanceWorkOrderPage'),
);
const MaintenanceAssetPage = lazy(() => import('@/features/maintenance/pages/MaintenanceAssetPage'));

function Suspended({ children }: { children: ReactNode }) {
  return <Suspense fallback={<p role="status">Loading workspace</p>}>{children}</Suspense>;
}

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
          { path: 'maintenance', element: <Suspended><MaintenancePage /></Suspended> },
          { path: 'maintenance/work-orders/:workOrderId', element: <Suspended><MaintenanceWorkOrderPage /></Suspended> },
          { path: 'maintenance/assets/:assetId', element: <Suspended><MaintenanceAssetPage /></Suspended> },
          { path: 'park-flow', element: <Suspended><FlowPage /></Suspended> },
          { path: 'park-flow/:attractionId', element: <Suspended><FlowAttractionPage /></Suspended> },
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
