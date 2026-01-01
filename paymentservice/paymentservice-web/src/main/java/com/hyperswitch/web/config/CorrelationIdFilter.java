package com.hyperswitch.web.config;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filter to add correlation ID to all requests for distributed tracing
 */
@Component
@Order(1)
public class CorrelationIdFilter implements WebFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String correlationIdHeader = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        final String correlationId = (correlationIdHeader == null || correlationIdHeader.isEmpty()) 
            ? UUID.randomUUID().toString() 
            : correlationIdHeader;
        
        // Add to response headers - use beforeCommit to avoid ReadOnlyHttpHeaders issues in test/mock contexts
        try {
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        } catch (UnsupportedOperationException e) {
            // In some test contexts the headers are read-only until commit; defer addition to beforeCommit
            exchange.getResponse().beforeCommit(() -> {
                exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
                return Mono.empty();
            });
        }
        
        // Add to MDC for logging
        return chain.filter(exchange)
            .doOnEach(signal -> {
                try {
                    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
                } catch (Exception ignored) {
                    // Ignore MDC errors
                }
            })
            .doFinally(signalType -> {
                try {
                    MDC.remove(CORRELATION_ID_MDC_KEY);
                } catch (Exception ignored) {
                    // Ignore MDC errors
                }
            });
    }
}

