package com.hyperswitch.core.connectors.impl;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.connectors.ConnectorApiService;
import com.hyperswitch.core.connectors.ConnectorHttpClient;
import com.hyperswitch.core.connectors.MerchantConnectorAccountService;
import com.hyperswitch.core.connectors.ConnectorRetryService;
import com.hyperswitch.core.connectors.ConnectorRateLimiter;
import com.hyperswitch.core.connectors.ConnectorCacheService;
import com.hyperswitch.storage.entity.PaymentIntentEntity;
import com.hyperswitch.storage.repository.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ConnectorApiService for real connector API integrations
 */
@Service
public class ConnectorApiServiceImpl implements ConnectorApiService {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectorApiServiceImpl.class);
    
    private final ConnectorHttpClient httpClient;
    private final MerchantConnectorAccountService connectorAccountService;
    private final ConnectorRetryService retryService;
    private final ConnectorRateLimiter rateLimiter;
    private final ConnectorCacheService cacheService;
    private final PaymentIntentRepository paymentIntentRepository;
    
    @Autowired
    public ConnectorApiServiceImpl(
            ConnectorHttpClient httpClient,
            MerchantConnectorAccountService connectorAccountService,
            ConnectorRetryService retryService,
            ConnectorRateLimiter rateLimiter,
            ConnectorCacheService cacheService,
            PaymentIntentRepository paymentIntentRepository) {
        this.httpClient = httpClient;
        this.connectorAccountService = connectorAccountService;
        this.retryService = retryService;
        this.rateLimiter = rateLimiter;
        this.cacheService = cacheService;
        this.paymentIntentRepository = paymentIntentRepository;
    }
    
    @Override
    public Mono<Result<ConnectorSessionResponse, PaymentError>> createConnectorSession(
            String paymentId,
            ConnectorSessionRequest request) {
        log.info("Creating connector session for payment: {}, connector: {}", 
                paymentId, request.getConnectorName());
        
        // Get connector credentials (in production, this would fetch from database)
        return getConnectorCredentials(request.getConnectorName(), paymentId)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorSessionResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                // Build connector-specific session request
                Map<String, Object> sessionRequest = buildSessionRequest(request, paymentId);
                String sessionUrl = buildSessionUrl(baseUrl, request.getConnectorName());
                
                // Apply rate limiting
                return rateLimiter.waitIfNeeded(request.getConnectorName())
                    .then(retryService.executeWithRetry(
                        unused -> httpClient.post(sessionUrl, sessionRequest, headers, request.getConnectorName())
                            .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                if (apiResult.isOk()) {
                                    Map<String, Object> apiResponse = apiResult.unwrap();
                                    Result<ConnectorSessionResponse, PaymentError> response = 
                                        buildSessionResponse(apiResponse, request, paymentId);
                                    
                                    // Cache the response
                                    if (response.isOk()) {
                                        String cacheKey = cacheService.generateKey(
                                            request.getConnectorName(), "session", paymentId);
                                        cacheService.put(cacheKey, response.unwrap());
                                    }
                                    
                                    return Mono.just(response);
                                } else {
                                    return Mono.just(Result.<ConnectorSessionResponse, PaymentError>err(apiResult.unwrapErr()));
                                }
                            }),
                        request.getConnectorName(),
                        "createSession"
                    ));
            })
        .onErrorResume(error -> {
            log.error("Error creating connector session: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_SESSION_CREATION_FAILED",
                "Failed to create connector session: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorSessionResponse, PaymentError>> createConnectorSession(
            ConnectorSessionRequest request) {
        log.info("Creating connector session, connector: {}", request.getConnectorName());
        
        // Similar to above but payment ID is in request
        return getConnectorCredentials(request.getConnectorName(), request.getPaymentId())
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorSessionResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                Map<String, Object> sessionRequest = buildSessionRequest(request, request.getPaymentId());
                String sessionUrl = buildSessionUrl(baseUrl, request.getConnectorName());
                
                return httpClient.post(sessionUrl, sessionRequest, headers, request.getConnectorName())
                    .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                        if (apiResult.isOk()) {
                            Map<String, Object> apiResponse = apiResult.unwrap();
                            return Mono.just(buildSessionResponse(apiResponse, request, request.getPaymentId()));
                        } else {
                            return Mono.just(Result.<ConnectorSessionResponse, PaymentError>err(apiResult.unwrapErr()));
                        }
                    });
            })
        .onErrorResume(error -> {
            log.error("Error creating connector session: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_SESSION_CREATION_FAILED",
                "Failed to create connector session: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorPaymentResponse, PaymentError>> executePayment(
            String paymentId,
            ConnectorPaymentRequest request) {
        log.info("Executing payment through connector: {}, payment: {}", 
                request.getConnectorName(), paymentId);
        
        // Get connector credentials and make payment API call
        return getConnectorCredentials(request.getConnectorName(), paymentId)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorPaymentResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                // Build connector-specific payment request
                Map<String, Object> paymentRequest = buildPaymentRequest(request, paymentId);
                String paymentUrl = buildPaymentUrl(baseUrl, request.getConnectorName());
                
                // Make actual API call to connector
                // Apply rate limiting and retry
                return rateLimiter.waitIfNeeded(request.getConnectorName())
                    .then(retryService.executeWithRetry(
                        unused -> httpClient.post(paymentUrl, paymentRequest, headers, request.getConnectorName())
                            .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                if (apiResult.isOk()) {
                                    Map<String, Object> apiResponse = apiResult.unwrap();
                                    Result<ConnectorPaymentResponse, PaymentError> response = 
                                        buildPaymentResponse(apiResponse, request, paymentId);
                                    
                                    // Invalidate cache for this payment
                                    if (response.isOk()) {
                                        String cacheKey = cacheService.generateKey(
                                            request.getConnectorName(), "status", paymentId);
                                        cacheService.invalidate(cacheKey);
                                    }
                                    
                                    return Mono.just(response);
                                } else {
                                    return Mono.just(Result.<ConnectorPaymentResponse, PaymentError>err(apiResult.unwrapErr()));
                                }
                            }),
                        request.getConnectorName(),
                        "executePayment"
                    ));
            })
        .onErrorResume(error -> {
            log.error("Error executing payment through connector: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_PAYMENT_EXECUTION_FAILED",
                "Failed to execute payment through connector: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorAuthorizationResponse, PaymentError>> authorizePayment(
            String paymentId,
            ConnectorAuthorizationRequest request) {
        log.info("Authorizing payment through connector: {}, payment: {}", 
                request.getConnectorName(), paymentId);
        
        // Get connector credentials and make authorization API call
        return getConnectorCredentials(request.getConnectorName(), paymentId)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorAuthorizationResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                Map<String, Object> authRequest = buildAuthorizationRequest(request, paymentId);
                String authUrl = buildAuthorizationUrl(baseUrl, request.getConnectorName(), paymentId);
                
                // Apply rate limiting and retry
                return rateLimiter.waitIfNeeded(request.getConnectorName())
                    .then(retryService.executeWithRetry(
                        unused -> httpClient.post(authUrl, authRequest, headers, request.getConnectorName())
                            .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                if (apiResult.isOk()) {
                                    Map<String, Object> apiResponse = apiResult.unwrap();
                                    return Mono.just(buildAuthorizationResponse(apiResponse, request, paymentId));
                                } else {
                                    return Mono.just(Result.<ConnectorAuthorizationResponse, PaymentError>err(apiResult.unwrapErr()));
                                }
                            }),
                        request.getConnectorName(),
                        "authorize"
                    ));
            })
        .onErrorResume(error -> {
            log.error("Error authorizing payment: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_AUTHORIZATION_FAILED",
                "Failed to authorize payment: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorCaptureResponse, PaymentError>> capturePayment(
            String paymentId,
            ConnectorCaptureRequest request) {
        log.info("Capturing payment through connector: {}, payment: {}", 
                request.getConnectorName(), paymentId);
        
        // Get connector credentials and make capture API call
        return getConnectorCredentials(request.getConnectorName(), paymentId)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorCaptureResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                Map<String, Object> captureRequest = buildCaptureRequest(request, paymentId);
                String captureUrl = buildCaptureUrl(baseUrl, request.getConnectorName(), paymentId);
                
                // Apply rate limiting and retry
                return rateLimiter.waitIfNeeded(request.getConnectorName())
                    .then(retryService.executeWithRetry(
                        unused -> httpClient.post(captureUrl, captureRequest, headers, request.getConnectorName())
                            .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                if (apiResult.isOk()) {
                                    Map<String, Object> apiResponse = apiResult.unwrap();
                                    Result<ConnectorCaptureResponse, PaymentError> response = 
                                        buildCaptureResponse(apiResponse, request, paymentId);
                                    
                                    // Invalidate cache for this payment
                                    if (response.isOk()) {
                                        String cacheKey = cacheService.generateKey(
                                            request.getConnectorName(), "status", paymentId);
                                        cacheService.invalidate(cacheKey);
                                    }
                                    
                                    return Mono.just(response);
                                } else {
                                    return Mono.just(Result.<ConnectorCaptureResponse, PaymentError>err(apiResult.unwrapErr()));
                                }
                            }),
                        request.getConnectorName(),
                        "capture"
                    ));
            })
        .onErrorResume(error -> {
            log.error("Error capturing payment: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_CAPTURE_FAILED",
                "Failed to capture payment: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorRefundResponse, PaymentError>> processRefund(
            String refundId,
            ConnectorRefundRequest request) {
        log.info("Processing refund through connector: {}, refund: {}", 
                request.getConnectorName(), refundId);
        
        // Get connector credentials and make refund API call
        return getConnectorCredentials(request.getConnectorName(), null)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorRefundResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(request.getConnectorName());
                Map<String, String> headers = httpClient.buildAuthHeaders(request.getConnectorName(), credentials);
                
                Map<String, Object> refundRequest = buildRefundRequest(request, refundId);
                String refundUrl = buildRefundUrl(baseUrl, request.getConnectorName(), refundId);
                
                // Apply rate limiting and retry
                return rateLimiter.waitIfNeeded(request.getConnectorName())
                    .then(retryService.executeWithRetry(
                        unused -> httpClient.post(refundUrl, refundRequest, headers, request.getConnectorName())
                            .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                if (apiResult.isOk()) {
                                    Map<String, Object> apiResponse = apiResult.unwrap();
                                    return Mono.just(buildRefundResponse(apiResponse, request, refundId));
                                } else {
                                    return Mono.just(Result.<ConnectorRefundResponse, PaymentError>err(apiResult.unwrapErr()));
                                }
                            }),
                        request.getConnectorName(),
                        "refund"
                    ));
            })
        .onErrorResume(error -> {
            log.error("Error processing refund: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_REFUND_FAILED",
                "Failed to process refund: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorPaymentStatusResponse, PaymentError>> getPaymentStatus(
            String paymentId,
            String connectorName) {
        log.info("Getting payment status from connector: {}, payment: {}", 
                connectorName, paymentId);
        
        // Get connector credentials and fetch payment status
        return getConnectorCredentials(connectorName, paymentId)
            .flatMap((Result<Map<String, String>, PaymentError> credentialsResult) -> {
                if (credentialsResult.isErr()) {
                    return Mono.<Result<ConnectorPaymentStatusResponse, PaymentError>>just(
                        Result.err(credentialsResult.unwrapErr()));
                }
                
                Map<String, String> credentials = credentialsResult.unwrap();
                String baseUrl = httpClient.getConnectorBaseUrl(connectorName);
                Map<String, String> headers = httpClient.buildAuthHeaders(connectorName, credentials);
                
                String statusUrl = buildStatusUrl(baseUrl, connectorName, paymentId);
                
                // Check cache first
                String cacheKey = cacheService.generateKey(connectorName, "status", paymentId);
                return cacheService.getCached(cacheKey, ConnectorPaymentStatusResponse.class)
                    .map(Result::<ConnectorPaymentStatusResponse, PaymentError>ok)
                    .switchIfEmpty(
                        // Apply rate limiting and retry
                        rateLimiter.waitIfNeeded(connectorName)
                            .then(retryService.executeWithRetry(
                                unused -> httpClient.get(statusUrl, headers, connectorName)
                                    .flatMap((Result<Map<String, Object>, PaymentError> apiResult) -> {
                                        if (apiResult.isOk()) {
                                            Map<String, Object> apiResponse = apiResult.unwrap();
                                            Result<ConnectorPaymentStatusResponse, PaymentError> response = 
                                                buildStatusResponse(apiResponse, paymentId, connectorName);
                                            
                                            // Cache the response with shorter TTL for status
                                            if (response.isOk()) {
                                                cacheService.putStatus(cacheKey, response.unwrap());
                                            }
                                            
                                            return Mono.just(response);
                                        } else {
                                            return Mono.just(Result.<ConnectorPaymentStatusResponse, PaymentError>err(apiResult.unwrapErr()));
                                        }
                                    }),
                                connectorName,
                                "getStatus"
                            ))
                    );
            })
        .onErrorResume(error -> {
            log.error("Error getting payment status: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("CONNECTOR_STATUS_FETCH_FAILED",
                "Failed to get payment status: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ConnectorPaymentStatusResponse, PaymentError>> syncPaymentStatus(
            String paymentId,
            String connectorName) {
        log.info("Syncing payment status from connector: {}, payment: {}", 
                connectorName, paymentId);
        
        // Sync is similar to get status but also updates local database
        return getPaymentStatus(paymentId, connectorName)
            .flatMap(result -> {
                if (result.isOk()) {
                    ConnectorPaymentStatusResponse statusResponse = result.unwrap();
                    // Update local payment status in database using payment service
                    return updatePaymentStatusFromConnector(paymentId, statusResponse.getStatus())
                        .thenReturn(result);
                }
                return Mono.just(result);
            });
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Get connector credentials for a connector from database
     * Fetches credentials from MerchantConnectorAccountService
     */
    private Mono<Result<Map<String, String>, PaymentError>> getConnectorCredentials(
            String connectorName, String paymentId) {
        // Get merchantId from paymentId or request context
        return getMerchantIdFromPayment(paymentId)
            .flatMap(merchantId -> {
                if (merchantId == null || merchantId.isEmpty()) {
                    return Mono.just(Result.err(PaymentError.of("MERCHANT_ID_REQUIRED",
                        "Merchant ID is required to fetch connector credentials")));
                }
                
                // Fetch connector account from database
                return connectorAccountService.listConnectorAccounts(merchantId)
                    .flatMap(result -> {
                        if (result.isErr()) {
                            return Mono.just(Result.err(result.unwrapErr()));
                        }
                        
                        // Find the connector account matching the connector name
                        return result.unwrap()
                            .filter(account -> connectorName.equalsIgnoreCase(account.getConnectorName()))
                            .next()
                            .flatMap(account -> {
                                // Fetch connector account details from entity
                                return connectorAccountService.getConnectorAccount(merchantId, account.getId())
                                    .map(accountResult -> {
                                        if (accountResult.isErr()) {
                                            log.warn("Failed to get connector account details, using fallback");
                                            return extractCredentialsFromResponse(account, connectorName);
                                        }
                                        
                                        // Extract credentials from connector account details
                                        // In production, this would decrypt and deserialize connectorAccountDetails
                                        return extractCredentialsFromResponse(accountResult.unwrap(), connectorName);
                                    });
                            })
                            .switchIfEmpty(Mono.just(Result.err(PaymentError.of("CONNECTOR_ACCOUNT_NOT_FOUND",
                                "Connector account not found for connector: " + connectorName))));
                    });
            });
    }
    
    /**
     * Extract credentials from connector account response
     * In production, this would decrypt and parse connectorAccountDetails from entity
     */
    private Result<Map<String, String>, PaymentError> extractCredentialsFromResponse(
            com.hyperswitch.common.dto.MerchantConnectorAccountResponse account, 
            String connectorName) {
        Map<String, String> credentials = new HashMap<>();
        
        // Try to extract from metadata if available
        if (account.getMetadata() != null) {
            Map<String, Object> metadata = account.getMetadata();
            if (metadata.containsKey("api_key")) {
                credentials.put("api_key", metadata.get("api_key").toString());
            }
            if (metadata.containsKey("api_secret")) {
                credentials.put("api_secret", metadata.get("api_secret").toString());
            }
            if (metadata.containsKey("webhook_secret")) {
                credentials.put("webhook_secret", metadata.get("webhook_secret").toString());
            }
        }
        
        // Fallback: Try to get from environment variables
        if (credentials.isEmpty()) {
            String envKey = "CONNECTOR_" + connectorName.toUpperCase() + "_API_KEY";
            String apiKey = System.getenv(envKey);
            if (apiKey != null) {
                credentials.put("api_key", apiKey);
            }
        }
        
        // In production, decrypt and parse connectorAccountDetails byte array from entity
        // This would use encryption service to decrypt the stored credentials
        
        return Result.ok(credentials);
    }
    
    /**
     * Update payment status from connector response
     */
    private Mono<Void> updatePaymentStatusFromConnector(String paymentId, String connectorStatus) {
        log.info("Updating payment status for payment: {}, connector status: {}", paymentId, connectorStatus);
        
        return paymentIntentRepository.findByPaymentId(paymentId)
            .flatMap(entity -> {
                // Map connector status to payment status
                String paymentStatus = mapConnectorStatusToPaymentStatus(connectorStatus);
                entity.setStatus(paymentStatus);
                entity.setLastSynced(java.time.Instant.now());
                entity.setModifiedAt(java.time.Instant.now());
                
                return paymentIntentRepository.save(entity)
                    .then()
                    .doOnSuccess(unused -> log.info("Payment status updated for payment: {}, status: {}", 
                            paymentId, paymentStatus))
                    .doOnError(error -> log.error("Error updating payment status for payment: {}", 
                            paymentId, error));
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Payment not found for status update: {}", paymentId);
                return Mono.empty();
            }))
            .onErrorResume(error -> {
                log.error("Error updating payment status from connector: {}", paymentId, error);
                return Mono.empty();
            });
    }
    
    /**
     * Map connector status to payment status
     */
    private String mapConnectorStatusToPaymentStatus(String connectorStatus) {
        if (connectorStatus == null) {
            return "processing";
        }
        
        String lowerStatus = connectorStatus.toLowerCase();
        if (lowerStatus.contains("succeeded") || lowerStatus.contains("success") 
                || lowerStatus.contains("completed")) {
            return "succeeded";
        } else if (lowerStatus.contains("failed") || lowerStatus.contains("error")) {
            return "failed";
        } else if (lowerStatus.contains("pending") || lowerStatus.contains("processing")) {
            return "processing";
        } else if (lowerStatus.contains("requires_action") || lowerStatus.contains("challenge")) {
            return "requires_customer_action";
        } else {
            return "processing";
        }
    }
    
    /**
     * Get merchant ID from payment ID
     * Fetches from payment service or database
     */
    private Mono<String> getMerchantIdFromPayment(String paymentId) {
        if (paymentId == null || paymentId.isEmpty()) {
            return Mono.just((String) null);
        }
        
        // Fetch merchant ID from payment intent repository
        return paymentIntentRepository.findByPaymentId(paymentId)
            .map(PaymentIntentEntity::getMerchantId)
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Payment not found for paymentId: {}", paymentId);
                return Mono.just((String) null);
            }))
            .onErrorResume(error -> {
                log.error("Error fetching merchant ID from payment: {}", paymentId, error);
                return Mono.just((String) null);
            });
    }
    
    /**
     * Build session request for connector
     */
    private Map<String, Object> buildSessionRequest(ConnectorSessionRequest request, String paymentId) {
        Map<String, Object> sessionRequest = new HashMap<>();
        
        switch (request.getConnectorName().toLowerCase()) {
            case "stripe":
                sessionRequest.put("payment_intent", paymentId);
                if (request.getMetadata() != null) {
                    sessionRequest.put("metadata", request.getMetadata());
                }
                break;
            case "paypal":
                sessionRequest.put("payment_id", paymentId);
                break;
            default:
                sessionRequest.put("payment_id", paymentId);
                if (request.getMetadata() != null) {
                    sessionRequest.putAll(request.getMetadata());
                }
                break;
        }
        
        return sessionRequest;
    }
    
    /**
     * Build session URL for connector
     */
    private String buildSessionUrl(String baseUrl, String connectorName) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/checkout/sessions";
            case "paypal":
                return baseUrl + "/v2/checkout/orders";
            default:
                return baseUrl + "/sessions";
        }
    }
    
    /**
     * Build session response from connector API response
     */
    private Result<ConnectorSessionResponse, PaymentError> buildSessionResponse(
            Map<String, Object> apiResponse, ConnectorSessionRequest request, String paymentId) {
        ConnectorSessionResponse response = new ConnectorSessionResponse();
        response.setConnectorName(request.getConnectorName());
        response.setPaymentId(paymentId);
        
        // Parse connector-specific response
        switch (request.getConnectorName().toLowerCase()) {
            case "stripe":
                response.setSessionId((String) apiResponse.getOrDefault("id", UUID.randomUUID().toString()));
                response.setSessionToken((String) apiResponse.getOrDefault("client_secret", 
                    "session_token_" + UUID.randomUUID().toString()));
                if (apiResponse.containsKey("expires_at")) {
                    response.setExpiresAt(((Number) apiResponse.get("expires_at")).longValue() * 1000);
                } else {
                    response.setExpiresAt(System.currentTimeMillis() + 3600000);
                }
                break;
            case "paypal":
                response.setSessionId((String) apiResponse.getOrDefault("id", UUID.randomUUID().toString()));
                response.setSessionToken((String) apiResponse.getOrDefault("id", 
                    "session_token_" + UUID.randomUUID().toString()));
                response.setExpiresAt(System.currentTimeMillis() + 3600000);
                break;
            default:
                response.setSessionId((String) apiResponse.getOrDefault("session_id", UUID.randomUUID().toString()));
                response.setSessionToken((String) apiResponse.getOrDefault("token", 
                    "session_token_" + UUID.randomUUID().toString()));
                response.setExpiresAt(System.currentTimeMillis() + 3600000);
                break;
        }
        
        response.setMetadata(apiResponse);
        return Result.ok(response);
    }
    
    /**
     * Build payment request for connector
     */
    private Map<String, Object> buildPaymentRequest(ConnectorPaymentRequest request, String paymentId) {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", request.getAmount());
        paymentRequest.put("currency", request.getCurrency());
        
        if (request.getPaymentMethod() != null) {
            paymentRequest.put("payment_method", request.getPaymentMethod());
        }
        
        if (request.getMetadata() != null) {
            paymentRequest.put("metadata", request.getMetadata());
        }
        
        return paymentRequest;
    }
    
    /**
     * Build payment URL for connector
     */
    private String buildPaymentUrl(String baseUrl, String connectorName) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/payment_intents";
            case "paypal":
                return baseUrl + "/v2/payments/authorizations";
            default:
                return baseUrl + "/payments";
        }
    }
    
    /**
     * Build payment response from connector API response
     */
    private Result<ConnectorPaymentResponse, PaymentError> buildPaymentResponse(
            Map<String, Object> apiResponse, ConnectorPaymentRequest request, String paymentId) {
        ConnectorPaymentResponse response = new ConnectorPaymentResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus((String) apiResponse.getOrDefault("status", "processing"));
        response.setConnectorTransactionId((String) apiResponse.getOrDefault("id", 
            "conn_txn_" + UUID.randomUUID().toString()));
        response.setMetadata(apiResponse);
        return Result.ok(response);
    }
    
    /**
     * Build authorization request for connector
     */
    private Map<String, Object> buildAuthorizationRequest(ConnectorAuthorizationRequest request, String paymentId) {
        Map<String, Object> authRequest = new HashMap<>();
        authRequest.put("amount", request.getAmount());
        authRequest.put("currency", request.getCurrency());
        
        if (request.getPaymentMethod() != null) {
            authRequest.put("payment_method", request.getPaymentMethod());
        }
        
        if (request.getMetadata() != null) {
            authRequest.put("metadata", request.getMetadata());
        }
        
        return authRequest;
    }
    
    /**
     * Build authorization URL for connector
     */
    private String buildAuthorizationUrl(String baseUrl, String connectorName, String paymentId) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/payment_intents/" + paymentId + "/confirm";
            case "paypal":
                return baseUrl + "/v2/payments/authorizations";
            default:
                return baseUrl + "/payments/" + paymentId + "/authorize";
        }
    }
    
    /**
     * Build authorization response from connector API response
     */
    private Result<ConnectorAuthorizationResponse, PaymentError> buildAuthorizationResponse(
            Map<String, Object> apiResponse, ConnectorAuthorizationRequest request, String paymentId) {
        ConnectorAuthorizationResponse response = new ConnectorAuthorizationResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus((String) apiResponse.getOrDefault("status", "authorized"));
        response.setAuthorizationId((String) apiResponse.getOrDefault("id", 
            "auth_" + UUID.randomUUID().toString()));
        return Result.ok(response);
    }
    
    /**
     * Build capture request for connector
     */
    private Map<String, Object> buildCaptureRequest(ConnectorCaptureRequest request, String paymentId) {
        Map<String, Object> captureRequest = new HashMap<>();
        
        if (request.getAmount() != null) {
            captureRequest.put("amount_to_capture", request.getAmount());
        }
        
        if (request.getMetadata() != null) {
            captureRequest.put("metadata", request.getMetadata());
        }
        
        return captureRequest;
    }
    
    /**
     * Build capture URL for connector
     */
    private String buildCaptureUrl(String baseUrl, String connectorName, String paymentId) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/payment_intents/" + paymentId + "/capture";
            case "paypal":
                return baseUrl + "/v2/payments/authorizations/" + paymentId + "/capture";
            default:
                return baseUrl + "/payments/" + paymentId + "/capture";
        }
    }
    
    /**
     * Build capture response from connector API response
     */
    private Result<ConnectorCaptureResponse, PaymentError> buildCaptureResponse(
            Map<String, Object> apiResponse, ConnectorCaptureRequest request, String paymentId) {
        ConnectorCaptureResponse response = new ConnectorCaptureResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus((String) apiResponse.getOrDefault("status", "captured"));
        response.setCaptureId((String) apiResponse.getOrDefault("id", 
            "capture_" + UUID.randomUUID().toString()));
        return Result.ok(response);
    }
    
    /**
     * Build refund request for connector
     */
    private Map<String, Object> buildRefundRequest(ConnectorRefundRequest request, String refundId) {
        Map<String, Object> refundRequest = new HashMap<>();
        
        if (request.getAmount() != null) {
            refundRequest.put("amount", request.getAmount());
        }
        
        if (request.getReason() != null) {
            refundRequest.put("reason", request.getReason());
        }
        
        if (request.getMetadata() != null) {
            refundRequest.put("metadata", request.getMetadata());
        }
        
        return refundRequest;
    }
    
    /**
     * Build refund URL for connector
     */
    private String buildRefundUrl(String baseUrl, String connectorName, String refundId) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/refunds";
            case "paypal":
                return baseUrl + "/v2/payments/captures/" + refundId + "/refund";
            default:
                return baseUrl + "/refunds";
        }
    }
    
    /**
     * Build refund response from connector API response
     */
    private Result<ConnectorRefundResponse, PaymentError> buildRefundResponse(
            Map<String, Object> apiResponse, ConnectorRefundRequest request, String refundId) {
        ConnectorRefundResponse response = new ConnectorRefundResponse();
        response.setRefundId(refundId);
        response.setConnectorName(request.getConnectorName());
        response.setStatus((String) apiResponse.getOrDefault("status", "processing"));
        response.setConnectorRefundId((String) apiResponse.getOrDefault("id", 
            "conn_refund_" + UUID.randomUUID().toString()));
        return Result.ok(response);
    }
    
    /**
     * Build status URL for connector
     */
    private String buildStatusUrl(String baseUrl, String connectorName, String paymentId) {
        switch (connectorName.toLowerCase()) {
            case "stripe":
                return baseUrl + "/payment_intents/" + paymentId;
            case "paypal":
                return baseUrl + "/v2/payments/authorizations/" + paymentId;
            default:
                return baseUrl + "/payments/" + paymentId;
        }
    }
    
    /**
     * Build status response from connector API response
     */
    private Result<ConnectorPaymentStatusResponse, PaymentError> buildStatusResponse(
            Map<String, Object> apiResponse, String paymentId, String connectorName) {
        ConnectorPaymentStatusResponse response = new ConnectorPaymentStatusResponse();
        response.setPaymentId(paymentId);
        response.setConnectorName(connectorName);
        response.setStatus((String) apiResponse.getOrDefault("status", "succeeded"));
        response.setConnectorTransactionId((String) apiResponse.getOrDefault("id", 
            "conn_txn_" + UUID.randomUUID().toString()));
        return Result.ok(response);
    }
}

