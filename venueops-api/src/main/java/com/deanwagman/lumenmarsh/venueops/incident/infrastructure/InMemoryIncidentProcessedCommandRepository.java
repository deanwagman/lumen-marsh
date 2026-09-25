package com.deanwagman.lumenmarsh.venueops.incident.infrastructure;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentProcessedCommandRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryIncidentProcessedCommandRepository implements IncidentProcessedCommandRepository {

    private final Map<UUID, ProcessedCommand> commands = new ConcurrentHashMap<>();

    @Override
    public Optional<ProcessedCommand> findByCommandId(UUID commandId) {
        return Optional.ofNullable(commands.get(commandId));
    }

    @Override
    public void save(ProcessedCommand command) {
        commands.put(command.commandId(), command);
    }
}
