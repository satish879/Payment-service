package com.hyperswitch.common.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Custom deserializer for Amount class
 * 
 * Handles deserialization of Amount objects from JSON.
 * Expected JSON format: {"value": <number>, "currencyCode": "<string>"}
 */
public class AmountDeserializer extends JsonDeserializer<Amount> {
    
    private static final Logger log = LoggerFactory.getLogger(AmountDeserializer.class);
    
    @Override
    public Amount deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        log.info("=== AmountDeserializer.deserialize() CALLED ===");
        log.info("Current token: {}", p.currentToken());
        String currentName = p.currentName();
        if (currentName != null) {
            log.info("Current name: {}", currentName);
        }
        
        try {
            JsonNode node = p.getCodec().readTree(p);
            log.info("Parsed JSON node type: {}, node: {}", node.getNodeType(), node);
            
            if (node == null || node.isNull()) {
                log.error("Amount JSON node is null");
                throw new IllegalArgumentException("Amount cannot be null");
            }
            
            // Handle case where amount might be sent as a simple number (fallback)
            if (node.isNumber()) {
                log.warn("Amount received as number without currencyCode. This is not supported. Expected format: {{\"value\": <number>, \"currencyCode\": \"<string>\"}}");
                throw new IllegalArgumentException("Amount must be an object with 'value' and 'currencyCode' fields");
            }
            
            // Handle case where amount might be sent as a string
            if (node.isTextual()) {
                log.warn("Amount received as string. Expected format: {{\"value\": <number>, \"currencyCode\": \"<string>\"}}");
                throw new IllegalArgumentException("Amount must be an object with 'value' and 'currencyCode' fields");
            }
            
            if (!node.isObject()) {
                log.error("Amount is not an object. Node type: {}, node: {}", node.getNodeType(), node);
                throw new IllegalArgumentException("Amount must be an object with 'value' and 'currencyCode' fields");
            }
            
            JsonNode valueNode = node.get("value");
            JsonNode currencyCodeNode = node.get("currencyCode");
            
            boolean valueNodeIsNull = valueNode == null || valueNode.isNull();
            boolean currencyCodeNodeIsNull = currencyCodeNode == null || currencyCodeNode.isNull();
            log.info("valueNode: {} (null: {})", valueNode, valueNodeIsNull);
            log.info("currencyCodeNode: {} (null: {})", currencyCodeNode, currencyCodeNodeIsNull);
            
            if (valueNode == null || valueNode.isNull()) {
                log.error("Amount value is null or missing in JSON. Full node: {}", node);
                throw new IllegalArgumentException("Amount value cannot be null");
            }
            
            if (currencyCodeNode == null || currencyCodeNode.isNull() || currencyCodeNode.asText().isEmpty()) {
                log.error("Currency code is null, missing, or empty in JSON. Full node: {}", node);
                throw new IllegalArgumentException("Currency code cannot be null or empty");
            }
            
            BigDecimal value = valueNode.decimalValue();
            String currencyCode = currencyCodeNode.asText();
            
            log.info("Deserializing Amount: value={}, currencyCode={}", value, currencyCode);
            
            Amount amount = Amount.of(value, currencyCode);
            log.info("Created Amount object: {}", amount);
            log.info("=== AmountDeserializer.deserialize() SUCCESS ===");
            
            return amount;
        } catch (IllegalArgumentException e) {
            log.error("Validation error in AmountDeserializer.deserialize(): {}", e.getMessage());
            throw new IOException("Failed to deserialize Amount: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error in AmountDeserializer.deserialize(): {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in AmountDeserializer.deserialize(): {}", e.getMessage(), e);
            throw new IOException("Failed to deserialize Amount: " + e.getMessage(), e);
        }
    }
}

