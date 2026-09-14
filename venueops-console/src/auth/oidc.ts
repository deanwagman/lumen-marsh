import { UserManager, WebStorageStateStore, type UserManagerSettings } from 'oidc-client-ts';

import { environment } from '@/config/environment';

let userManager: UserManager | undefined;

export function getOidcUserManager(): UserManager {
  if (!userManager) {
    const settings: UserManagerSettings = {
      authority: environment.oidc.authority,
      client_id: environment.oidc.clientId,
      redirect_uri: environment.oidc.redirectUri,
      post_logout_redirect_uri: environment.oidc.logoutUri,
      response_type: 'code',
      scope: environment.oidc.scopes,
      disablePKCE: false,
      // Session storage survives a reload in this tab and is cleared when the tab closes.
      // The access token is a bearer JWT, not a cookie.
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
      stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
      automaticSilentRenew: false,
      monitorSession: false,
    };
    userManager = new UserManager(settings);
  }
  return userManager;
}
