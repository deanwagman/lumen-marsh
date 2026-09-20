package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorResolver;
import com.deanwagman.lumenmarsh.venueops.security.CommandAuthorization;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/attractions")
@Tag(name = "Operator attractions")
public class OperatorAttractionController {

    private final AttractionService attractionService;
    private final ActorResolver actorResolver;
    private final CommandAuthorization commandAuthorization;

    public OperatorAttractionController(
            AttractionService attractionService,
            ActorResolver actorResolver,
            CommandAuthorization commandAuthorization
    ) {
        this.attractionService = attractionService;
        this.actorResolver = actorResolver;
        this.commandAuthorization = commandAuthorization;
    }

    @GetMapping("/{attractionId}")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    public OperatorAttractionResponse get(@PathVariable String attractionId) {
        return OperatorAttractionResponse.from(attractionService.get(new AttractionId(attractionId)));
    }

    @GetMapping("/{attractionId}/activity")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    public List<AttractionActivityResponse> activity(@PathVariable String attractionId) {
        return attractionService.activity(new AttractionId(attractionId)).stream()
                .map(AttractionActivityResponse::from)
                .toList();
    }

    @PostMapping("/{attractionId}/commands")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_ATTRACTIONS_COMMAND + "')")
    public OperatorAttractionResponse command(
            @PathVariable String attractionId,
            @Valid @RequestBody AttractionCommandRequest request
    ) {
        commandAuthorization.requireAttractionCommand();
        ActorIdentity actor = actorResolver.requireActor();
        return ActorAuditContext.call(actor, () -> OperatorAttractionResponse.from(attractionService.execute(
                new AttractionId(attractionId),
                request.commandId(),
                request.type(),
                actor.auditLabel(),
                request.reason(),
                request.waitMinutes(),
                request.expectedVersion()
        )));
    }
}
