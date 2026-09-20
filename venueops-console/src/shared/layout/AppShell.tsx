import { NavLink, Outlet } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { CommandReceiptBanner } from '@/features/attractions/components/CommandReceiptBanner';
import { useIncidents } from '@/features/incidents/hooks/useIncidents';
import { isOpenIncident } from '@/features/incidents/domain/incident';
import { useFlowNavBadge } from '@/features/flow/hooks/useFlowNavBadge';
import { useMaintenanceNavBadge } from '@/features/maintenance/hooks/useMaintenanceNavBadge';
import { Button } from '@/shared/ui/Button';

import { ConnectionIndicator } from './ConnectionIndicator';
import styles from './AppShell.module.css';

export function AppShell() {
  const auth = useAuth();
  const operator = auth.session!;
  const incidents = useIncidents();
  const maintenance = useMaintenanceNavBadge();
  const flow = useFlowNavBadge();
  const openCount = (incidents.data ?? []).filter(isOpenIncident).length;
  const initials = operator.displayName
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className={styles.shell}>
      <a className={styles.skipLink} href="#workspace">
        Skip to workspace
      </a>
      <aside className={styles.rail}>
        <NavLink className={styles.brand} to="/dashboard">
          <svg className={styles.mark} viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="8" fill="#07110F" />
            <path
              d="M8 22c4-8 6-14 8-14s4 6 8 14"
              stroke="#C4784A"
              strokeWidth="2"
              strokeLinecap="round"
              fill="none"
            />
            <circle cx="16" cy="20" r="2" fill="#6F9A2E" />
          </svg>
          <span className={styles.brandText}>
            <span className={styles.product}>
              Lumen Marsh <span className={styles.productSuffix}>Control</span>
            </span>
            <span className={styles.context}>Operator console</span>
          </span>
        </NavLink>
        <nav className={styles.nav} aria-label="Console">
          <NavLink
            className={({ isActive }) =>
              isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
            }
            to="/dashboard"
          >
            Dashboard
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
            }
            to="/attractions"
          >
            Attractions
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
            }
            to="/incidents"
          >
            Incidents
            {openCount > 0 ? (
              <span className={styles.badge} aria-label={`${openCount} open incidents`}>
                {openCount}
              </span>
            ) : null}
          </NavLink>
          {maintenance.visible ? (
            <NavLink
              className={({ isActive }) =>
                isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
              }
              to="/maintenance"
            >
              Maintenance
              {maintenance.count > 0 ? (
                <span
                  className={styles.badge}
                  aria-label={`${maintenance.count} pending maintenance items`}
                >
                  {maintenance.count}
                </span>
              ) : null}
            </NavLink>
          ) : null}
          {flow.visible ? (
            <NavLink
              className={({ isActive }) =>
                isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
              }
              to="/park-flow"
            >
              Park Flow
              {flow.count > 0 ? (
                <span className={styles.badge} aria-label={`${flow.count} pending flow recommendations`}>
                  {flow.count}
                </span>
              ) : null}
            </NavLink>
          ) : null}
          <NavLink
            className={({ isActive }) =>
              isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
            }
            to="/docs"
          >
            Docs
          </NavLink>
        </nav>
        <div className={styles.railMeta}>
          <p className={styles.narrowNotice}>
            Designed for operator workstations. This layout is compacted for a narrower
            display.
          </p>
        </div>
      </aside>
      <div className={styles.main}>
        <header className={styles.topBar}>
          <ConnectionIndicator />
          <div className={styles.operator}>
            <span className={styles.operatorMark} aria-hidden="true">
              {initials}
            </span>
            <span>
              <span className={styles.operatorName}>{operator.displayName}</span>
              <span className={styles.operatorId}>
                {' '}
                · {operator.role} · {operator.operatorId}
              </span>
            </span>
            <Button variant="ghost" onClick={() => void auth.logout()}>
              Sign out
            </Button>
          </div>
        </header>
        <CommandReceiptBanner />
        <main className={styles.workspace} id="workspace" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
