import { Link, useParams } from 'react-router-dom';

import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { formatLabel } from '@/features/attractions/domain/formatters';
import { WorkOrderTable } from '@/features/maintenance/components/WorkOrderTable';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import { formatMaintenanceTime, serviceStatusTone } from '@/features/maintenance/domain/maintenancePresentation';
import { useMaintenanceAsset, useMaintenanceAssetWorkOrders } from '@/features/maintenance/hooks/useMaintenanceAsset';
import { useAuth } from '@/auth/AuthContext';
import { NotFoundError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './MaintenanceAssetPage.module.css';

export default function MaintenanceAssetPage() {
  const { assetId } = useParams();
  const { session } = useAuth();
  const asset = useMaintenanceAsset(assetId);
  const workOrders = useMaintenanceAssetWorkOrders(assetId);
  const attractions = useAttractions();
  const names = new Map((attractions.data ?? []).map((item) => [item.id, item.name]));

  if (!canReadMaintenance(session)) {
    return (
      <section className={styles.page}>
        <h1>Asset</h1>
        <p>This workspace requires the venueops/maintenance.read scope.</p>
      </section>
    );
  }

  if (asset.isPending) {
    return (
      <section className={styles.page}>
        <p className={styles.muted} role="status">
          Loading asset
        </p>
      </section>
    );
  }

  if (asset.isError || !asset.data) {
    return (
      <section className={styles.page}>
        <h1>{asset.error instanceof NotFoundError ? 'Asset not found' : 'Unable to load asset'}</h1>
        <p className={styles.muted}>{asset.error?.message}</p>
        {asset.error instanceof NotFoundError ? (
          <Link to="/maintenance">Return to maintenance</Link>
        ) : (
          <Button onClick={() => void asset.refetch()}>Retry</Button>
        )}
      </section>
    );
  }

  const record = asset.data;
  const crumbs = [names.get(record.attractionId) ?? record.attractionId, record.assetCode];

  return (
    <section className={styles.page}>
      <Link className={styles.back} to="/maintenance">
        ← Maintenance
      </Link>
      <p className={styles.crumbs}>{crumbs.join(' / ')}</p>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Asset</p>
          <h1 className={styles.mono}>{record.assetCode}</h1>
          <p>{record.name}</p>
        </div>
        <StatusBadge tone={serviceStatusTone(record.serviceStatus)} label={formatLabel(record.serviceStatus)} />
      </header>
      <dl className={styles.facts}>
        <div>
          <dt>Type</dt>
          <dd>{formatLabel(record.assetType)}</dd>
        </div>
        <div>
          <dt>Criticality</dt>
          <dd>{formatLabel(record.criticality)}</dd>
        </div>
        <div>
          <dt>Manufacturer</dt>
          <dd>{record.manufacturer ?? '—'}</dd>
        </div>
        <div>
          <dt>Model</dt>
          <dd>{record.model ?? '—'}</dd>
        </div>
        <div>
          <dt>Installed</dt>
          <dd>{formatMaintenanceTime(record.installedAt)}</dd>
        </div>
        <div>
          <dt>Version</dt>
          <dd>v{record.version}</dd>
        </div>
      </dl>
      <section>
        <h2>Work orders</h2>
        {workOrders.isPending ? (
          <p className={styles.muted} role="status">
            Loading work orders
          </p>
        ) : (
          <WorkOrderTable workOrders={workOrders.data?.items ?? []} attractionNames={names} />
        )}
      </section>
    </section>
  );
}
