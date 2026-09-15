package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AttractionConfiguration {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    AttractionService attractionService(
            AttractionRepository repository,
            Clock clock,
            AttractionUpdatePublisher updatePublisher
    ) {
        return new AttractionService(repository, clock, updatePublisher);
    }

    @Bean
    GuestAttractionReadService guestAttractionReadService(
            AttractionRepository attractionRepository,
            AttractionExperienceRepository experienceRepository
    ) {
        return new GuestAttractionReadService(attractionRepository, experienceRepository);
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "venueops.attractions.seed", havingValue = "true")
    ApplicationRunner attractionSeedRunner(
            AttractionRepository repository,
            AttractionExperienceRepository experienceRepository,
            Clock clock
    ) {
        return args -> {
            if (repository.findAll().isEmpty()) {
                for (Attraction attraction : AttractionSeedData.attractions(clock)) {
                    repository.save(attraction);
                }
            }
            if (experienceRepository.findAll().isEmpty()) {
                for (AttractionExperienceProfile profile : AttractionSeedData.experienceProfiles()) {
                    experienceRepository.save(profile);
                }
            }
        };
    }
}
