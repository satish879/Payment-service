package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration
 * Provides WebClient.Builder bean for services that need it.
 * 
 * Ensures WebClient uses the same custom ObjectMapper (with AmountDeserializer)
 * as the rest of the application for consistent JSON serialization/deserialization.
 * 
 * By default, Spring Boot 4.0.1 auto-configures WebClient.Builder to use the primary
 * ObjectMapper and application codecs. We explicitly configure it here to ensure
 * consistency across all WebClient instances, including manually created ones.
 * 
 * Uses Jackson 3.x compatible classes (JacksonJsonEncoder/JacksonJsonDecoder)
 * as per Spring Framework 7.0 and Spring Boot 4.0.1 standards.
 */
@Configuration
public class WebClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
    private final ObjectMapper objectMapper;
    
    public WebClientConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("WebClientConfig initialized with ObjectMapper: {}", objectMapper != null);
    }
    
    /**
     * Provides WebClient.Builder bean configured with custom ObjectMapper.
     * This ensures that all WebClient instances created from this builder
     * will use the custom ObjectMapper with AmountDeserializer.
     * 
     * TODO: Migrate to JacksonJsonEncoder/JacksonJsonDecoder when available in Spring Boot 4.0.1+
     * Currently using deprecated Jackson2JsonEncoder/Jackson2JsonDecoder as the new classes
     * (JacksonJsonEncoder/JacksonJsonDecoder) are not yet available in Spring Boot 4.0.1.
     * 
     * According to Spring Framework 7.0 documentation, the new classes should be used:
     * - JacksonJsonEncoder instead of Jackson2JsonEncoder
     * - JacksonJsonDecoder instead of Jackson2JsonDecoder
     * - Methods: jacksonJsonEncoder() and jacksonJsonDecoder() instead of jackson2JsonEncoder/Decoder()
     */
    @Bean
    @SuppressWarnings("deprecation")
    public WebClient.Builder webClientBuilder() {
        log.info("Creating WebClient.Builder bean with custom ObjectMapper");
        if (objectMapper == null) {
            log.warn("ObjectMapper is null, creating WebClient.Builder without custom codecs");
            return WebClient.builder();
        }
        return WebClient.builder()
            .codecs(configurer -> {
                // Using deprecated methods until JacksonJsonEncoder/JacksonJsonDecoder are available
                configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            });
    }
}

