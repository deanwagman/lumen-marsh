import { Link } from 'react-router-dom';

import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import { statusTone } from '@/features/attractions/domain/attraction';
import type { LiveAttractionFlash } from '@/features/attractions/hooks/useLiveAttractionChanges';
import {
  formatCapacity,
  formatStatus,
  formatUpdatedAt,
  formatVersion,
  formatWaitTime,
} from '@/features/attractions/domain/formatters';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './AttractionTable.module.css';

export function AttractionTable({
  attractions,
  flashes,
}: {
  attractions: AttractionSummary[];
  flashes?: LiveAttractionFlash[];
}) {
  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">Attraction</th>
            <th scope="col">Status</th>
            <th scope="col">Capacity</th>
            <th scope="col">Wait</th>
            <th scope="col">Last updated</th>
            <th scope="col">Version</th>
          </tr>
        </thead>
        <tbody>
          {attractions.map((attraction) => {
            const flash = flashes?.find(
              (item) => item.id === attraction.id && item.version === attraction.version,
            );
            const flashKey = flash ? `${attraction.id}-${attraction.version}` : attraction.id;

            return (
              <tr
                key={attraction.id}
                className={flash ? `${styles.row} ${styles.flashRow}` : styles.row}
              >
                <th scope="row">
                  <Link className={styles.nameLink} to={`/attractions/${attraction.id}`}>
                    {attraction.name}
                    <span className={styles.area}>{attraction.area}</span>
                  </Link>
                </th>
                <td>
                  <span key={`${flashKey}-status`} className={flash ? styles.flashCell : undefined}>
                    <StatusBadge
                      tone={statusTone(attraction.status)}
                      label={formatStatus(attraction.status)}
                    />
                  </span>
                </td>
                <td>{formatCapacity(attraction.capacityMode)}</td>
                <td>
                  <span key={`${flashKey}-wait`} className={flash ? styles.flashCell : undefined}>
                    {formatWaitTime(attraction.waitMinutes)}
                  </span>
                </td>
                <td>{flash ? 'Updated just now' : formatUpdatedAt(attraction.updatedAt)}</td>
                <td className={styles.mono}>{formatVersion(attraction.version)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
