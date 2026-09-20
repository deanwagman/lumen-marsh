import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { listArticles } from '@/features/docs/content/registry';

import styles from './DocsPages.module.css';

const categoryLabels: Record<string, string> = {
  overview: 'Overview',
  attractions: 'Attractions',
  incidents: 'Incidents',
  weather: 'Weather',
  maintenance: 'Maintenance',
  flow: 'Park Flow',
};

export default function DocsIndexPage() {
  const { session } = useAuth();
  const articles = listArticles({ role: session?.role });

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <p className={styles.kicker}>Documentation</p>
        <h1>Operator guides</h1>
        <p className={styles.muted}>
          Procedures for shift work in Lumen Marsh Control. Open a guide, or use the help
          links next to controls in the console.
        </p>
      </header>
      <ul className={styles.list}>
        {articles.map((article) => (
          <li key={article.slug}>
            <Link className={styles.card} to={`/docs/${article.slug}`}>
              <span className={styles.category}>
                {categoryLabels[article.category] ?? article.category}
              </span>
              <h2 className={styles.cardTitle}>{article.title}</h2>
              <p className={styles.muted}>{article.summary}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
