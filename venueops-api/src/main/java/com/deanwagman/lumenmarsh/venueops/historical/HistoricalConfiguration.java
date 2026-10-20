package com.deanwagman.lumenmarsh.venueops.historical;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;

@Configuration
public class HistoricalConfiguration {

    @Bean
    @Order(20)
    @ConditionalOnProperty(name = "venueops.historical.seed", havingValue = "true", matchIfMissing = true)
    ApplicationRunner historicalFixtureSeedRunner(HistoricalReplayService replayService) {
        return args -> replayService.replayFixture(FIXTURE_DATASET_ID, false);
    }
}
