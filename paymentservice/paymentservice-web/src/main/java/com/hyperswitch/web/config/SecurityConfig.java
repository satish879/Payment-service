package com.hyperswitch.web.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

/**
 * Security configuration
 * Implements API key-based authentication for payment service endpoints
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${hyperswitch.security.enable-csrf:false}")
    private boolean enableCsrf;

    @Value("${hyperswitch.security.enable-auth:false}")
    private boolean enableAuth;

    @Autowired(required = false)
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @PostConstruct
    public void init() {
        logger.error("=== SecurityConfig BEAN CREATED ===");
        logger.error("enableAuth: {}, enableCsrf: {}", enableAuth, enableCsrf);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        
        logger.info("=== Configuring Spring Security ===");
        logger.info("enableAuth: {}, enableCsrf: {}", enableAuth, enableCsrf);
        
        // Always disable CSRF if not explicitly enabled
        ServerHttpSecurity httpSecurity = http.csrf(csrf -> csrf.disable());
        
        // Configure authentication
        if (enableAuth && apiKeyAuthenticationFilter != null) {
            logger.info("Authentication is ENABLED - configuring API key authentication");
            AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(
                new ApiKeyAuthenticationManager()
            );
            authenticationWebFilter.setServerAuthenticationConverter(apiKeyAuthenticationFilter);
            
            httpSecurity = httpSecurity
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                    // Permit all health and actuator endpoints
                    .pathMatchers(
                        "/health", 
                        "/health/**", 
                        "/api/health", 
                        "/api/health/**",
                        "/v2/health",
                        "/v2/health/**",
                        "/actuator/**",
                        "/api/webhooks/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/webjars/**"
                    ).permitAll()
                    .anyExchange().authenticated()
                );
        } else {
            logger.info("Authentication is DISABLED - permitting all requests without authentication");
            // When authentication is disabled, completely disable security requirements
            // IMPORTANT: This must come BEFORE any authentication configuration
            httpSecurity = httpSecurity
                .authorizeExchange(exchanges -> exchanges
                    .anyExchange().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable());
        }
        
        logger.info("=== Spring Security configuration complete ===");
        return httpSecurity.build();
    }
}


