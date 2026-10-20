package com.deanwagman.lumenmarsh.venueops.demo;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

final class DemoGatewayService {

    record Component(String name, String state, boolean core, String scenario) {
    }

    record Readiness(boolean ready, List<Component> components) {
    }

    private record Target(String baseUrl, String path) {
    }

    private final DemoSandboxProperties properties;
    private final RestClient rest;

    DemoGatewayService(DemoSandboxProperties properties, RestClient rest) {
        this.properties = properties;
        this.rest = rest;
    }

    Readiness readiness() {
        List<Component> components = List.of(
                new Component("VenueOps", "READY", true, null),
                probe("Guest", properties.guestUrl(), "/health", true, null),
                probe("Control Tower", properties.towerUrl(), "/health", true, null),
                probe("Weather", properties.environmentalUrl(), "/health/ready", false, "weather"),
                probe("Flow", properties.flowUrl(), "/health/ready", false, "flow"),
                probe("Reliability", properties.reliabilityUrl(), "/health/ready", false, "reliability")
        );
        boolean ready = components.stream().filter(Component::core)
                .allMatch(component -> "READY".equals(component.state()));
        return new Readiness(ready, components);
    }

    Object scenario(String name) {
        Target target = switch (name) {
            case "lightning-hold" -> new Target(
                    properties.environmentalUrl(), "/api/v1/simulation/scenarios/hold-conditions");
            case "clear-storm" -> new Target(
                    properties.environmentalUrl(), "/api/v1/simulation/scenarios/clearance-period");
            case "cypress-vibration" -> new Target(
                    properties.reliabilityUrl(), "/simulation/scenarios/cypress-coil-vibration");
            case "mangrove-disruption" -> new Target(
                    properties.flowUrl(), "/simulation/scenarios/mangrove-disruption");
            default -> throw new UnknownScenarioException();
        };
        return rest.post()
                .uri(target.baseUrl() + target.path())
                .body(Map.of())
                .retrieve()
                .body(Object.class);
    }

    private Component probe(String name, String baseUrl, String path, boolean core, String scenario) {
        try {
            HttpStatusCode status = rest.get().uri(baseUrl + path).retrieve().toBodilessEntity().getStatusCode();
            return new Component(name, status.is2xxSuccessful() ? "READY" : "UNAVAILABLE", core, scenario);
        } catch (RuntimeException unavailable) {
            return new Component(name, "UNAVAILABLE", core, scenario);
        }
    }

    static final class UnknownScenarioException extends RuntimeException {
    }
}
