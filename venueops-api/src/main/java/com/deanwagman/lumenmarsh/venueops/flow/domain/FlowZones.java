package com.deanwagman.lumenmarsh.venueops.flow.domain;

import java.util.Map;

public final class FlowZones {

    public static final String LUMINOUS_WETLANDS = "luminous-wetlands";
    public static final String RESEARCH_QUARTER = "research-quarter";
    public static final String CYPRESS_BASIN = "cypress-basin";

    private static final Map<String, String> ATTRACTION_ZONES = Map.of(
            "mangrove-run", LUMINOUS_WETLANDS,
            "stormglass-station", RESEARCH_QUARTER,
            "cypress-coil", CYPRESS_BASIN
    );

    private static final Map<String, String> ZONE_NAMES = Map.of(
            LUMINOUS_WETLANDS, "Luminous Wetlands",
            RESEARCH_QUARTER, "Research Quarter",
            CYPRESS_BASIN, "Cypress Basin"
    );

    private FlowZones() {
    }

    public static String zoneId(String attractionId) {
        return ATTRACTION_ZONES.getOrDefault(attractionId, "park");
    }

    public static String zoneName(String zoneId) {
        return ZONE_NAMES.getOrDefault(zoneId, "Park");
    }

    public static int walkingMinutes(String fromZone, String toZone) {
        if (fromZone == null || toZone == null || fromZone.equals(toZone)) {
            return 3;
        }
        if ((LUMINOUS_WETLANDS.equals(fromZone) && CYPRESS_BASIN.equals(toZone))
                || (CYPRESS_BASIN.equals(fromZone) && LUMINOUS_WETLANDS.equals(toZone))) {
            return 8;
        }
        if ((LUMINOUS_WETLANDS.equals(fromZone) && RESEARCH_QUARTER.equals(toZone))
                || (RESEARCH_QUARTER.equals(fromZone) && LUMINOUS_WETLANDS.equals(toZone))) {
            return 10;
        }
        return 12;
    }
}
