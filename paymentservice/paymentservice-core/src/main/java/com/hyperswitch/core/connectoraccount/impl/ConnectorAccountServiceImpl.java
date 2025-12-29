package com.hyperswitch.core.connectoraccount.impl;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.connectoraccount.ConnectorAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of ConnectorAccountService
 */
@Service
public class ConnectorAccountServiceImpl implements ConnectorAccountService {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectorAccountServiceImpl.class);
    
    @Override
    public Mono<Result<ConnectorAccountResponse, PaymentError>> createConnectorAccount(
            ConnectorAccountCreateRequest request) {
        log.info("Creating connector account: {}, type: {}", 
                request.getConnectorName(), request.getConnectorType());
        
        return Mono.fromCallable(() -> {
            String connectorAccountId = "mca_" + UUID.randomUUID().toString().replace("-", "");
            
            ConnectorAccountResponse response = new ConnectorAccountResponse();
            response.setId(connectorAccountId);
            response.setConnectorType(request.getConnectorType());
            response.setConnectorName(request.getConnectorName());
            response.setConnectorLabel(request.getConnectorLabel() != null 
                ? request.getConnectorLabel() 
                : request.getConnectorName() + "_default");
            response.setProfileId(request.getProfileId());
            response.setConnectorAccountDetails(request.getConnectorAccountDetails());
            response.setPaymentMethodsEnabled(request.getPaymentMethodsEnabled());
            response.setConnectorWebhookDetails(request.getConnectorWebhookDetails());
            response.setMetadata(request.getMetadata());
            response.setDisabled(request.getDisabled() != null ? request.getDisabled() : false);
            response.setStatus(request.getStatus() != null ? request.getStatus() : "active");
            response.setCreatedAt(Instant.now());
            response.setUpdatedAt(Instant.now());
            
            // In production, this would:
            // 1. Validate connector type and name
            // 2. Validate profile exists
            // 3. Encrypt connector account details
            // 4. Store connector account in database
            // 5. Initialize connector-specific configurations
            // 6. Return created connector account
            
            return Result.<ConnectorAccountResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error creating connector account: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_CREATE_FAILED",
                "Failed to create connector account: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorAccountResponse, PaymentError>> getConnectorAccount(String id) {
        log.info("Getting connector account: {}", id);
        
        return Mono.fromCallable(() -> {
            ConnectorAccountResponse response = new ConnectorAccountResponse();
            response.setId(id);
            response.setConnectorType("payment_processor");
            response.setConnectorName("stripe");
            response.setConnectorLabel("stripe_default");
            response.setDisabled(false);
            response.setStatus("active");
            response.setCreatedAt(Instant.now());
            response.setUpdatedAt(Instant.now());
            
            // In production, this would query connector account from database
            
            return Result.<ConnectorAccountResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error getting connector account: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_RETRIEVAL_FAILED",
                "Failed to get connector account: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorAccountResponse, PaymentError>> updateConnectorAccount(
            String id,
            ConnectorAccountUpdateRequest request) {
        log.info("Updating connector account: {}", id);
        
        return Mono.fromCallable(() -> {
            ConnectorAccountResponse response = new ConnectorAccountResponse();
            response.setId(id);
            response.setConnectorType(request.getConnectorType());
            response.setConnectorLabel(request.getConnectorLabel());
            response.setConnectorAccountDetails(request.getConnectorAccountDetails());
            response.setPaymentMethodsEnabled(request.getPaymentMethodsEnabled());
            response.setConnectorWebhookDetails(request.getConnectorWebhookDetails());
            response.setMetadata(request.getMetadata());
            response.setDisabled(request.getDisabled());
            response.setStatus(request.getStatus());
            response.setUpdatedAt(Instant.now());
            
            // In production, this would:
            // 1. Validate connector account exists
            // 2. Update connector account in database
            // 3. Update encrypted credentials if provided
            // 4. Return updated connector account
            
            return Result.<ConnectorAccountResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error updating connector account: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_UPDATE_FAILED",
                "Failed to update connector account: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorAccountDeleteResponse, PaymentError>> deleteConnectorAccount(
            String id) {
        log.info("Deleting connector account: {}", id);
        
        return Mono.fromCallable(() -> {
            ConnectorAccountDeleteResponse response = new ConnectorAccountDeleteResponse();
            response.setId(id);
            response.setDeleted(true);
            
            // In production, this would:
            // 1. Validate connector account exists
            // 2. Check for active payments/transactions
            // 3. Soft delete or hard delete based on configuration
            // 4. Clean up related resources
            
            return Result.<ConnectorAccountDeleteResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error deleting connector account: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_DELETE_FAILED",
                "Failed to delete connector account: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<Flux<ConnectorAccountResponse>, PaymentError>> listConnectorAccountsForProfile(
            String merchantId, 
            String profileId) {
        log.info("Listing connector accounts for profile: {}, merchant: {}", profileId, merchantId);
        
        return Mono.fromCallable(() -> {
            // In production, this would query connector accounts from database
            // filtered by merchantId and profileId
            ConnectorAccountResponse response = new ConnectorAccountResponse();
            response.setId("mca_sample");
            response.setMerchantId(merchantId);
            response.setProfileId(profileId);
            response.setConnectorType("payment_processor");
            response.setConnectorName("stripe");
            response.setConnectorLabel("stripe_default");
            response.setDisabled(false);
            response.setStatus("active");
            response.setCreatedAt(Instant.now());
            response.setUpdatedAt(Instant.now());
            
            Flux<ConnectorAccountResponse> accounts = Flux.just(response);
            return Result.<Flux<ConnectorAccountResponse>, PaymentError>ok(accounts);
        })
        .onErrorResume(error -> {
            log.error("Error listing connector accounts for profile: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_LIST_FAILED",
                "Failed to list connector accounts for profile: " + error.getMessage())));
        });
    }
}

