package com.deanwagman.lumenmarsh.venueops.maintenance.application;

public interface MaintenanceUpdatePublisher {

    void publish(MaintenanceOperationalUpdate update);

    void publish(MaintenanceRecommendationOperationalUpdate update);
}
