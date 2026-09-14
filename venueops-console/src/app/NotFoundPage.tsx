import { Link } from 'react-router-dom';

import styles from './NotFoundPage.module.css';

export default function NotFoundPage() {
  return (
    <section className={styles.page}>
      <h1>Page not found</h1>
      <p className={styles.muted}>
        That route is not part of Lumen Marsh Control. Return to the attraction overview.
      </p>
      <Link className={styles.back} to="/attractions">
        Go to attractions
      </Link>
    </section>
  );
}
