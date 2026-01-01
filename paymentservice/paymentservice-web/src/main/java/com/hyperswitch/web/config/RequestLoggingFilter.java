package com.hyperswitch.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

// For debug deserialization check
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.CreatePaymentRequest;

/**
 * WebFilter to log request body for debugging
 * 
 * This filter captures and logs the raw request body for payment creation requests
 * to help diagnose deserialization issues.
 * 
 * IMPORTANT: @Order(0) ensures this filter runs BEFORE the decoder processes the body,
 * allowing us to cache and replay the body for downstream processing.
 */
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements WebFilter {
    // Very early debug logs to verify this filter runs before other filters that may consume the body
    private static final boolean EARLY_DEBUG = true;
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Debug-only: inject ObjectMapper to verify direct deserialization of the cached body
    // This helps determine whether the configured ObjectMapper can parse the incoming payload
    // even if WebFlux decoding path skips our LoggingJackson2JsonDecoder.
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        log.info("RequestLoggingFilter invoked - method={}, path={}, content-length={}", request.getMethod(), path, request.getHeaders().getContentLength());
        System.out.println("[STDOUT] RequestLoggingFilter invoked - method=" + request.getMethod() + " path=" + path + " content-length=" + request.getHeaders().getContentLength());
        
        // Only log for payment creation endpoint
        if ("/api/payments".equals(path) && "POST".equals(request.getMethod().name())) {
            if (EARLY_DEBUG) {
                log.info("Early debug: entering RequestLoggingFilter body-capture branch");
            }            return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    try {
                        // Read all bytes - this consumes the original buffer
                        int readableBytes = dataBuffer.readableByteCount();
                        byte[] bytes = new byte[readableBytes];
                        dataBuffer.read(bytes);
                        
                        // Log the request body
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        log.info("=== RAW REQUEST BODY ===");
                        log.info("Path: {}", path);
                        log.info("Content-Type: {}", request.getHeaders().getFirst("Content-Type"));
                        log.info("Body length: {} bytes", bytes.length);
                        log.info("Body: {}", body);
                        log.info("=== END RAW REQUEST BODY ===");

                        // DEBUG CHECK: Try direct deserialization using the injected ObjectMapper
                        CreatePaymentRequest cachedDirect = null;
                        if (objectMapper != null) {
                            try {
                                CreatePaymentRequest direct = objectMapper.readValue(body, CreatePaymentRequest.class);
                                log.info("=== DEBUG: ObjectMapper.readValue result === merchantId={}, amount={}", direct.getMerchantId(), direct.getAmount());

                                // Store the directly parsed object temporarily - we'll attach it to the
                                // mutated exchange immediately before delegating to the next filter.
                                cachedDirect = direct;
                            } catch (Exception e) {
                                log.error("=== DEBUG: ObjectMapper.readValue failed: {} ===", e.getMessage(), e);
                            }
                        } else {
                            log.warn("=== DEBUG: ObjectMapper is not available in RequestLoggingFilter ===");
                        }
                        
                        // Release the original buffer since we've consumed it
                        DataBufferUtils.release(dataBuffer);
                        
                        // Create a decorator that provides the cached body
                        // CRITICAL: Properly create a new buffer that can be read from the beginning
                        // This ensures the decoder can properly read the body
                        // Pattern from: https://docs.spring.io/spring-framework/reference/web/webflux-webclient/body-filters.html
                        // IMPORTANT: Create a fresh buffer each time getBody() is called, as WebFlux may call it multiple times
                        final byte[] cachedBytes = bytes; // Final reference for use in inner class
                        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                // Create a NEW buffer each time getBody() is called
                                // Use wrap() which creates a buffer that wraps the byte array
                                // This is simpler and ensures the buffer is readable from position 0
                                // CRITICAL: wrap() creates a buffer that can be read immediately
                                DataBuffer cachedBuffer = exchange.getResponse().bufferFactory().wrap(cachedBytes);
                                log.debug("Replaying cached request body - buffer size: {}, readable bytes: {}", 
                                    cachedBuffer.capacity(), cachedBuffer.readableByteCount());
                                return Flux.just(cachedBuffer);
                            }
                        };
                        
                        // Mutate the exchange with the decorated request that replays the cached body
                        // This ensures the decoder receives a readable body stream
                        ServerWebExchange mutatedExchange = exchange.mutate().request(decoratedRequest).build();
                        if (cachedDirect != null) {
                            mutatedExchange.getAttributes().put("directCreatePaymentRequest", cachedDirect);
                            log.info("Attached cached direct CreatePaymentRequest to mutated exchange attributes: merchantId={}, amount={}", cachedDirect.getMerchantId(), cachedDirect.getAmount());
                        }
                        log.info("Request mutated with cached body - Content-Type: {}, proceeding to decoder", 
                            decoratedRequest.getHeaders().getFirst("Content-Type"));
                        return chain.filter(mutatedExchange);
                    } catch (Exception e) {
                        log.error("Error in RequestLoggingFilter: {}", e.getMessage(), e);
                        DataBufferUtils.release(dataBuffer);
                        return chain.filter(exchange);
                    }
                })
                .switchIfEmpty(chain.filter(exchange));
        }
        
        return chain.filter(exchange);
    }
}

