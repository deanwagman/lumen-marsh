import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { UserManager } from 'oidc-client-ts';

import { defaultAuthenticatedRoute } from '@/app/consoleRoutes';
import { environment } from '@/config/environment';

import { readAccessTokenClaims } from './jwtClaims';
import { getOidcUserManager } from './oidc';
import {
  localDevelopmentSession,
  resolveOperatorRole,
  type AuthSession,
} from './session';

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'expired';

type AuthContextValue = {
  session: AuthSession | null;
  status: AuthStatus;
  accessDenied: boolean;
  login: (returnUrl?: string) => Promise<void>;
  completeLogin: () => Promise<string>;
  logout: () => Promise<void>;
  expireSession: () => void;
  reportAccessDenied: () => void;
  clearAccessDenied: () => void;
  getAccessToken: () => string | undefined;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({
  children,
  initialSession,
  manager,
}: {
  children: ReactNode;
  initialSession?: AuthSession | null;
  manager?: UserManager;
}) {
  const configuredSession =
    initialSession !== undefined
      ? initialSession
      : environment.authMode === 'local'
        ? localDevelopmentSession
        : null;
  const [session, setSession] = useState<AuthSession | null>(configuredSession);
  const [status, setStatus] = useState<AuthStatus>(
    configuredSession
      ? 'authenticated'
      : environment.authMode === 'oidc'
        ? 'loading'
        : 'unauthenticated',
  );
  const [accessDenied, setAccessDenied] = useState(false);

  const resolveManager = useCallback(
    () => manager ?? getOidcUserManager(),
    [manager],
  );

  useEffect(() => {
    if (
      initialSession !== undefined ||
      environment.authMode === 'local' ||
      window.location.pathname === '/auth/callback'
    ) {
      return;
    }

    let active = true;
    void resolveManager()
      .getUser()
      .then((user) => {
        if (!active) return;
        if (!user || user.expired) {
          setStatus('unauthenticated');
          return;
        }
        setSession(sessionFromUser(user));
        setStatus('authenticated');
      })
      .catch(() => {
        if (active) setStatus('unauthenticated');
      });
    return () => {
      active = false;
    };
  }, [initialSession, resolveManager]);

  useEffect(() => {
    if (!session?.expiresAt) return;
    const delay = Math.max(0, session.expiresAt * 1000 - Date.now());
    const timer = window.setTimeout(() => {
      setSession(null);
      setStatus('expired');
    }, delay);
    return () => window.clearTimeout(timer);
  }, [session]);

  const login = useCallback(
    async (returnUrl = defaultAuthenticatedRoute) => {
      if (environment.authMode === 'local') {
        setSession(localDevelopmentSession);
        setStatus('authenticated');
        return;
      }
      await resolveManager().signinRedirect({ state: { returnUrl } });
    },
    [resolveManager],
  );

  const completeLogin = useCallback(async () => {
    const user = await resolveManager().signinRedirectCallback();
    const nextSession = sessionFromUser(user);
    setSession(nextSession);
    setStatus('authenticated');
    const state = user.state as { returnUrl?: unknown } | undefined;
    return typeof state?.returnUrl === 'string' &&
      state.returnUrl.startsWith('/')
      ? state.returnUrl
      : defaultAuthenticatedRoute;
  }, [resolveManager]);

  const logout = useCallback(async () => {
    setSession(null);
    setStatus('unauthenticated');
    setAccessDenied(false);
    if (environment.authMode === 'local') return;
    await resolveManager().signoutRedirect();
  }, [resolveManager]);

  const expireSession = useCallback(() => {
    setSession(null);
    setStatus('expired');
  }, []);
  const reportAccessDenied = useCallback(() => setAccessDenied(true), []);
  const clearAccessDenied = useCallback(() => setAccessDenied(false), []);
  const getAccessToken = useCallback(
    () => session?.accessToken,
    [session?.accessToken],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      status,
      accessDenied,
      login,
      completeLogin,
      logout,
      expireSession,
      reportAccessDenied,
      clearAccessDenied,
      getAccessToken,
    }),
    [
      accessDenied,
      clearAccessDenied,
      completeLogin,
      expireSession,
      getAccessToken,
      login,
      logout,
      reportAccessDenied,
      session,
      status,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// The context hook intentionally lives beside its provider.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

function sessionFromUser(user: {
  access_token: string;
  id_token?: string;
  expires_at?: number;
  profile: Record<string, unknown>;
}): AuthSession {
  const profile = user.profile;
  const scopes =
    readAccessTokenClaims(user.access_token)
      ?.scope?.split(/\s+/)
      .filter(Boolean) ?? [];
  return {
    accessToken: user.access_token,
    idToken: user.id_token,
    expiresAt: user.expires_at,
    operatorId: firstString(profile, ['sub']) ?? 'operator',
    displayName:
      firstString(profile, ['name', 'preferred_username', 'email']) ??
      'VenueOps Operator',
    role: resolveOperatorRole(profile),
    scopes,
  };
}

function firstString(
  profile: Record<string, unknown>,
  keys: string[],
): string | undefined {
  for (const key of keys) {
    if (typeof profile[key] === 'string' && profile[key]) return profile[key];
  }
  return undefined;
}
