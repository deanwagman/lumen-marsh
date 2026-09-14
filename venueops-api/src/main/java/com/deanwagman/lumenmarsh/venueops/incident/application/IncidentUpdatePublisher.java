package com.deanwagman.lumenmarsh.venueops.incident.application;

public interface IncidentUpdatePublisher {
    void publish(IncidentOperationalUpdate update);
}
