package com.deanwagman.lumenmarsh.venueops.flow.application;

public interface FlowUpdatePublisher {

    void publish(FlowOperationalUpdate update);

    void publish(GuestFlowOperationalUpdate update);
}
