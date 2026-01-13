package com.hyperswitch.common.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for CustomerId to handle String to CustomerId conversion
 */
public class CustomerIdDeserializer extends JsonDeserializer<CustomerId> {

    @Override
    public CustomerId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isNull()) {
            return null;
        }
        String value = node.asText();
        if (value == null || value.isEmpty()) {
            return null;
        }
        return CustomerId.of(value);
    }
}

