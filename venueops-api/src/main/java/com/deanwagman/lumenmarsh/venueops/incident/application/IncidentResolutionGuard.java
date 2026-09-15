package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;

public interface IncidentResolutionGuard {

    void assertResolutionAllowed(Incident incident, boolean confirmActiveWorkOrders);
}
