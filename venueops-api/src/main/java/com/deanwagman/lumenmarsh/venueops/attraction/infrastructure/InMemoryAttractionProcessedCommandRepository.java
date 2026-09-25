package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionProcessedCommandRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAttractionProcessedCommandRepository implements AttractionProcessedCommandRepository {

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
