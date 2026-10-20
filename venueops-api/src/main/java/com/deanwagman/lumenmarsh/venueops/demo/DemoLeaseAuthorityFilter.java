package com.deanwagman.lumenmarsh.venueops.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class DemoLeaseAuthorityFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    private final DemoLeaseService leases;

    DemoLeaseAuthorityFilter(DemoLeaseService leases) {
        this.leases = leases;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE.contains(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/operator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, DemoLeaseService.COOKIE);
        try {
            leases.require(cookie == null ? null : cookie.getValue(), true);
            filterChain.doFilter(request, response);
        } catch (DemoLeaseService.InvalidLeaseException invalid) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"title\":\"Active sandbox lease required\",\"status\":403}"
                            .getBytes(StandardCharsets.UTF_8)
            );
        } catch (DemoLeaseService.DemoRateLimitException limited) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"title\":\"Demo mutation rate exceeded\",\"status\":429}"
                            .getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
