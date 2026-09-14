package com.deanwagman.lumenmarsh.venueops.incident.application;

public interface GuestAdvisoryUpdatePublisher {
    void publish(GuestAdvisoryOperationalUpdate update);
}
