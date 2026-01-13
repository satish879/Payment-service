package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.AmountDeserializer;
import com.hyperswitch.common.types.MerchantId;
import com.hyperswitch.common.types.MerchantIdDeserializer;
import com.hyperswitch.common.types.CustomerId;
import com.hyperswitch.common.types.CustomerIdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dedicated configuration for the primary ObjectMapper bean.
 * Moved out of `JacksonConfig` to avoid circular dependencies with WebFlux config.
 */
@Configuration
public class ObjectMapperConfig {
    private static final Logger log = LoggerFactory.getLogger(ObjectMapperConfig.class);

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("=== Creating ObjectMapper bean with AmountDeserializer (ObjectMapperConfig) ===");

        ObjectMapper mapper = new ObjectMapper();

        // Register Java time module
        mapper.registerModule(new JavaTimeModule());

        // Register custom deserializers with AmountModule ID for JacksonConfig verification
        SimpleModule customModule = new SimpleModule("AmountModule");
        AmountDeserializer amountDeserializer = new AmountDeserializer();
        MerchantIdDeserializer merchantIdDeserializer = new MerchantIdDeserializer();
        CustomerIdDeserializer customerIdDeserializer = new CustomerIdDeserializer();
        
        customModule.addDeserializer(Amount.class, amountDeserializer);
        customModule.addDeserializer(MerchantId.class, merchantIdDeserializer);
        customModule.addDeserializer(CustomerId.class, customerIdDeserializer);
        mapper.registerModule(customModule);

        log.info("Registered AmountDeserializer: {}", amountDeserializer.getClass().getName());
        log.info("Registered MerchantIdDeserializer: {}", merchantIdDeserializer.getClass().getName());
        log.info("Registered CustomerIdDeserializer: {}", customerIdDeserializer.getClass().getName());

        // Date handling
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Deserialization features
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        log.info("ObjectMapper configured with AmountDeserializer - registered modules: {}", mapper.getRegisteredModuleIds());
        log.info("=== ObjectMapper bean created successfully ===");

        return mapper;
    }
}
