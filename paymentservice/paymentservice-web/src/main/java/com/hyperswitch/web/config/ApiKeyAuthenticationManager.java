package com.hyperswitch.web.config;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * Authentication manager for API key validation
 * In production, this would validate API keys against a database
 */
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private static final List<SimpleGrantedAuthority> AUTHORITIES = 
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"));

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // In production, validate API key against database/cache
        // For now, accept any non-empty API key
        if (authentication != null && authentication.getPrincipal() != null) {
            String apiKey = authentication.getPrincipal().toString();
            if (!apiKey.isEmpty()) {
                // Create authenticated token
                Authentication authenticated = new UsernamePasswordAuthenticationToken(
                    apiKey,
                    null,
                    AUTHORITIES
                );
                return Mono.just(authenticated);
            }
        }
        return Mono.empty();
    }
}

