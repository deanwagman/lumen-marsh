package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public class IncidentService {

    private final IncidentRepository incidents;
    private final AttractionRepository attractions;
    private final Clock clock;
    private final GuestAdvisoryUpdatePublisher guestAdvisoryPublisher;
    private final IncidentUpdatePublisher incidentPublisher;

    public IncidentService(
            IncidentRepository incidents,
            AttractionRepository attractions,
            Clock clock,
            GuestAdvisoryUpdatePublisher guestAdvisoryPublisher,
            IncidentUpdatePublisher incidentPublisher
    ) {
        this.incidents = Objects.requireNonNull(incidents);
        this.attractions = Objects.requireNonNull(attractions);
        this.clock = Objects.requireNonNull(clock);
        this.guestAdvisoryPublisher = Objects.requireNonNull(guestAdvisoryPublisher);
        this.incidentPublisher = Objects.requireNonNull(incidentPublisher);
    }

    public List<Incident> list() {
        return incidents.findAll();
    }

    public Incident get(IncidentId id) {
        return incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    public List<IncidentActivity> activity(IncidentId id) {
        return get(id).activity();
    }

    public List<Incident> listActiveGuestAdvisories() {
        return incidents.findAll().stream()
                .filter(Incident::hasActiveGuestAdvisory)
                .toList();
    }

    public Incident report(
            String title,
            IncidentType type,
            IncidentSeverity severity,
            String internalDescription,
            List<String> attractionIds,
            String actor
    ) {
        List<AttractionId> links = attractionIds == null ? List.of() : attractionIds.stream()
                .map(AttractionId::new)
                .toList();
        requireKnownAttractions(links);
        Incident incident = Incident.report(title, type, severity, internalDescription, links, actor, clock);
        IncidentActivity activity = lastUncommitted(incident);
        incidents.save(incident);
        publishIncident(activity, incident);
        publishGuestAdvisoryIfNeeded(activity, incident);
        return incident;
    }

    public Incident execute(
            IncidentId id,
            IncidentCommand command,
            String actor,
            String reason,
            long expectedVersion,
            String assignee,
            IncidentSeverity severity,
            String attractionId,
            String guestTitle,
            String guestMessage
    ) {
        Incident incident = get(id);
        if (incident.version() != expectedVersion) {
            throw new StaleIncidentVersionException(id, expectedVersion, incident.version());
        }
        apply(incident, command, actor, reason, assignee, severity, attractionId, guestTitle, guestMessage);
        IncidentActivity activity = lastUncommitted(incident);
        incidents.save(incident);
        publishIncident(activity, incident);
        publishGuestAdvisoryIfNeeded(activity, incident);
        return incident;
    }

    private void publishIncident(IncidentActivity activity, Incident incident) {
        incidentPublisher.publish(IncidentOperationalUpdate.from(activity, incident));
    }

    private void publishGuestAdvisoryIfNeeded(IncidentActivity activity, Incident incident) {
        GuestAdvisoryOperationalUpdate update = GuestAdvisoryOperationalUpdate.fromActivity(activity, incident);
        if (update != null) {
            guestAdvisoryPublisher.publish(update);
        }
    }

    private void apply(
            Incident incident,
            IncidentCommand command,
            String actor,
            String reason,
            String assignee,
            IncidentSeverity severity,
            String attractionId,
            String guestTitle,
            String guestMessage
    ) {
        switch (command) {
            case ACKNOWLEDGE -> incident.acknowledge(actor, reason, clock);
            case ASSIGN -> incident.assign(requireCommandValue(assignee, "ASSIGN requires assignee"), actor, reason, clock);
            case START_MITIGATION -> incident.startMitigation(actor, reason, clock);
            case CHANGE_SEVERITY -> {
                if (severity == null) {
                    throw new IllegalArgumentException("CHANGE_SEVERITY requires severity");
                }
                incident.changeSeverity(severity, actor, reason, clock);
            }
            case LINK_ATTRACTION -> {
                AttractionId link = new AttractionId(requireCommandValue(attractionId, "LINK_ATTRACTION requires attractionId"));
                requireKnownAttractions(List.of(link));
                incident.linkAttraction(link, actor, reason, clock);
            }
            case UNLINK_ATTRACTION -> incident.unlinkAttraction(
                    new AttractionId(requireCommandValue(attractionId, "UNLINK_ATTRACTION requires attractionId")),
                    actor,
                    reason,
                    clock
            );
            case PUBLISH_GUEST_ADVISORY -> incident.publishGuestAdvisory(guestTitle, guestMessage, actor, reason, clock);
            case WITHDRAW_GUEST_ADVISORY -> incident.withdrawGuestAdvisory(actor, reason, clock);
            case RESOLVE -> incident.resolve(actor, reason, clock);
        }
    }

    private void requireKnownAttractions(List<AttractionId> attractionIds) {
        for (AttractionId attractionId : attractionIds) {
            if (attractions.findById(attractionId).isEmpty()) {
                throw new AttractionNotFoundException(attractionId);
            }
        }
    }

    private static IncidentActivity lastUncommitted(Incident incident) {
        List<IncidentActivity> uncommitted = incident.uncommittedActivity();
        if (uncommitted.isEmpty()) {
            throw new IllegalStateException("Accepted command produced no activity");
        }
        return uncommitted.getLast();
    }

    private static String requireCommandValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
