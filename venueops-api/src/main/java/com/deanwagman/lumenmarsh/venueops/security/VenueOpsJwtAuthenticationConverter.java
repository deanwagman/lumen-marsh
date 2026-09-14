package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps Cognito scopes and groups onto Spring authorities.
 */
public class VenueOpsJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(mapScopes(jwt));
        authorities.addAll(mapGroups(jwt));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    static Collection<GrantedAuthority> mapScopes(Jwt jwt) {
        Set<String> scopes = new HashSet<>();
        Object scopeClaim = jwt.getClaims().get("scope");
        if (scopeClaim instanceof String scopeString) {
            for (String part : scopeString.split(" ")) {
                if (!part.isBlank()) {
                    scopes.add(part.trim());
                }
            }
        }
        Object scp = jwt.getClaims().get("scp");
        if (scp instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    scopes.add(item.toString());
                }
            }
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String scope : scopes) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return authorities;
    }

    static Collection<GrantedAuthority> mapGroups(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object groupsClaim = jwt.getClaims().get("cognito:groups");
        if (!(groupsClaim instanceof Collection<?> groups)) {
            return authorities;
        }
        for (Object group : groups) {
            if (group == null) {
                continue;
            }
            String name = group.toString().trim().toLowerCase(Locale.ROOT);
            if ("operators".equals(name)) {
                authorities.add(new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR));
            } else if ("supervisors".equals(name)) {
                authorities.add(new SimpleGrantedAuthority(VenueOpsScopes.ROLE_SUPERVISOR));
                authorities.add(new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR));
            }
        }
        return authorities;
    }
}
