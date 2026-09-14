import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { defaultAuthenticatedRoute } from '@/app/consoleRoutes';
import { Button } from '@/shared/ui/Button';

import { useAuth } from './AuthContext';
import { readAccessTokenClaims } from './jwtClaims';

export function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === 'loading') {
    return <p role="status">Loading operator session</p>;
  }
  if (auth.status === 'expired') {
    return <Navigate to="/session-expired" replace />;
  }
  if (auth.accessDenied) {
    return <Navigate to="/access-denied" replace />;
  }
  if (auth.status !== 'authenticated') {
    return (
      <Navigate
        to="/login"
        replace
        state={{ returnUrl: `${location.pathname}${location.search}${location.hash}` }}
      />
    );
  }
  return <Outlet />;
}

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const returnUrl =
    (location.state as { returnUrl?: string } | null)?.returnUrl ?? defaultAuthenticatedRoute;

  if (auth.status === 'authenticated') {
    return <Navigate to={returnUrl} replace />;
  }

  return (
    <main>
      <h1>VenueOps sign in</h1>
      <p>Sign in with your operator account to open the console.</p>
      <Button onClick={() => void auth.login(returnUrl)}>Sign in</Button>
    </main>
  );
}

export function OAuthCallbackPage() {
  const auth = useAuth();
  const { completeLogin, login } = auth;
  const navigate = useNavigate();
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    void completeLogin()
      .then((returnUrl) => {
        if (!active) return;
        if (import.meta.env.DEV) {
          navigate('/auth/session', { replace: true, state: { returnUrl } });
          return;
        }
        navigate(returnUrl, { replace: true });
      })
      .catch((caught: unknown) => {
        if (!active) return;
        const message = caught instanceof Error ? caught.message : 'Authentication failed.';
        setError(message.includes('access_denied') ? 'Access was denied.' : message);
      });
    return () => {
      active = false;
    };
  }, [completeLogin, navigate]);

  return (
    <main>
      <h1>{error ? 'Unable to sign in' : 'Completing sign in'}</h1>
      {error ? (
        <>
          <p role="alert">{error}</p>
          <Button onClick={() => void login()}>Try again</Button>
        </>
      ) : (
        <p role="status">Verifying the authorization response…</p>
      )}
    </main>
  );
}

export function AuthSessionPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const returnUrl =
    (location.state as { returnUrl?: string } | null)?.returnUrl ?? defaultAuthenticatedRoute;
  const claims = auth.session?.accessToken
    ? readAccessTokenClaims(auth.session.accessToken)
    : null;

  if (auth.status === 'loading') {
    return <p role="status">Loading operator session</p>;
  }
  if (auth.status !== 'authenticated' || !auth.session) {
    return <Navigate to="/login" replace />;
  }

  return (
    <main>
      <h1>Sign-in succeeded</h1>
      <p>
        {auth.session.displayName} is signed in as <strong>{auth.session.role}</strong>.
      </p>
      {claims ? (
        <dl>
          <dt>Issuer</dt>
          <dd>{claims.issuer ?? '—'}</dd>
          <dt>Audience</dt>
          <dd>{formatAudience(claims.audience)}</dd>
          <dt>Client ID</dt>
          <dd>{claims.clientId ?? '—'}</dd>
          <dt>Subject</dt>
          <dd>{claims.subject ?? auth.session.operatorId}</dd>
          <dt>Token use</dt>
          <dd>{claims.tokenUse ?? '—'}</dd>
          <dt>Groups</dt>
          <dd>{claims.groups.length > 0 ? claims.groups.join(', ') : '—'}</dd>
          <dt>Scopes</dt>
          <dd>{claims.scope ?? '—'}</dd>
          <dt>Expires</dt>
          <dd>{formatExpiry(claims.expiresAt)}</dd>
        </dl>
      ) : (
        <p>This session is not a JWT, so token claims are not shown.</p>
      )}
      <p>The access token is held in memory and is not displayed.</p>
      <Button onClick={() => navigate(returnUrl, { replace: true })}>Continue</Button>
    </main>
  );
}

function formatAudience(audience: string | string[] | undefined): string {
  if (!audience) {
    return '—';
  }
  return Array.isArray(audience) ? audience.join(', ') : audience;
}

function formatExpiry(expiresAt: number | undefined): string {
  if (!expiresAt) {
    return '—';
  }
  return new Date(expiresAt * 1000).toISOString();
}

export function SessionExpiredPage() {
  const auth = useAuth();
  return (
    <main>
      <h1>Session expired</h1>
      <p>Your operator session ended. Sign in again to continue.</p>
      <Button onClick={() => void auth.login()}>Sign in again</Button>
    </main>
  );
}

export function AccessDeniedPage() {
  const auth = useAuth();
  return (
    <main>
      <h1>Access denied</h1>
      <p>Your account does not have permission to perform that action.</p>
      <Button
        onClick={() => {
          auth.clearAccessDenied();
          window.history.back();
        }}
      >
        Go back
      </Button>
      <Button variant="ghost" onClick={() => void auth.logout()}>
        Sign out
      </Button>
    </main>
  );
}
