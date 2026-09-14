package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class AttractionSeedData {

    private AttractionSeedData() {
    }

    public static List<Attraction> attractions(Clock clock) {
        Instant now = Instant.now(clock);
        return List.of(
                Attraction.rehydrate(
                        new AttractionId("mangrove-run"),
                        "Mangrove Run",
                        "Luminous Wetlands",
                        AttractionType.BOAT_EXPEDITION,
                        AttractionStatus.OPERATING,
                        CapacityMode.NORMAL,
                        25,
                        now,
                        0L,
                        List.of()
                ),
                Attraction.create(
                        new AttractionId("stormglass-station"),
                        "Stormglass Station",
                        "Research Quarter",
                        AttractionType.INDOOR_DARK_RIDE,
                        clock
                ),
                Attraction.create(
                        new AttractionId("cypress-coil"),
                        "Cypress Coil",
                        "Cypress Basin",
                        AttractionType.LAUNCH_COASTER,
                        clock
                )
        );
    }

    public static List<AttractionExperienceProfile> experienceProfiles() {
        return List.of(
                AttractionExperienceProfile.create(
                        new AttractionId("mangrove-run"),
                        "Glide beneath a living canopy through the luminous wetlands.",
                        8,
                        null,
                        Intensity.GENTLE,
                        Environment.OUTDOOR,
                        false,
                        "Guests must transfer into the ride vehicle.",
                        new ExperienceMedia(
                                "/media/attractions/mangrove-run/hero.webp",
                                "/media/attractions/mangrove-run/thumbnail.webp",
                                "An expedition boat moving through a glowing mangrove forest."
                        )
                ),
                AttractionExperienceProfile.create(
                        new AttractionId("stormglass-station"),
                        "Explore a research outpost where stormglass instruments reveal hidden weather patterns.",
                        12,
                        42,
                        Intensity.MODERATE,
                        Environment.INDOOR,
                        false,
                        "Wheelchair accessible queue and load area. Transfer required for ride vehicle.",
                        new ExperienceMedia(
                                "/media/attractions/stormglass-station/hero.webp",
                                "/media/attractions/stormglass-station/thumbnail.webp",
                                "A glass-walled research station glowing with stormglass instruments."
                        )
                ),
                AttractionExperienceProfile.create(
                        new AttractionId("cypress-coil"),
                        "Launch through the cypress basin on a high-speed coil through marsh mist.",
                        3,
                        48,
                        Intensity.THRILL,
                        Environment.OUTDOOR,
                        true,
                        "Guests must be able to sit upright with a lap bar restraint.",
                        new ExperienceMedia(
                                "/media/attractions/cypress-coil/hero.webp",
                                "/media/attractions/cypress-coil/thumbnail.webp",
                                "A launch coaster racing through a misty cypress forest."
                        )
                )
        );
    }
}
