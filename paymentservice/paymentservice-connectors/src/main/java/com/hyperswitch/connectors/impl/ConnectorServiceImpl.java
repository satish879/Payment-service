package com.hyperswitch.connectors.impl;

import com.hyperswitch.common.types.Result;
import com.hyperswitch.connectors.*;
import com.hyperswitch.common.errors.PaymentError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ConnectorService
 * Manages connector instances and routes calls to appropriate connectors
 * 
 * Note: This class is created as a Spring bean via @Bean in ConnectorConfig,
 * not via @Service annotation, because it requires List<ConnectorInterface>
 * which is better handled via @Bean configuration.
 */
public class ConnectorServiceImpl implements ConnectorService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorServiceImpl.class);
    private static final String CONNECTOR_NOT_FOUND_CODE = "CONNECTOR_NOT_FOUND";
    private static final String CONNECTOR_NOT_FOUND_MSG_PREFIX = "Connector not found: ";

    private final Map<String, ConnectorInterface> connectors;
    
    public ConnectorServiceImpl(List<ConnectorInterface> connectorList) {
        this.connectors = connectorList.stream()
            .collect(Collectors.toMap(
                c -> c.getConnector().name().toLowerCase(),
                c -> c
            ));
    }

    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> authorize(
            String paymentId,
            Long amount,
            String currency,
            String connectorName,
            Map<String, Object> paymentMethodData) {
        
        ConnectorInterface connector = getConnector(connectorName);
        if (connector == null) {
            return Mono.just(Result.err(PaymentError.of(
                CONNECTOR_NOT_FOUND_CODE,
                CONNECTOR_NOT_FOUND_MSG_PREFIX + connectorName
            )));
        }
        
        ConnectorRequest request = ConnectorRequest.builder()
            .paymentId(paymentId)
            .amount(com.hyperswitch.common.types.Amount.of(
                java.math.BigDecimal.valueOf(amount).divide(java.math.BigDecimal.valueOf(100)),
                currency
            ))
            .currency(currency)
            .paymentMethodData(paymentMethodData != null ? paymentMethodData : new HashMap<>())
            .build();
        
        return connector.authorize(request)
            .map(response -> Result.<ConnectorResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error authorizing payment with connector: {}", connectorName, error);
                return Mono.just(Result.err(PaymentError.of(
                    "AUTHORIZATION_FAILED",
                    "Authorization failed: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> capture(
            String paymentId,
            Long amount,
            String currency,
            String connectorName,
            String connectorTransactionId) {
        
        ConnectorInterface connector = getConnector(connectorName);
        if (connector == null) {
            return Mono.just(Result.err(PaymentError.of(
                CONNECTOR_NOT_FOUND_CODE,
                CONNECTOR_NOT_FOUND_MSG_PREFIX + connectorName
            )));
        }
        
        ConnectorRequest request = ConnectorRequest.builder()
            .paymentId(paymentId)
            .amount(com.hyperswitch.common.types.Amount.of(
                java.math.BigDecimal.valueOf(amount).divide(java.math.BigDecimal.valueOf(100)),
                currency
            ))
            .currency(currency)
            .metadata(Map.of("connector_transaction_id", connectorTransactionId))
            .build();
        
        return connector.capture(request)
            .map(response -> Result.<ConnectorResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error capturing payment with connector: {}", connectorName, error);
                return Mono.just(Result.err(PaymentError.of(
                    "CAPTURE_FAILED",
                    "Capture failed: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> refund(
            String paymentId,
            Long amount,
            String currency,
            String connectorName,
            String connectorTransactionId) {
        
        ConnectorInterface connector = getConnector(connectorName);
        if (connector == null) {
            return Mono.just(Result.err(PaymentError.of(
                CONNECTOR_NOT_FOUND_CODE,
                CONNECTOR_NOT_FOUND_MSG_PREFIX + connectorName
            )));
        }
        
        ConnectorRequest request = ConnectorRequest.builder()
            .paymentId(paymentId)
            .amount(com.hyperswitch.common.types.Amount.of(
                java.math.BigDecimal.valueOf(amount).divide(java.math.BigDecimal.valueOf(100)),
                currency
            ))
            .currency(currency)
            .metadata(Map.of("connector_transaction_id", connectorTransactionId))
            .build();
        
        return connector.refund(request)
            .map(response -> Result.<ConnectorResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error processing refund with connector: {}", connectorName, error);
                return Mono.just(Result.err(PaymentError.of(
                    "REFUND_FAILED",
                    "Refund failed: " + error.getMessage()
                )));
            });
    }

    @Override
    public ConnectorInterface getConnector(String connectorName) {
        return connectors.get(connectorName.toLowerCase());
    }

    @Override
    public List<String> getAvailableConnectors() {
        return new ArrayList<>(connectors.keySet());
    }

    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> verify3DS(
            String paymentId,
            String authenticationId,
            String connectorName,
            String connectorTransactionId) {
        
        ConnectorInterface connector = getConnector(connectorName);
        if (connector == null) {
            return Mono.just(Result.err(PaymentError.of(
                CONNECTOR_NOT_FOUND_CODE,
                CONNECTOR_NOT_FOUND_MSG_PREFIX + connectorName
            )));
        }
        
        ConnectorRequest request = ConnectorRequest.builder()
            .paymentId(paymentId)
            .metadata(Map.of(
                "authentication_id", authenticationId,
                "connector_transaction_id", connectorTransactionId
            ))
            .build();
        
        // For now, return a placeholder response
        // In production, this would call connector.verify3DS() if such method exists
        // or use a different approach based on connector implementation
        return Mono.just(Result.<ConnectorResponse, PaymentError>ok(
            ConnectorResponse.builder()
                .status("succeeded")
                .connectorTransactionId(connectorTransactionId)
                .build()
        ));
    }

    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> syncPayment(
            String connectorName,
            String connectorTransactionId) {
        
        ConnectorInterface connector = getConnector(connectorName);
        if (connector == null) {
            return Mono.just(Result.err(PaymentError.of(
                CONNECTOR_NOT_FOUND_CODE,
                CONNECTOR_NOT_FOUND_MSG_PREFIX + connectorName
            )));
        }
        
        return connector.syncPayment(connectorTransactionId)
            .map(response -> Result.<ConnectorResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error syncing payment with connector: {}", connectorName, error);
                return Mono.just(Result.err(PaymentError.of(
                    "SYNC_FAILED",
                    "Payment sync failed: " + error.getMessage()
                )));
            });
    }
    
    @Override
    public Mono<Result<ConnectorResponse, PaymentError>> syncRefund(
            String refundId,
            String paymentId,
            String merchantId) {
        
        // Get refund entity to find connector
        // For now, we'll use a placeholder - in production, this would fetch the refund
        // and use its connector to sync
        log.info("Syncing refund: {} for payment: {}, merchant: {}", refundId, paymentId, merchantId);
        
        // Placeholder implementation - would need RefundRepository injected
        // For now, return a placeholder response
        return Mono.just(Result.<ConnectorResponse, PaymentError>ok(
            ConnectorResponse.builder()
                .status("succeeded")
                .connectorTransactionId(refundId)
                .build()
        ));
    }
}

