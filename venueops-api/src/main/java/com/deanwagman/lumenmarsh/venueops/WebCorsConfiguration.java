package com.deanwagman.lumenmarsh.venueops;

import com.deanwagman.lumenmarsh.venueops.security.VenueOpsSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(VenueOpsSecurityProperties.class)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final VenueOpsSecurityProperties securityProperties;

    public WebCorsConfiguration(VenueOpsSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = securityProperties.allowedOriginPatterns();
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        registry.addMapping("/media/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .maxAge(3600);
    }
}
