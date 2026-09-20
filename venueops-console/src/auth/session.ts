export type OperatorRole = 'supervisor' | 'operator';

export const venueOpsScopes = {
  operatorRead: 'venueops/operator.read',
  attractionsCommand: 'venueops/attractions.command',
  incidentsCommand: 'venueops/incidents.command',
  advisoriesPublish: 'venueops/advisories.publish',
  weatherRecommendationsReview: 'venueops/weather-recommendations.review',
  maintenanceRead: 'venueops/maintenance.read',
  maintenanceCommand: 'venueops/maintenance.command',
  maintenanceInspect: 'venueops/maintenance.inspect',
  flowRead: 'venueops/flow.read',
  flowCommand: 'venueops/flow.command',
  flowPublish: 'venueops/flow.publish',
} as const;

export type VenueOpsScope =
  (typeof venueOpsScopes)[keyof typeof venueOpsScopes];

export type AuthSession = {
  accessToken: string;
  idToken?: string;
  expiresAt?: number;
  operatorId: string;
  displayName: string;
  role: OperatorRole;
  scopes: string[];
};

export const localDevelopmentSession: AuthSession = {
  accessToken: 'local-development-token',
  operatorId: 'local-operator',
  displayName: 'Local Operator',
  role: 'supervisor',
  scopes: Object.values(venueOpsScopes),
};

export function hasVenueOpsScope(
  session: AuthSession | null | undefined,
  scope: VenueOpsScope,
): boolean {
  return session?.scopes.includes(scope) ?? false;
}

export function resolveOperatorRole(
  profile: Record<string, unknown>,
): OperatorRole {
  const groups = Array.isArray(profile['cognito:groups'])
    ? profile['cognito:groups'].filter(
        (value): value is string => typeof value === 'string',
      )
    : [];
  const declaredRole =
    typeof profile['custom:role'] === 'string'
      ? profile['custom:role']
      : typeof profile.role === 'string'
        ? profile.role
        : '';

  return [...groups, declaredRole].some(
    (value) => value.toLowerCase() === 'supervisor',
  )
    ? 'supervisor'
    : 'operator';
}
