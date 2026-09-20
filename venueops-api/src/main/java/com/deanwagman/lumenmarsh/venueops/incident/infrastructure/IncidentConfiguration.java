package com.deanwagman.lumenmarsh.venueops.incident.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentResolutionGuard;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentUpdatePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class IncidentConfiguration {

    @Bean
    IncidentService incidentService(
            IncidentRepository incidents,
            AttractionRepository attractions,
            IncidentProcessedCommandRepository processedCommands,
            Clock clock,
            GuestAdvisoryUpdatePublisher guestAdvisoryPublisher,
            IncidentUpdatePublisher incidentPublisher,
            IncidentResolutionGuard resolutionGuard
    ) {
        return new IncidentService(
                incidents,
                attractions,
                processedCommands,
                clock,
                guestAdvisoryPublisher,
                incidentPublisher,
                resolutionGuard
        );
    }
}
