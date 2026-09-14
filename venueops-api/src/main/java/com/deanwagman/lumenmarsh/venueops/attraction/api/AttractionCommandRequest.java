package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import jakarta.validation.constraints.NotNull;

public record AttractionCommandRequest(
        @NotNull AttractionCommand type,
        String reason,
        @NotNull Long expectedVersion,
        Integer waitMinutes
) {
}
