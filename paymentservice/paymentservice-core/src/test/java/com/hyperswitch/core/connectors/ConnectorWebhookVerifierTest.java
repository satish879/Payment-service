package com.hyperswitch.core.connectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectorWebhookVerifier
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorWebhookVerifier Unit Tests")
class ConnectorWebhookVerifierTest {
    
    @InjectMocks
    private ConnectorWebhookVerifier webhookVerifier;
    
    private String testSecret;
    private String testPayload;
    
    @BeforeEach
    void setUp() {
        testSecret = "test_secret_key_12345";
        testPayload = "{\"id\":\"evt_123\",\"type\":\"payment.succeeded\"}";
    }
    
    @Test
    @DisplayName("Should verify Stripe webhook signature")
    void testVerifyStripeSignature() {
        // Given
        String signature = computeTestSignature(testPayload, testSecret);
        String stripeSignature = "t=" + (System.currentTimeMillis() / 1000) + ",v1=" + signature;
        
        // When
        boolean verified = webhookVerifier.verifyWebhookSignature(
            "stripe", testPayload, stripeSignature, testSecret);
        
        // Then
        assertThat(verified).isTrue();
    }
    
    @Test
    @DisplayName("Should verify PayPal webhook signature")
    void testVerifyPayPalSignature() {
        // Given
        String signature = computeTestSignature(testPayload, testSecret);
        
        // When
        boolean verified = webhookVerifier.verifyWebhookSignature(
            "paypal", testPayload, signature, testSecret);
        
        // Then
        assertThat(verified).isTrue();
    }
    
    @Test
    @DisplayName("Should reject invalid signature")
    void testRejectInvalidSignature() {
        // Given
        String invalidSignature = "invalid_signature";
        
        // When
        boolean verified = webhookVerifier.verifyWebhookSignature(
            "stripe", testPayload, invalidSignature, testSecret);
        
        // Then
        assertThat(verified).isFalse();
    }
    
    @Test
    @DisplayName("Should get webhook secret from credentials")
    void testGetWebhookSecret() {
        // Given
        Map<String, String> credentials = new HashMap<>();
        credentials.put("webhook_secret", testSecret);
        
        // When
        String secret = webhookVerifier.getWebhookSecret("stripe", credentials);
        
        // Then
        assertThat(secret).isEqualTo(testSecret);
    }
    
    /**
     * Compute test signature (simplified for testing)
     */
    private String computeTestSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = 
                new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

