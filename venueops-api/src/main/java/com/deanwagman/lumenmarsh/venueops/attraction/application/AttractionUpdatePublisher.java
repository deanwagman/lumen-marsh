package com.deanwagman.lumenmarsh.venueops.attraction.application;

public interface AttractionUpdatePublisher {
    void publish(AttractionOperationalUpdate update);
}
