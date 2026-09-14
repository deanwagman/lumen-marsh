import type { OperatorRole } from '@/auth/session';

export type DocCategory =
  | 'overview'
  | 'attractions'
  | 'incidents'
  | 'weather';

export type DocSection = {
  id: string;
  title: string;
  /** Paragraphs of procedural guidance. */
  body: string[];
  /** When set, only these roles see the section. */
  roles?: OperatorRole[];
};

export type DocArticle = {
  slug: string;
  title: string;
  summary: string;
  category: DocCategory;
  /** When set, only these roles see the article. */
  roles?: OperatorRole[];
  sections: DocSection[];
};
