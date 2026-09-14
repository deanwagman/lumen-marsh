package com.deanwagman.lumenmarsh.venueops.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

public class WeatherIngestRateLimitFilter extends OncePerRequestFilter {

    static final String INGEST_PATH = "/api/v1/integrations/weather/recommendations";

    private final WeatherIngestRateLimiter limiter;

    public WeatherIngestRateLimitFilter(VenueOpsSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    WeatherIngestRateLimitFilter(VenueOpsSecurityProperties properties, Clock clock) {
        this.limiter = new WeatherIngestRateLimiter(properties.weatherIngestRateLimitPerMinute(), clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limiter.isDisabled()
                || !HttpMethod.POST.matches(request.getMethod())
                || !INGEST_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!limiter.tryAcquire(clientKey(request))) {
            SecurityAccessLogger.rateLimited(request);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            byte[] body = """
                    {"title":"Too Many Requests","status":429,"detail":"Weather ingest rate limit exceeded."}
                    """.getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma < 0 ? forwarded : forwarded.substring(0, comma);
            return first.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
