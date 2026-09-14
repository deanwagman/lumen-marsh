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
        'Review requires the weather-recommendations.review scope. Without it, you can still read the inbox but cannot acknowledge or dismiss.',
      ],
    },
    {
      id: 'ack-dismiss',
      title: 'Acknowledge and dismiss',
      body: [
        'Acknowledge when you have taken ownership of an active recommendation and are working the guidance.',
        'Dismiss when the recommendation does not require further action. Dismiss records an operator reason for the trail.',
        'Neither action places attractions on hold by itself—use attraction commands or an incident when operational state must change.',
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
