package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.AmountDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Jackson configuration for ObjectMapper bean and WebFlux codecs
 * 
 * Provides ObjectMapper bean required by services like DisputeServiceImpl,
 * SubscriptionServiceImpl, and PayoutServiceImpl.
 * Also configures WebFlux to use the custom ObjectMapper for JSON serialization/deserialization.
 * 
 * Implementation via WebFluxConfigurer:
 * - Registers custom ObjectMapper with WebFlux codecs using Jackson 3.x compatible classes
 * - Ensures all HTTP request/response serialization uses AmountDeserializer
 * - Compatible with Spring Boot 4.0.1 and Spring Framework 7.0 (Jackson 3.x support)
 */
@Configuration
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class JacksonConfig implements WebFluxConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);
    private final ObjectMapper objectMapper;

    private ServerCodecConfigurer codecConfigurer;
    private Jackson2JsonDecoder registeredDecoder;

    // Constructor injection - ObjectMapper is provided by ObjectMapperConfig
    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("=== JacksonConfig BEAN CREATED (constructor) ===");
        log.info("=== ObjectMapper injected into JacksonConfig via constructor ===");
        log.info("ObjectMapper instance: {}", objectMapper);
        log.info("ObjectMapper modules: {}", objectMapper.getRegisteredModuleIds());
    }
    
    /**
     * Post-construct verification to ensure decoder is properly registered
     * This runs after all beans are created and WebFlux is fully initialized
     */
    @PostConstruct
    public void verifyDecoderRegistration() {
        log.info("=== PostConstruct: Verifying decoder registration ===");
        if (codecConfigurer != null && registeredDecoder != null) {
            // Try to get the actual decoder being used
            try {
                // Get all registered readers
                var readers = codecConfigurer.getReaders();
                log.info("Total registered HttpMessageReaders: {}", readers.size());
                
                // Find our custom decoder
                boolean foundOurDecoder = readers.stream()
                    .anyMatch(LoggingJackson2JsonDecoder.class::isInstance);
                log.info("Our LoggingJackson2JsonDecoder found in readers: {}", foundOurDecoder);
                
                // Find any Jackson2JsonDecoder
                var jacksonDecoders = readers.stream()
                    .filter(Jackson2JsonDecoder.class::isInstance)
                    .toList();
                log.info("Total Jackson2JsonDecoder instances found: {}", jacksonDecoders.size());
                
                for (var decoder : jacksonDecoders) {
                    if (decoder instanceof LoggingJackson2JsonDecoder loggingDecoder) {
                        log.info("✓ Found our LoggingJackson2JsonDecoder in registered readers");
                        ObjectMapper decoderMapper = loggingDecoder.getObjectMapper();
                        log.info("Decoder ObjectMapper: {}", decoderMapper);
                        log.info("Decoder ObjectMapper modules: {}", decoderMapper.getRegisteredModuleIds());
                    } else if (decoder instanceof Jackson2JsonDecoder) {
                        log.warn("⚠ Found another Jackson2JsonDecoder (not ours): {}", decoder.getClass().getName());
                    }
                }
            } catch (Exception e) {
                log.error("Error verifying decoder registration: {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠ CodecConfigurer or registeredDecoder is null - cannot verify registration");
        }
        log.info("=== PostConstruct verification complete ===");
    }
    
    // ObjectMapper bean moved to ObjectMapperConfig to prevent circular dependencies

    
    /**
     * Configure WebFlux codecs to use our custom ObjectMapper.
     * This ensures that all HTTP request/response serialization/deserialization
     * uses the custom ObjectMapper with AmountDeserializer.
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
    @Override
    @SuppressWarnings("deprecation")
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        log.info("=== Configuring WebFlux codecs with custom ObjectMapper ===");
        
        // CRITICAL: Ensure ObjectMapper is available (it should be via setter injection)
        if (objectMapper == null) {
            log.error("ERROR: ObjectMapper is null! Cannot configure codecs. This should not happen.");
            return;
        }
        
        log.info("ObjectMapper instance: {}", objectMapper);
        log.info("ObjectMapper registered modules: {}", objectMapper.getRegisteredModuleIds());
        
        // Store configurer for PostConstruct verification
        this.codecConfigurer = configurer;
        
        // Verify AmountDeserializer is registered
        boolean hasAmountDeserializer = objectMapper.getRegisteredModuleIds().contains("AmountModule");
        log.info("AmountDeserializer registered: {}", hasAmountDeserializer);
        
        if (!hasAmountDeserializer) {
            log.error("ERROR: AmountDeserializer is NOT registered in ObjectMapper! Deserialization will fail.");
        }
        
        // Using deprecated methods until JacksonJsonEncoder/JacksonJsonDecoder are available
        Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(objectMapper);
        // Use our custom logging decoder to debug deserialization issues
        Jackson2JsonDecoder decoder = new LoggingJackson2JsonDecoder(objectMapper);
        
        // Store decoder for PostConstruct verification
        this.registeredDecoder = decoder;
        
        // Verify the decoder's ObjectMapper
        log.info("Decoder ObjectMapper instance: {}", decoder.getObjectMapper());
        log.info("Decoder ObjectMapper modules: {}", decoder.getObjectMapper().getRegisteredModuleIds());
        
        // CRITICAL: Check for existing Jackson2JsonDecoder that might conflict
        // Spring Boot auto-configuration might have already registered one
        try {
            var existingReaders = configurer.getReaders();
            log.info("Existing HttpMessageReaders before our registration: {}", existingReaders.size());
            
            // Count existing Jackson2JsonDecoders (excluding our custom one)
            long existingJacksonDecoders = existingReaders.stream()
                .filter(Jackson2JsonDecoder.class::isInstance)
                .filter(reader -> !(reader instanceof LoggingJackson2JsonDecoder))
                .count();
            
            if (existingJacksonDecoders > 0) {
                log.warn("⚠ Found {} existing Jackson2JsonDecoder(s) - our decoder should override them", existingJacksonDecoders);
            }
        } catch (Exception e) {
            log.warn("Could not check existing decoders: {}", e.getMessage());
        }
        
        // Set reasonable buffer size
        configurer.defaultCodecs().maxInMemorySize(256 * 1024);
        
        // CRITICAL: Register our decoder FIRST and as the PRIMARY decoder
        // This ensures it's selected over any default decoders
        configurer.defaultCodecs().jackson2JsonDecoder(decoder);
        configurer.defaultCodecs().jackson2JsonEncoder(encoder);
        
        // Also register in custom codecs as a fallback
        configurer.customCodecs().register(decoder);
        configurer.customCodecs().register(encoder);
        
        log.info("Jackson2JsonEncoder created with ObjectMapper: {}", encoder);
        log.info("Jackson2JsonDecoder created with ObjectMapper: {}", decoder);
        log.info("Custom decoder registered in both defaultCodecs and customCodecs");
        
        // Verify ObjectMapper instances match
        ObjectMapper decoderMapper = decoder.getObjectMapper();
        boolean sameInstance = decoderMapper == objectMapper;
        log.info("Decoder ObjectMapper is same instance as configured: {}", sameInstance);
        if (!sameInstance) {
            log.error("ERROR: Decoder is using a different ObjectMapper instance! This WILL cause deserialization issues.");
        }

        // Ensure our decoder reader is placed at the front of the readers list so it's selected first
        try {
            var readers = configurer.getReaders();

            // Remove any existing DecoderHttpMessageReaders that wrap a Jackson2JsonDecoder
            readers.removeIf(r -> {
                try {
                    if (r instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                        Object innerDecoder = dhr.getDecoder();
                        return innerDecoder instanceof org.springframework.http.codec.json.Jackson2JsonDecoder;
                    }
                } catch (Exception e) {
                    // ignore
                }
                return false;
            });

            // Add our reader at the start so it has priority
            org.springframework.http.codec.DecoderHttpMessageReader<?> ourReader = new org.springframework.http.codec.DecoderHttpMessageReader<>(decoder);
            readers.add(0, ourReader);
            log.info("Inserted our DecoderHttpMessageReader at index 0 to take precedence over other JSON readers");

            // Verify our reader is present and wraps our decoder
            boolean decoderInList = readers.stream().anyMatch(r -> {
                if (r instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                    Object inner = dhr.getDecoder();
                    return inner instanceof com.hyperswitch.web.config.LoggingJackson2JsonDecoder;
                }
                return r instanceof com.hyperswitch.web.config.LoggingJackson2JsonDecoder;
            });
            log.info("Our decoder is in the readers list: {}", decoderInList);
            if (!decoderInList) {
                log.error("ERROR: Our decoder is NOT in the readers list! WebFlux may not use it.");
            }
        } catch (Exception e) {
            log.warn("Could not modify HttpMessageReaders to insert our decoder: {}", e.getMessage());
        }
        
        log.info("=== WebFlux codecs configured successfully ===");
    }

    public void addArgumentResolvers(java.util.List<org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver> resolvers) {
        try {
            log.info("Registering CreatePaymentRequestArgumentResolver at highest precedence");
            resolvers.add(0, new CreatePaymentRequestArgumentResolver(this.codecConfigurer));
        } catch (Exception e) {
            log.error("Failed to register CreatePaymentRequestArgumentResolver: {}", e.getMessage(), e);
        }
    }

    /**
     * Expose our LoggingJackson2JsonDecoder as a Spring bean so it can be injected
     * into other components (e.g., an ApplicationReady listener which will
     * ensure it replaces any other Jackson-based decoders at runtime).
     */
    @Bean
    public LoggingJackson2JsonDecoder loggingJackson2JsonDecoder() {
        LoggingJackson2JsonDecoder decoder = new LoggingJackson2JsonDecoder(this.objectMapper);
        log.info("Registered LoggingJackson2JsonDecoder bean: {}", decoder);
        return decoder;
    }
}

