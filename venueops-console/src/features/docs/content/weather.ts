import type { DocArticle } from '@/features/docs/domain/types';

export const weatherArticle: DocArticle = {
  slug: 'weather',
  title: 'Weather recommendations',
  summary: 'Review inbox items, acknowledge or dismiss, and create linked incidents.',
  category: 'weather',
  sections: [
    {
      id: 'review',
      title: 'Reviewing recommendations',
      body: [
        'Weather recommendations appear on the Attractions overview. Each card shows recommended action, evidence, data age, and affected attractions.',
        'Active items need attention. Cleared or already-handled items remain for context.',
        'The inbox list is GET /api/v1/operator/weather/recommendations and requires venueops/weather-recommendations.review. Without that scope the list fails, so the Attractions inbox does not load. The dashboard can still show pending weather cards, because GET /api/v1/operator/dashboard requires only venueops/operator.read.',
      ],
    },
    {
      id: 'ack-dismiss',
      title: 'Acknowledge and dismiss',
      body: [
        'Acknowledge and Dismiss are on the inbox for a session with venueops/weather-recommendations.review. The same scope is what the API requires for those commands.',
        'Acknowledge when you have taken ownership of an active recommendation and are working the guidance. The button does not ask for a reason.',
        'Dismiss when the recommendation does not require further action. The button does not ask for a reason. It files the fixed reason Dismissed from Control Tower.',
        'Neither action places attractions on hold by itself. Use attraction commands or an incident when operational state must change.',
      ],
    },
    {
      id: 'create-incident',
      title: 'Create incident from weather',
      body: [
        'When review and incident command scopes are present, you can create a weather-typed incident from a recommendation.',
        'Review the prefilled title, severity, description, and affected attractions before submitting.',
        'Creating an incident links the recommendation; it does not automatically hold attractions.',
      ],
    },
  ],
};
