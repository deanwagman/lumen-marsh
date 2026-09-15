import { describe, expect, it } from 'vitest';

import {
  getArticle,
  getSectionHref,
  listArticles,
} from '@/features/docs/content/registry';

describe('docs registry', () => {
  it('lists starter articles in order', () => {
    expect(listArticles().map((article) => article.slug)).toEqual([
      'getting-started',
      'dashboard',
      'attractions',
      'incidents',
      'weather',
      'maintenance',
    ]);
  });

  it('looks up an article by slug', () => {
    const article = getArticle('attractions');
    expect(article?.title).toBe('Attractions');
    expect(article?.sections.some((section) => section.id === 'commands')).toBe(
      true,
    );
  });

  it('returns undefined for an unknown slug', () => {
    expect(getArticle('missing-guide')).toBeUndefined();
  });

  it('builds stable section hrefs', () => {
    expect(getSectionHref('incidents', 'lifecycle')).toBe(
      '/docs/incidents#lifecycle',
    );
  });

  it('filters supervisor-only sections for operators', () => {
    const forOperator = getArticle('incidents', { role: 'operator' });
    const forSupervisor = getArticle('incidents', { role: 'supervisor' });

    expect(forOperator?.sections.map((section) => section.id)).toEqual([
      'lifecycle',
      'severity',
      'advisories-operators',
    ]);
    expect(forSupervisor?.sections.map((section) => section.id)).toEqual([
      'lifecycle',
      'severity',
      'advisories',
    ]);
  });

  it('includes all articles for both roles in the starter set', () => {
    expect(listArticles({ role: 'operator' })).toHaveLength(6);
    expect(listArticles({ role: 'supervisor' })).toHaveLength(6);
  });
});
