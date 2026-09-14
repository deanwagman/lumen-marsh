import { Link, Navigate, useLocation, useParams } from 'react-router-dom';
import { useEffect } from 'react';

import { useAuth } from '@/auth/AuthContext';
import { getArticle, getSectionHref } from '@/features/docs/content/registry';

import styles from './DocsPages.module.css';

export default function DocArticlePage() {
  const { slug = '' } = useParams();
  const location = useLocation();
  const { session } = useAuth();
  const article = getArticle(slug, { role: session?.role });

  useEffect(() => {
    if (!article) {
      return;
    }

    const targetId = location.hash.replace('#', '');
    if (!targetId) {
      return;
    }

    document.getElementById(targetId)?.scrollIntoView({ block: 'start' });
  }, [article, location.hash]);

  if (!article) {
    return <Navigate to="/not-found" replace />;
  }

  return (
    <section className={styles.page}>
      <Link className={styles.back} to="/docs">
        ← Operator guides
      </Link>
      <header className={styles.header}>
        <p className={styles.kicker}>Documentation</p>
        <h1>{article.title}</h1>
        <p className={styles.muted}>{article.summary}</p>
      </header>
      <div className={styles.layout}>
        <nav className={styles.toc} aria-label="On this page">
          <p className={styles.tocTitle}>On this page</p>
          <ul className={styles.tocList}>
            {article.sections.map((section) => (
              <li key={section.id}>
                <a className={styles.tocLink} href={getSectionHref(article.slug, section.id)}>
                  {section.title}
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <article className={styles.article}>
          {article.sections.map((section) => (
            <section key={section.id} id={section.id} className={styles.section}>
              <h2 className={styles.sectionTitle}>{section.title}</h2>
              <div className={styles.sectionBody}>
                {section.body.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
              </div>
            </section>
          ))}
        </article>
      </div>
    </section>
  );
}
