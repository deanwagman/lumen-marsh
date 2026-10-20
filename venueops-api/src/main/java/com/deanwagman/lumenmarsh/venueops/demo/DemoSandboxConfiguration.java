package com.deanwagman.lumenmarsh.venueops.demo;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetRepository;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsSecurityProperties;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "venueops.demo-sandbox.enabled", havingValue = "true")
@EnableConfigurationProperties(DemoSandboxProperties.class)
class DemoSandboxConfiguration {

    @Bean
    RestClient demoRestClient() {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(2));
        requests.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requests).build();
    }

    @Bean
    DemoLeaseStore demoLeaseStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        return new DemoLeaseStore(jdbc, transactions);
    }

    @Bean
    DemoResetService demoResetService(
            DemoSandboxProperties properties,
            RestClient demoRestClient,
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            EntityManager entityManager,
            AttractionRepository attractions,
            AttractionExperienceRepository experiences,
            MaintenanceAssetRepository assets,
            Clock clock
    ) {
        return new DemoResetService(
                properties, demoRestClient, jdbc, transactions,
                entityManager, attractions, experiences, assets, clock);
    }

    @Bean
    DemoLeaseService demoLeaseService(
            DemoSandboxProperties properties,
            DemoLeaseStore leases,
            DemoResetService resets,
            Clock clock
    ) {
        return new DemoLeaseService(properties, leases, resets, clock);
    }

    @Bean
    DemoGatewayService demoGatewayService(DemoSandboxProperties properties, RestClient demoRestClient) {
        return new DemoGatewayService(properties, demoRestClient);
    }

    @Bean
    DemoLeaseAuthorityFilter demoLeaseAuthorityFilter(DemoLeaseService leases) {
        return new DemoLeaseAuthorityFilter(leases);
    }

    @Bean
    DemoGatewayAuthenticationFilter demoGatewayAuthenticationFilter(
            DemoSandboxProperties properties,
            VenueOpsSecurityProperties securityProperties
    ) {
        return new DemoGatewayAuthenticationFilter(properties, securityProperties);
    }
}
