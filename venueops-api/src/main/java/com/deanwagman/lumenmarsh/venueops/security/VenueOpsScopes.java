package com.deanwagman.lumenmarsh.venueops.security;

public final class VenueOpsScopes {
    public static final String OPERATOR_READ = "venueops/operator.read";
    public static final String ATTRACTIONS_COMMAND = "venueops/attractions.command";
    public static final String INCIDENTS_COMMAND = "venueops/incidents.command";
    public static final String ADVISORIES_PUBLISH = "venueops/advisories.publish";
    public static final String WEATHER_REVIEW = "venueops/weather-recommendations.review";
    public static final String WEATHER_WRITE = "venueops/weather-recommendations.write";
    public static final String MAINTENANCE_READ = "venueops/maintenance.read";
    public static final String MAINTENANCE_COMMAND = "venueops/maintenance.command";
    public static final String MAINTENANCE_INSPECT = "venueops/maintenance.inspect";
    public static final String RELIABILITY_WRITE = "venueops/reliability.write";

    public static final String SCOPE_OPERATOR_READ = "SCOPE_" + OPERATOR_READ;
    public static final String SCOPE_ATTRACTIONS_COMMAND = "SCOPE_" + ATTRACTIONS_COMMAND;
    public static final String SCOPE_INCIDENTS_COMMAND = "SCOPE_" + INCIDENTS_COMMAND;
    public static final String SCOPE_ADVISORIES_PUBLISH = "SCOPE_" + ADVISORIES_PUBLISH;
    public static final String SCOPE_WEATHER_REVIEW = "SCOPE_" + WEATHER_REVIEW;
    public static final String SCOPE_WEATHER_WRITE = "SCOPE_" + WEATHER_WRITE;
    public static final String SCOPE_MAINTENANCE_READ = "SCOPE_" + MAINTENANCE_READ;
    public static final String SCOPE_MAINTENANCE_COMMAND = "SCOPE_" + MAINTENANCE_COMMAND;
    public static final String SCOPE_MAINTENANCE_INSPECT = "SCOPE_" + MAINTENANCE_INSPECT;
    public static final String SCOPE_RELIABILITY_WRITE = "SCOPE_" + RELIABILITY_WRITE;

    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";
    public static final String ROLE_SUPERVISOR = "ROLE_SUPERVISOR";

    private VenueOpsScopes() {
    }
}
