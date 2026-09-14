package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(VenueOpsSecurityProperties.class)
public class SecurityConfiguration {

    private final VenueOpsSecurityProperties properties;

    public SecurityConfiguration(VenueOpsSecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    WeatherIngestRateLimitFilter weatherIngestRateLimitFilter() {
        return new WeatherIngestRateLimitFilter(properties);
    }

    @Bean
    SecurityFilterChain venueOpsSecurityFilterChain(
            HttpSecurity http,
            WeatherIngestRateLimitFilter weatherIngestRateLimitFilter
    ) throws Exception {
        // CSRF disabled: the API uses bearer tokens and stateless sessions, not cookie auth.
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (properties.exposeApiDocs()) {
                        auth.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attractions", "/api/v1/attractions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/advisories", "/api/v1/advisories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                        .requestMatchers("/api/v1/integrations/weather/**")
                        .hasAuthority(VenueOpsScopes.SCOPE_WEATHER_WRITE)
                        .requestMatchers("/api/v1/operator/**")
                        .hasAuthority(VenueOpsScopes.ROLE_OPERATOR)
                        .anyRequest().denyAll();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new VenueOpsJwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(
                        weatherIngestRateLimitFilter,
                        org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
                );
        if ("LOCAL_JWT".equalsIgnoreCase(properties.mode().name())) {
            http.addFilterBefore(
                    new LocalDevBearerAuthenticationFilter(properties),
                    org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
            );
        }
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "venueops.security.mode", havingValue = "OIDC")
    JwtDecoder oidcJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuerUri()).build();
        decoder.setJwtValidator(VenueOpsJwtValidators.accessToken(properties.issuerUri(), properties.allowedClientIdSet()));
        return decoder;
    }

    @Bean
    @ConditionalOnProperty(name = "venueops.security.mode", havingValue = "LOCAL_JWT")
    KeyPair localJwtKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    @ConditionalOnProperty(name = "venueops.security.mode", havingValue = "LOCAL_JWT")
    JwtDecoder localJwtDecoder(KeyPair localJwtKeyPair) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) localJwtKeyPair.getPublic())
                .build();
        decoder.setJwtValidator(VenueOpsJwtValidators.accessToken(properties.issuerUri(), properties.allowedClientIdSet()));
        return decoder;
    }

    @Bean
    @ConditionalOnProperty(name = "venueops.security.mode", havingValue = "LOCAL_JWT")
    JwtEncoder localJwtEncoder(KeyPair localJwtKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) localJwtKeyPair.getPublic())
                .privateKey((RSAPrivateKey) localJwtKeyPair.getPrivate())
                .keyID("venueops-local")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    private static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            SecurityAccessLogger.authenticationFailure(request, ex);
            writeProblem(response, 401, "Unauthorized", "Authentication is required.");
        };
    }

    private static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SecurityAccessLogger.authorizationFailure(request, authentication, ex);
            writeProblem(response, 403, "Forbidden", "You are not allowed to perform this action.");
        };
    }

    private static void writeProblem(
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String title,
            String detail
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = "{\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

}
