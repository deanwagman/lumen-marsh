package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import java.time.Clock;

public interface WorkOrderNumberGenerator {

    String next(Clock clock);
}
