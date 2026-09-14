import type { OperatorRole } from '@/auth/session';
import { attractionsArticle } from '@/features/docs/content/attractions';
import { dashboardArticle } from '@/features/docs/content/dashboard';
import { gettingStartedArticle } from '@/features/docs/content/getting-started';
import { incidentsArticle } from '@/features/docs/content/incidents';
import { weatherArticle } from '@/features/docs/content/weather';
import type { DocArticle, DocSection } from '@/features/docs/domain/types';

const articles: DocArticle[] = [
  gettingStartedArticle,
  dashboardArticle,
  attractionsArticle,
  incidentsArticle,
  weatherArticle,
];

function isVisibleToRole(
  roles: OperatorRole[] | undefined,
  role: OperatorRole,
): boolean {
  return roles === undefined || roles.includes(role);
}

export function listArticles(options?: { role?: OperatorRole }): DocArticle[] {
  const role = options?.role;
  if (!role) {
    return articles.map(filterArticleSections);
  }

  return articles
    .filter((article) => isVisibleToRole(article.roles, role))
    .map((article) => filterArticleForRole(article, role));
}

export function getArticle(
  slug: string,
  options?: { role?: OperatorRole },
): DocArticle | undefined {
  const article = articles.find((entry) => entry.slug === slug);
  if (!article) {
    return undefined;
  }

  const role = options?.role;
  if (!role) {
    return filterArticleSections(article);
  }

  if (!isVisibleToRole(article.roles, role)) {
    return undefined;
  }

  return filterArticleForRole(article, role);
}

export function getSectionHref(slug: string, sectionId: string): string {
  return `/docs/${slug}#${sectionId}`;
}

function filterArticleForRole(article: DocArticle, role: OperatorRole): DocArticle {
  return {
    ...article,
    sections: article.sections.filter((section) =>
      isVisibleToRole(section.roles, role),
    ),
  };
}

function filterArticleSections(article: DocArticle): DocArticle {
  return {
    ...article,
    sections: [...article.sections],
  };
}

export function sectionIds(article: DocArticle): string[] {
  return article.sections.map((section: DocSection) => section.id);
}
