package com.hyperswitch.common.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for MerchantId to handle String to MerchantId conversion
 */
public class MerchantIdDeserializer extends JsonDeserializer<MerchantId> {

    @Override
    public MerchantId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isNull()) {
            return null;
        }
        String value = node.asText();
        if (value == null || value.isEmpty()) {
            return null;
        }
        return MerchantId.of(value);
    }
}

