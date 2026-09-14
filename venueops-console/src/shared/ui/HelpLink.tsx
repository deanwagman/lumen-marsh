import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';

import { getSectionHref } from '@/features/docs/content/registry';

import styles from './HelpLink.module.css';

export function HelpLink({
  article,
  section,
  label,
  className,
}: {
  article: string;
  section: string;
  label: string;
  className?: string;
}) {
  const href = getSectionHref(article, section);
  const classes = [styles.link, className].filter(Boolean).join(' ');

  return (
    <Link className={classes} to={href} aria-label={label} title={label}>
      ?
    </Link>
  );
}

export function HelpHeading({
  as: Tag = 'h2',
  id,
  children,
  article,
  section,
  label,
}: {
  as?: 'h2' | 'h3';
  id?: string;
  children: ReactNode;
  article: string;
  section: string;
  label: string;
}) {
  return (
    <div className={styles.headingRow}>
      <Tag id={id}>{children}</Tag>
      <HelpLink article={article} section={section} label={label} />
    </div>
  );
}
