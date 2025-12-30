package com.hyperswitch.core.test;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.types.AnalyticsDomain;

import java.time.Instant;
import java.util.*;

/**
 * Test data builders for creating test objects
 * Follows builder pattern for easy test data creation
 */
public class TestDataBuilders {
    
    private TestDataBuilders() {
        // Utility class
    }
    
    /**
     * Build a payment request for testing
     */
    public static PaymentsCreateIntentRequest paymentRequest() {
        PaymentsCreateIntentRequest request = new PaymentsCreateIntentRequest();
        AmountDetails amountDetails = new AmountDetails();
        amountDetails.setAmount(com.hyperswitch.common.types.Amount.of(
            java.math.BigDecimal.valueOf(1000L), "USD"));
        amountDetails.setCurrency("USD");
        request.setAmountDetails(amountDetails);
        request.setCustomerId("test_customer_" + UUID.randomUUID().toString());
        request.setDescription("Test payment");
        request.setReturnUrl("https://example.com/return");
        return request;
    }
    
    /**
     * Build a merchant connector account request for testing
     */
    public static MerchantConnectorAccountRequest connectorAccountRequest() {
        MerchantConnectorAccountRequest request = new MerchantConnectorAccountRequest();
        Map<String, Object> accountDetails = new HashMap<>();
        accountDetails.put("api_key", "test_api_key_" + UUID.randomUUID().toString());
        accountDetails.put("merchant_id", "test_merchant_id");
        
        request.setConnectorName("stripe");
        request.setConnectorType("payment_processor");
        request.setTestMode(true);
        request.setDisabled(false);
        request.setConnectorAccountDetails(accountDetails);
        return request;
    }
    
    /**
     * Build a connector session request for testing
     */
    public static ConnectorSessionRequest connectorSessionRequest() {
        ConnectorSessionRequest request = new ConnectorSessionRequest();
        request.setConnectorName("stripe");
        request.setPaymentId("pay_test_" + UUID.randomUUID().toString());
        return request;
    }
    
    /**
     * Build a refund request for testing
     */
    public static RefundsCreateRequest refundRequest(String paymentId) {
        RefundsCreateRequest request = new RefundsCreateRequest();
        request.setPaymentId(paymentId);
        request.setAmount(com.hyperswitch.common.types.Amount.of(
            java.math.BigDecimal.valueOf(1000L), "USD"));
        request.setReason("customer_request");
        return request;
    }
    
    /**
     * Build a user signup request for testing
     */
    public static SignUpRequest signupRequest() {
        SignUpRequest request = new SignUpRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");
        return request;
    }
    
    /**
     * Build a merchant account request for testing
     */
    public static MerchantAccountCreateRequest merchantAccountRequest() {
        MerchantAccountCreateRequest request = new MerchantAccountCreateRequest();
        request.setMerchantName("Test Merchant");
        request.setMerchantDetails(Map.of("description", "Test merchant account"));
        return request;
    }
    
    /**
     * Build an analytics metrics request for testing
     */
    public static PaymentMetricsRequest paymentMetricsRequest() {
        PaymentMetricsRequest request = new PaymentMetricsRequest();
        PaymentMetricsRequest.TimeRange timeRange = new PaymentMetricsRequest.TimeRange();
        timeRange.setStartTime(Instant.now().minusSeconds(3600).toString());
        timeRange.setEndTime(Instant.now().toString());
        request.setTimeRange(timeRange);
        return request;
    }
    
    /**
     * Build a connector payment request for testing
     */
    public static ConnectorPaymentRequest connectorPaymentRequest() {
        ConnectorPaymentRequest request = new ConnectorPaymentRequest();
        request.setConnectorName("stripe");
        request.setAmount(1000L);
        request.setCurrency("USD");
        Map<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put("type", "card");
        paymentMethod.put("card", Map.of(
            "number", "4242424242424242",
            "exp_month", 12,
            "exp_year", 2025,
            "cvc", "123"
        ));
        request.setPaymentMethod(paymentMethod);
        return request;
    }
    
    /**
     * Build a routing configuration request for testing
     */
    public static RoutingConfigRequest routingConfigRequest() {
        RoutingConfigRequest request = new RoutingConfigRequest();
        request.setName("test_algorithm_" + UUID.randomUUID().toString());
        request.setDescription("Priority-based routing");
        Map<String, Object> algorithm = new HashMap<>();
        algorithm.put("type", "priority_based");
        algorithm.put("connector_priority", List.of("stripe", "paypal"));
        request.setAlgorithm(algorithm);
        return request;
    }
    
    /**
     * Build a webhook verification request for testing
     */
    public static Map<String, String> webhookVerificationData(String connectorName, String payload, String signature) {
        Map<String, String> data = new HashMap<>();
        data.put("connector", connectorName);
        data.put("payload", payload);
        data.put("signature", signature);
        return data;
    }
}

