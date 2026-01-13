package com.hyperswitch.core.payments.impl;

import com.hyperswitch.common.enums.PaymentStatus;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.payments.*;
import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.dto.UpdateRefundRequest;
import com.hyperswitch.common.dto.RefundAggregatesResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.routing.RoutingService;
import com.hyperswitch.routing.RoutingAlgorithm;
import com.hyperswitch.connectors.ConnectorService;
import com.hyperswitch.connectors.ConnectorResponse;
import com.hyperswitch.common.analytics.AnalyticsService;
import com.hyperswitch.common.enums.Connector;
import com.hyperswitch.common.enums.PaymentMethod;
import com.hyperswitch.storage.entity.PaymentIntentEntity;
import com.hyperswitch.storage.entity.PaymentAttemptEntity;
import com.hyperswitch.storage.entity.RefundEntity;
import com.hyperswitch.storage.entity.RoutingDecisionLogEntity;
import com.hyperswitch.storage.repository.PaymentIntentRepository;
import com.hyperswitch.storage.repository.PaymentAttemptRepository;
import com.hyperswitch.storage.repository.RefundRepository;
import com.hyperswitch.storage.repository.RoutingDecisionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of PaymentService
 * Handles all payment operations following Hyperswitch patterns
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String PAYMENT_NOT_FOUND_MSG = "Payment not found";
    private static final int CURRENCY_MULTIPLIER = 100;
    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_PENDING = "pending";

    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRepository refundRepository;
    private final RoutingService routingService;
    private final ConnectorService connectorService;
    private final PaymentMapper paymentMapper;
    private final com.hyperswitch.core.mandates.MandateService mandateService;
    private final com.hyperswitch.core.metrics.PaymentMetrics paymentMetrics;
    private AnalyticsService analyticsService; // Made optional - no implementation available
    private final RoutingDecisionLogRepository routingDecisionLogRepository;
    private final TransactionalOperator transactionalOperator;

    @Autowired
    public PaymentServiceImpl(
            PaymentIntentRepository paymentIntentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            RefundRepository refundRepository,
            RoutingService routingService,
            ConnectorService connectorService,
            PaymentMapper paymentMapper,
            com.hyperswitch.core.mandates.MandateService mandateService,
            com.hyperswitch.core.metrics.PaymentMetrics paymentMetrics,
            RoutingDecisionLogRepository routingDecisionLogRepository,
            TransactionalOperator transactionalOperator) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.refundRepository = refundRepository;
        this.routingService = routingService;
        this.connectorService = connectorService;
        this.paymentMapper = paymentMapper;
        this.mandateService = mandateService;
        this.paymentMetrics = paymentMetrics;
        this.routingDecisionLogRepository = routingDecisionLogRepository;
        this.transactionalOperator = transactionalOperator;
        log.info("TransactionalOperator injected for reactive transaction management");
    }

    /**
     * Optional setter for AnalyticsService - will be null if no implementation is available
     */
    @Autowired(required = false)
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        if (analyticsService == null) {
            log.warn("AnalyticsService not available - analytics recording will be skipped");
        } else {
            log.info("AnalyticsService injected successfully");
        }
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for merchant: {}", request.getMerchantId());
        long startTime = System.currentTimeMillis();
        
        // Validate amount is not null
        if (request.getAmount() == null) {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "INVALID_REQUEST",
                "Amount is required"
            )));
        }
        
        // Generate payment ID and create entity (avoiding Mono.fromCallable to preserve transaction context)
            PaymentId paymentId = PaymentId.generate();
                String clientSecret = generateClientSecretForPayment(paymentId.getValue());
        
        // Prepare metadata - include paymentMethod if provided
        Map<String, Object> metadata = request.getMetadata() != null 
            ? new HashMap<>(request.getMetadata()) 
            : new HashMap<>();
        
        // Store paymentMethod in metadata so it can be retrieved later
        if (request.getPaymentMethod() != null) {
            metadata.put("payment_method", request.getPaymentMethod().name());
        }
                
                // Create payment intent entity
        PaymentIntentEntity entity = PaymentIntentEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .paymentId(paymentId.getValue())
                    .merchantId(request.getMerchantId())
                    .status(PaymentStatus.REQUIRES_CONFIRMATION.name())
                    .amount(convertToMinorUnits(request.getAmount()))
                    .currency(request.getAmount().getCurrencyCode())
                    .amountCaptured(0L)
                    .customerId(request.getCustomerId())
                    .description(request.getDescription())
                    .returnUrl(request.getReturnUrl())
            .metadata(metadata)
                    .attemptCount(0)
                    .createdAt(Instant.now())
                    .modifiedAt(Instant.now())
                    .offSession(request.getOffSession() != null ? request.getOffSession() : Boolean.FALSE)
                    .setupFutureUsage(request.getPaymentType() != null && "setup_mandate".equals(request.getPaymentType()) 
                        ? "off_session" : null)
                    .clientSecret(clientSecret)
                    .build();
        
        log.info("Saving payment intent entity: paymentId={}, merchantId={}", 
            entity.getPaymentId(), entity.getMerchantId());
        
        // Build the entire reactive chain
        Mono<Result<PaymentIntent, PaymentError>> paymentOperation = paymentIntentRepository.save(entity)
            .doOnNext(saved -> {
                log.info("Payment intent saved successfully: paymentId={}, id={}", 
                    saved.getPaymentId(), saved.getId());
            })
            .doOnError(error -> {
                log.error("Error saving payment intent: paymentId={}, error={}", 
                    entity.getPaymentId(), error.getMessage(), error);
            })
            .doOnSuccess(saved -> {
                log.info("Save operation completed successfully - transaction should be committed: paymentId={}, id={}", 
                    saved.getPaymentId(), saved.getId());
            })
            .doOnTerminate(() -> {
                log.info("Save reactive chain terminated for paymentId={}", entity.getPaymentId());
            })
            .flatMap(saved -> {
                log.info("Mapping saved payment intent to PaymentIntent: paymentId={}", saved.getPaymentId());
                try {
            PaymentIntent paymentIntent = paymentMapper.toPaymentIntent(saved);
            paymentMetrics.incrementPaymentCreated();
            paymentMetrics.recordPaymentProcessingTime(
                System.currentTimeMillis() - startTime, 
                java.util.concurrent.TimeUnit.MILLISECONDS
            );
                    log.info("Payment mapping completed successfully: paymentId={}", saved.getPaymentId());
                    return Mono.just(Result.<PaymentIntent, PaymentError>ok(paymentIntent));
                } catch (Exception e) {
                    log.error("Error mapping payment intent: paymentId={}, error={}", saved.getPaymentId(), e.getMessage(), e);
                    // Don't return error here - let the transaction commit, but return error result
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "PAYMENT_MAPPING_FAILED",
                        "Failed to map payment intent: " + e.getMessage()
                    )));
                }
            })
            .doOnSuccess(result -> {
                log.info("Payment creation reactive chain completed successfully: paymentId={}, resultOk={}", 
                    entity.getPaymentId(), result.isOk());
            })
            .doOnTerminate(() -> {
                log.info("Payment creation reactive chain terminated: paymentId={}", entity.getPaymentId());
        })
        .onErrorResume(error -> {
                log.error("Error creating payment: paymentId={}, error={}", entity.getPaymentId(), error.getMessage(), error);
                // This will cause transaction rollback, but return error result
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_CREATE_FAILED",
                "Failed to create payment: " + error.getMessage()
            )));
        });
        
        // Wrap the entire operation with TransactionalOperator to ensure transaction commits
        // Use .as(transactionalOperator::transactional) which is the recommended approach for Mono
        return paymentOperation
            .as(transactionalOperator::transactional)
            .doOnSubscribe(subscription -> {
                log.info("Transaction started for paymentId={}", entity.getPaymentId());
            })
            .doOnSuccess(result -> {
                log.info("Transaction committed successfully for paymentId={}, resultOk={}", 
                    entity.getPaymentId(), result.isOk());
            })
            .doOnError(error -> {
                log.error("Transaction failed for paymentId={}, error={}", entity.getPaymentId(), error.getMessage(), error);
            });
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> confirmPayment(
            PaymentId paymentId, 
            ConfirmPaymentRequest request) {
        log.info("Confirming payment: {}", paymentId);
        log.info("Looking up payment with paymentId: {}", paymentId.getValue());
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .doOnNext(intent -> {
                log.info("Found payment intent: paymentId={}, id={}, status={}, merchantId={}", 
                    intent.getPaymentId(), intent.getId(), intent.getStatus(), intent.getMerchantId());
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Payment not found in database: paymentId={}", paymentId.getValue());
                // Try to find by merchant ID as well for debugging
                return paymentIntentRepository.findAll()
                    .take(10)
                    .collectList()
                    .doOnNext(allPayments -> {
                        log.warn("Sample of payments in database (first 10): count={}", allPayments.size());
                        if (allPayments.isEmpty()) {
                            log.warn("Database appears to be empty - no payments found");
                        } else {
                            allPayments.forEach(p -> {
                                log.warn("  - paymentId={}, id={}, merchantId={}, status={}, createdAt={}", 
                                    p.getPaymentId(), p.getId(), p.getMerchantId(), p.getStatus(), p.getCreatedAt());
                            });
                        }
                    })
                    .then(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue())));
            }))
            .flatMap(intent -> validateAndProcessConfirmation(intent, request))
            .onErrorResume(error -> {
                log.error("Error confirming payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "PAYMENT_CONFIRM_FAILED",
                    "Failed to confirm payment: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Validate and process payment confirmation
     */
    private Mono<Result<PaymentIntent, PaymentError>> validateAndProcessConfirmation(
            PaymentIntentEntity intent,
            ConfirmPaymentRequest request) {
        // Validate payment can be confirmed
        if (!canConfirmPayment(intent.getStatus())) {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "INVALID_STATUS",
                "Payment cannot be confirmed in current status: " + intent.getStatus()
            )));
        }
        
        // Handle MIT payments with recurring_details
        if (Boolean.TRUE.equals(request.getOffSession()) && request.getRecurringDetails() != null) {
            return handleMitPayment(intent, request);
        }
        
        // Update off_session flag if provided
        if (request.getOffSession() != null) {
            intent.setOffSession(request.getOffSession());
        }
        
        return processRegularPaymentConfirmation(intent, request);
    }
    
    /**
     * Process regular payment confirmation (non-MIT)
     */
    private Mono<Result<PaymentIntent, PaymentError>> processRegularPaymentConfirmation(
            PaymentIntentEntity intent,
            ConfirmPaymentRequest request) {
        CreatePaymentRequest routingRequest = buildRoutingRequest(intent, request);
        
        return routingService.selectConnectors(routingRequest, intent.getMerchantId())
            .flatMap(connectors -> {
                if (connectors.isEmpty()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "NO_CONNECTOR_AVAILABLE",
                        "No suitable connector found for payment"
                    )));
                }
                
                String connectorName = connectors.get(0).name();
                return createAndProcessPaymentAttempt(intent, request, connectorName);
            });
    }
    
    /**
     * Build routing request from payment intent
     */
    private CreatePaymentRequest buildRoutingRequest(
            PaymentIntentEntity intent,
            ConfirmPaymentRequest request) {
        return CreatePaymentRequest.builder()
            .amount(Amount.of(
                java.math.BigDecimal.valueOf(intent.getAmount()).divide(
                    java.math.BigDecimal.valueOf(100)
                ),
                intent.getCurrency()
            ))
            .merchantId(intent.getMerchantId())
            .paymentMethod(request.getPaymentMethod())
            .build();
    }
    
    /**
     * Create payment attempt and process payment
     */
    private Mono<Result<PaymentIntent, PaymentError>> createAndProcessPaymentAttempt(
            PaymentIntentEntity intent,
            ConfirmPaymentRequest request,
            String connectorName) {
        PaymentAttemptEntity attempt = createPaymentAttempt(intent, connectorName);
        
        @SuppressWarnings("null")
        PaymentAttemptEntity attemptToSave = attempt;
        return paymentAttemptRepository.save(attemptToSave)
            .flatMap(savedAttempt -> {
                logRoutingDecisionForAttempt(intent, savedAttempt, connectorName, request);
                return updateIntentAndProcessPayment(intent, savedAttempt, request, connectorName);
            });
    }
    
    /**
     * Create payment attempt entity
     */
    private PaymentAttemptEntity createPaymentAttempt(
            PaymentIntentEntity intent,
            String connectorName) {
        return PaymentAttemptEntity.builder()
            .id(UUID.randomUUID().toString())
            .paymentId(intent.getPaymentId())
            .merchantId(intent.getMerchantId())
            .status(STATUS_PROCESSING)
            .connector(connectorName)
            .createdAt(Instant.now())
            .modifiedAt(Instant.now())
            .profileId(intent.getProfileId())
            .organizationId(intent.getOrganizationId())
            .build();
    }
    
    /**
     * Log routing decision for payment attempt
     */
    private void logRoutingDecisionForAttempt(
            PaymentIntentEntity intent,
            PaymentAttemptEntity attempt,
            String connectorName,
            ConfirmPaymentRequest request) {
        RoutingAlgorithm algorithm = routingService.getAlgorithm();
        PaymentMethod pm = request.getPaymentMethod();
        logRoutingDecision(
            intent.getPaymentId(),
            attempt.getId(),
            intent.getMerchantId(),
            intent.getProfileId(),
            connectorName,
            algorithm != null ? algorithm.name() : "UNKNOWN",
            intent.getAmount(),
            intent.getCurrency(),
            pm != null ? pm.name() : "UNKNOWN"
        ).subscribe(
            null,
            error -> log.warn("Failed to log routing decision", error)
        );
    }
    
    /**
     * Update payment intent and process payment with connector
     */
    private Mono<Result<PaymentIntent, PaymentError>> updateIntentAndProcessPayment(
            PaymentIntentEntity intent,
            PaymentAttemptEntity savedAttempt,
            ConfirmPaymentRequest request,
            String connectorName) {
        intent.setStatus(PaymentStatus.PROCESSING.name());
        intent.setActiveAttemptId(savedAttempt.getId());
        intent.setAttemptCount(intent.getAttemptCount() + 1);
        intent.setModifiedAt(Instant.now());
        
        return paymentIntentRepository.save(intent)
            .flatMap(updatedIntent -> processPaymentWithConnector(
                updatedIntent, 
                savedAttempt, 
                request,
                connectorName
            ));
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> capturePayment(
            PaymentId paymentId, 
            CapturePaymentRequest request) {
        log.info("Capturing payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> 
                // Validate payment status
                validateCaptureStatus(intent)
                    .switchIfEmpty(Mono.defer(() -> {
                        // Calculate and validate capture amount
                        Mono<Result<Long, PaymentError>> amountValidation = calculateCaptureAmount(intent, request);
                        return amountValidation.flatMap(amountResult -> {
                            if (amountResult.isErr()) {
                                return Mono.just(Result.<PaymentIntent, PaymentError>err(amountResult.unwrapErr()));
                            }
                            Long amountToCapture = amountResult.unwrap();
                            
                            // Get active attempt and process capture
                            @SuppressWarnings("null")
                            String activeAttemptId = intent.getActiveAttemptId();
                            return paymentAttemptRepository.findById(activeAttemptId)
                                .flatMap(attempt -> connectorService.capture(
                                        intent.getPaymentId(),
                                        amountToCapture,
                                        intent.getCurrency(),
                                        attempt.getConnector(),
                                        attempt.getConnectorTransactionId()
                                    )
                                    .flatMap(captureResult -> processCaptureResult(captureResult, intent, amountToCapture)));
                        });
                    }))
            )
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error capturing payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "PAYMENT_CAPTURE_FAILED",
                    "Failed to capture payment: " + error.getMessage()
                )));
            });
    }

    private Mono<Result<PaymentIntent, PaymentError>> validateCaptureStatus(PaymentIntentEntity intent) {
        String status = intent.getStatus();
        String requiresCapture = PaymentStatus.REQUIRES_CAPTURE.name();
        String partiallyCaptured = PaymentStatus.PARTIALLY_CAPTURED.name();
        if (!requiresCapture.equals(status) && !partiallyCaptured.equals(status)) {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "INVALID_STATUS",
                "Payment cannot be captured in current status: " + status
            )));
        }
        return Mono.empty();
    }

    private Mono<Result<Long, PaymentError>> calculateCaptureAmount(
            PaymentIntentEntity intent, 
            CapturePaymentRequest request) {
        Long amountToCapture;
        if (request.getAmountToCapture() != null) {
            amountToCapture = convertToMinorUnits(request.getAmountToCapture());
            // Validate partial capture amount
            Long captured = intent.getAmountCaptured() != null ? intent.getAmountCaptured() : 0L;
            Long remaining = intent.getAmount() - captured;
            if (amountToCapture > remaining) {
                return Mono.just(Result.<Long, PaymentError>err(PaymentError.of(
                    "INVALID_AMOUNT",
                    "Capture amount exceeds remaining amount. Remaining: " + remaining
                )));
            }
            if (amountToCapture <= 0) {
                return Mono.just(Result.<Long, PaymentError>err(PaymentError.of(
                    "INVALID_AMOUNT",
                    "Capture amount must be greater than zero"
                )));
            }
        } else {
            Long captured = intent.getAmountCaptured() != null ? intent.getAmountCaptured() : 0L;
            amountToCapture = intent.getAmount() - captured;
        }
        return Mono.just(Result.<Long, PaymentError>ok(amountToCapture));
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> getPayment(PaymentId paymentId) {
        log.info("Getting payment: paymentId={}", paymentId.getValue());
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .doOnNext(intent -> {
                log.info("Found payment: paymentId={}, id={}, status={}, merchantId={}", 
                    intent.getPaymentId(), intent.getId(), intent.getStatus(), intent.getMerchantId());
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Payment not found: paymentId={}", paymentId.getValue());
                return paymentIntentRepository.findAll()
                    .take(5)
                    .collectList()
                    .doOnNext(allPayments -> {
                        log.warn("Sample payments in DB (first 5): count={}", allPayments.size());
                        allPayments.forEach(p -> {
                            log.warn("  - paymentId={}, merchantId={}, status={}", 
                                p.getPaymentId(), p.getMerchantId(), p.getStatus());
                        });
                    })
                    .then(Mono.empty());
            }))
            .map(intent -> Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(intent)))
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error getting payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "PAYMENT_GET_FAILED",
                    "Failed to get payment: " + error.getMessage()
                )));
            });
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Result<Refund, PaymentError>> refundPayment(
            PaymentId paymentId, 
            RefundRequest request) {
        log.info("Processing refund for payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(intent -> {
                Long refundAmount = request.getAmount() != null
                    ? convertToMinorUnits(request.getAmount())
                    : intent.getAmountCaptured();
                
                // Get active attempt
                @SuppressWarnings("null")
                String activeAttemptIdForRefund = intent.getActiveAttemptId();
                return paymentAttemptRepository.findById(activeAttemptIdForRefund)
                    .flatMap(attempt -> {
                        // Create refund entity
                        RefundEntity refundEntity = RefundEntity.builder()
                            .id(UUID.randomUUID().toString())
                            .refundId(UUID.randomUUID().toString())
                            .paymentId(intent.getPaymentId())
                            .merchantId(intent.getMerchantId())
                            .connectorTransactionId(attempt.getConnectorTransactionId())
                            .connector(attempt.getConnector())
                            .refundType("instant")
                            .totalAmount(intent.getAmountCaptured())
                            .currency(intent.getCurrency())
                            .refundAmount(refundAmount)
                            .refundStatus(STATUS_PENDING)
                            .sentToGateway(false)
                            .refundReason(request.getReason())
                            .attemptId(attempt.getId())
                            .createdAt(Instant.now())
                            .modifiedAt(Instant.now())
                            .profileId(intent.getProfileId())
                            .organizationId(intent.getOrganizationId())
                            .build();
                        
                        @SuppressWarnings("null")
                        RefundEntity savedRefundEntity = refundEntity;
                        return refundRepository.save(savedRefundEntity)
                            .flatMap(savedRefund -> connectorService.refund(
                                    intent.getPaymentId(),
                                    refundAmount,
                                    intent.getCurrency(),
                                    attempt.getConnector(),
                                    attempt.getConnectorTransactionId()
                            )
                            .flatMap(refundResult -> {
                                    if (refundResult.isOk()) {
                                        savedRefund.setRefundStatus(STATUS_SUCCEEDED);
                                        savedRefund.setSentToGateway(true);
                                        savedRefund.setConnectorRefundId(refundResult.unwrap().getConnectorTransactionId());
                                        savedRefund.setModifiedAt(Instant.now());
                                        
                                        return refundRepository.save(savedRefund)
                                            .map(entity -> Result.<Refund, PaymentError>ok(paymentMapper.toRefund(entity)));
                                    } else {
                                        savedRefund.setRefundStatus(STATUS_FAILED);
                                        savedRefund.setRefundErrorMessage(refundResult.unwrapErr().getMessage());
                                        savedRefund.setModifiedAt(Instant.now());
                                        
                                        return refundRepository.save(savedRefund)
                                            .then(Mono.just(Result.<Refund, PaymentError>err(refundResult.unwrapErr())));
                                    }
                            }));
                    });
            })
            .onErrorResume(error -> {
                log.error("Error processing refund: {}", paymentId, error);
                return Mono.just(Result.<Refund, PaymentError>err(PaymentError.of(
                    "REFUND_FAILED",
                    "Failed to process refund: " + error.getMessage()
                )));
            });
    }

    private Mono<Result<PaymentIntent, PaymentError>> processPaymentWithConnector(
            PaymentIntentEntity intent,
            PaymentAttemptEntity attempt,
            ConfirmPaymentRequest request,
            String connectorName) {
        
        return connectorService.authorize(
            intent.getPaymentId(),
            intent.getAmount(),
            intent.getCurrency(),
            connectorName,
            request.getPaymentMethodData() != null ? request.getPaymentMethodData() : new java.util.HashMap<>()
        )
        .flatMap(authResult -> {
            if (authResult.isOk()) {
                ConnectorResponse response = authResult.unwrap();
                
                // Update attempt
                attempt.setConnectorTransactionId(response.getConnectorTransactionId());
                attempt.setConnectorMetadata(response.getAdditionalData());
                
                // Determine payment status based on connector response
                String paymentStatus = determinePaymentStatus(response);
                attempt.setStatus(paymentStatus);
                attempt.setModifiedAt(Instant.now());
                
                return paymentAttemptRepository.save(attempt)
                    .flatMap(savedAttempt -> {
                        // Update intent status
                        intent.setStatus(paymentStatus);
                        intent.setModifiedAt(Instant.now());
                        
                        return paymentIntentRepository.save(intent)
                            .flatMap(saved -> {
                                // Record payment attempt for success rate analytics
                                recordPaymentAttemptForAnalytics(saved, attempt, true)
                                    .subscribe(
                                        null,
                                        error -> log.warn("Failed to record payment attempt for analytics", error)
                                    );
                                
                                // Update routing decision log with success status
                                updateRoutingDecisionLog(saved.getPaymentId(), attempt.getId(), true)
                                    .subscribe(
                                        null,
                                        error -> log.warn("Failed to update routing decision log", error)
                                    );
                                
                                // Create mandate if this is a setup_mandate payment or off_session payment
                                if (shouldCreateMandate(saved, request)) {
                                    return createMandateFromPayment(saved, attempt, request)
                                        .then(Mono.just(Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(saved))));
                                }
                                return Mono.just(Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(saved)));
                            });
                    });
            } else {
                // Payment failed
                attempt.setStatus(STATUS_FAILED);
                attempt.setErrorMessage(authResult.unwrapErr().getMessage());
                attempt.setErrorCode(authResult.unwrapErr().getCode());
                attempt.setModifiedAt(Instant.now());
                
                return paymentAttemptRepository.save(attempt)
                    .flatMap(savedAttempt -> {
                        intent.setStatus(PaymentStatus.FAILED.name());
                        intent.setModifiedAt(Instant.now());
                        
                        return paymentIntentRepository.save(intent)
                            .flatMap(saved -> {
                                // Record payment attempt for success rate analytics
                                recordPaymentAttemptForAnalytics(saved, savedAttempt, false)
                                    .subscribe(
                                        null,
                                        error -> log.warn("Failed to record payment attempt for analytics", error)
                                    );
                                
                                // Update routing decision log with failure status
                                updateRoutingDecisionLog(saved.getPaymentId(), savedAttempt.getId(), false)
                                    .subscribe(
                                        null,
                                        error -> log.warn("Failed to update routing decision log", error)
                                    );
                                
                                return Mono.just(Result.<PaymentIntent, PaymentError>err(authResult.unwrapErr()));
                            });
                    });
            }
        });
    }

    /**
     * Handle MIT (Merchant-Initiated Transaction) payment with recurring_details
     */
    private Mono<Result<PaymentIntent, PaymentError>> handleMitPayment(
            PaymentIntentEntity intent,
            ConfirmPaymentRequest request) {
        log.info("Handling MIT payment with recurring_details for payment: {}", intent.getPaymentId());
        
        com.hyperswitch.common.dto.RecurringDetails recurringDetails = request.getRecurringDetails();
        if (recurringDetails == null) {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "INVALID_REQUEST",
                "recurring_details is required for MIT payments"
            )));
        }
        
        // Get mandate or payment method from recurring_details
        String mandateId = recurringDetails.getMandateId();
        String paymentMethodId = recurringDetails.getPaymentMethodId();
        
        if (mandateId != null) {
            // Use existing mandate
            return mandateService.getMandate(com.hyperswitch.common.types.MandateId.of(mandateId))
                .flatMap(mandateResult -> {
                    if (mandateResult.isErr()) {
                        return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                            "MANDATE_NOT_FOUND",
                            "Mandate not found: " + mandateId
                        )));
                    }
                    
                    com.hyperswitch.common.dto.MandateResponse mandate = mandateResult.unwrap();
                    if (mandate.getStatus() != com.hyperswitch.common.types.MandateStatus.ACTIVE) {
                        return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                            "MANDATE_INACTIVE",
                            "Mandate is not active: " + mandateId
                        )));
                    }
                    
                    // Proceed with payment using mandate
                    intent.setOffSession(true);
                    return processMitPaymentWithMandate(intent, mandate, request);
                });
        } else if (paymentMethodId != null) {
            // Use payment method directly for MIT
            intent.setOffSession(true);
            return processMitPaymentWithPaymentMethod(intent, paymentMethodId, request);
        } else {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "INVALID_REQUEST",
                "Either mandate_id or payment_method_id must be provided in recurring_details"
            )));
        }
    }

    /**
     * Process MIT payment using an existing mandate
     */
    private Mono<Result<PaymentIntent, PaymentError>> processMitPaymentWithMandate(
            PaymentIntentEntity intent,
            @SuppressWarnings("java:S1172") com.hyperswitch.common.dto.MandateResponse mandate,
            ConfirmPaymentRequest request) {
        // Use mandate's payment method and connector
        intent.setOffSession(true);
        
        // Select connector and process payment (similar to regular flow)
        CreatePaymentRequest routingRequest = CreatePaymentRequest.builder()
            .amount(Amount.of(
                java.math.BigDecimal.valueOf(intent.getAmount()).divide(
                    java.math.BigDecimal.valueOf(100)
                ),
                intent.getCurrency()
            ))
            .merchantId(intent.getMerchantId())
            .paymentMethod(request.getPaymentMethod())
            .build();
        
        return routingService.selectConnectors(routingRequest, intent.getMerchantId())
            .flatMap(connectors -> {
                if (connectors.isEmpty()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "NO_CONNECTOR_AVAILABLE",
                        "No suitable connector found for MIT payment"
                    )));
                }
                
                String connectorName = connectors.get(0).name();
                
                PaymentAttemptEntity attempt = PaymentAttemptEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .paymentId(intent.getPaymentId())
                    .merchantId(intent.getMerchantId())
                    .status(STATUS_PROCESSING)
                    .connector(connectorName)
                    .createdAt(Instant.now())
                    .modifiedAt(Instant.now())
                    .profileId(intent.getProfileId())
                    .organizationId(intent.getOrganizationId())
                    .build();
                
                return paymentAttemptRepository.save(attempt)
                    .flatMap(savedAttempt -> {
                        intent.setStatus(PaymentStatus.PROCESSING.name());
                        intent.setActiveAttemptId(savedAttempt.getId());
                        intent.setAttemptCount(intent.getAttemptCount() + 1);
                        intent.setModifiedAt(Instant.now());
                        
                        return paymentIntentRepository.save(intent)
                            .flatMap(updatedIntent -> processPaymentWithConnector(
                                updatedIntent, 
                                savedAttempt, 
                                request,
                                connectorName
                            ));
                    });
            });
    }

    /**
     * Process MIT payment using a payment method directly
     */
    private Mono<Result<PaymentIntent, PaymentError>> processMitPaymentWithPaymentMethod(
            PaymentIntentEntity intent,
            @SuppressWarnings("java:S1172") String paymentMethodId,
            ConfirmPaymentRequest request) {
        // Similar to processMitPaymentWithMandate but using provided payment method
        intent.setOffSession(true);
        
        CreatePaymentRequest routingRequest = CreatePaymentRequest.builder()
            .amount(Amount.of(
                java.math.BigDecimal.valueOf(intent.getAmount()).divide(
                    java.math.BigDecimal.valueOf(100)
                ),
                intent.getCurrency()
            ))
            .merchantId(intent.getMerchantId())
            .paymentMethod(request.getPaymentMethod())
            .build();
        
        return routingService.selectConnectors(routingRequest, intent.getMerchantId())
            .flatMap(connectors -> {
                if (connectors.isEmpty()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "NO_CONNECTOR_AVAILABLE",
                        "No suitable connector found for MIT payment"
                    )));
                }
                
                String connectorName = connectors.get(0).name();
                
                PaymentAttemptEntity attempt = PaymentAttemptEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .paymentId(intent.getPaymentId())
                    .merchantId(intent.getMerchantId())
                    .status(STATUS_PROCESSING)
                    .connector(connectorName)
                    .createdAt(Instant.now())
                    .modifiedAt(Instant.now())
                    .profileId(intent.getProfileId())
                    .organizationId(intent.getOrganizationId())
                    .build();
                
                return paymentAttemptRepository.save(attempt)
                    .flatMap(savedAttempt -> {
                        intent.setStatus(PaymentStatus.PROCESSING.name());
                        intent.setActiveAttemptId(savedAttempt.getId());
                        intent.setAttemptCount(intent.getAttemptCount() + 1);
                        intent.setModifiedAt(Instant.now());
                        
                        return paymentIntentRepository.save(intent)
                            .flatMap(updatedIntent -> processPaymentWithConnector(
                                updatedIntent, 
                                savedAttempt, 
                                request,
                                connectorName
                            ));
                    });
            });
    }

    /**
     * Check if mandate should be created after payment success
     */
    private boolean shouldCreateMandate(PaymentIntentEntity intent, @SuppressWarnings("java:S1172") ConfirmPaymentRequest request) {
        // Create mandate if:
        // 1. Payment is setup_mandate type (zero-dollar authorization)
        // 2. Payment is off_session and succeeded
        // 3. setup_future_usage is off_session
        boolean isSetupMandate = "off_session".equals(intent.getSetupFutureUsage());
        boolean isOffSession = Boolean.TRUE.equals(intent.getOffSession());
        boolean isSucceeded = STATUS_SUCCEEDED.equals(intent.getStatus());
        
        return (isSetupMandate || isOffSession) && isSucceeded && intent.getCustomerId() != null;
    }

    /**
     * Create mandate from successful payment
     */
    private Mono<Void> createMandateFromPayment(
            PaymentIntentEntity intent,
            PaymentAttemptEntity attempt,
            ConfirmPaymentRequest request) {
        log.info("Creating mandate from payment: {}", intent.getPaymentId());
        
        // Determine mandate type based on amount
        com.hyperswitch.common.types.MandateType mandateType = intent.getAmount() == 0L 
            ? com.hyperswitch.common.types.MandateType.SINGLE_USE 
            : com.hyperswitch.common.types.MandateType.MULTI_USE;
        
        // Get payment method ID from request or attempt
        String paymentMethodId = extractPaymentMethodId(request, attempt);
        if (paymentMethodId == null) {
            log.warn("Cannot create mandate: payment method ID not found for payment: {}", intent.getPaymentId());
            return Mono.empty();
        }
        
        com.hyperswitch.common.dto.MandateRequest mandateRequest = com.hyperswitch.common.dto.MandateRequest.builder()
            .customerId(intent.getCustomerId())
            .paymentMethodId(paymentMethodId)
            .mandateType(mandateType)
            .mandateAmount(intent.getAmount() > 0L ? 
                com.hyperswitch.common.types.Amount.of(
                    java.math.BigDecimal.valueOf(intent.getAmount()).divide(
                        java.math.BigDecimal.valueOf(100)
                    ),
                    intent.getCurrency()
                ) : null)
            .metadata(intent.getMetadata())
            .build();
        
        return mandateService.createMandate(intent.getMerchantId(), mandateRequest)
            .doOnSuccess(result -> {
                if (result.isOk()) {
                    log.info("Mandate created successfully: {} for payment: {}", 
                        result.unwrap().getMandateId(), intent.getPaymentId());
                } else {
                    log.warn("Failed to create mandate for payment: {} - {}", 
                        intent.getPaymentId(), result.unwrapErr().getMessage());
                }
            })
            .then();
    }

    /**
     * Extract payment method ID from request or attempt
     */
    private String extractPaymentMethodId(ConfirmPaymentRequest request, PaymentAttemptEntity attempt) {
        // Try to get from payment method data
        if (request.getPaymentMethodData() != null) {
            Object pmId = request.getPaymentMethodData().get("payment_method_id");
            if (pmId != null) {
                return pmId.toString();
            }
        }
        
        // Try to get from attempt metadata
        if (attempt.getConnectorMetadata() != null) {
            Object pmId = attempt.getConnectorMetadata().get("payment_method_id");
            if (pmId != null) {
                return pmId.toString();
            }
        }
        
        return null;
    }

    private Mono<Result<PaymentIntent, PaymentError>> processCaptureResult(
            Result<ConnectorResponse, PaymentError> captureResult,
            PaymentIntentEntity intent,
            Long amountToCapture) {
        if (captureResult.isOk()) {
            return updateIntentAfterCapture(intent, amountToCapture);
        } else {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(captureResult.unwrapErr()));
        }
    }

    private Mono<Result<PaymentIntent, PaymentError>> updateIntentAfterCapture(
            PaymentIntentEntity intent,
            Long amountToCapture) {
        Long newAmountCaptured = (intent.getAmountCaptured() != null 
            ? intent.getAmountCaptured() : 0L) + amountToCapture;
        
        if (newAmountCaptured >= intent.getAmount()) {
            intent.setStatus(PaymentStatus.SUCCEEDED.name());
        } else {
            intent.setStatus(PaymentStatus.PARTIALLY_CAPTURED.name());
        }
        
        intent.setAmountCaptured(newAmountCaptured);
        intent.setModifiedAt(Instant.now());
        
        return paymentIntentRepository.save(intent)
            .map(saved -> Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(saved)));
    }

    private String determinePaymentStatus(ConnectorResponse response) {
        if (response.isRequires3DS()) {
            return PaymentStatus.REQUIRES_CUSTOMER_ACTION.name();
        }
        
        if (STATUS_SUCCEEDED.equalsIgnoreCase(response.getStatus())) {
            return PaymentStatus.SUCCEEDED.name();
        }
        
        return PaymentStatus.PROCESSING.name();
    }

    private boolean canConfirmPayment(String status) {
        return PaymentStatus.REQUIRES_CONFIRMATION.name().equals(status) ||
               PaymentStatus.REQUIRES_CUSTOMER_ACTION.name().equals(status);
    }

    @Override
    public Mono<Result<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>> handle3DSChallenge(
            PaymentId paymentId, 
            com.hyperswitch.common.dto.ThreeDSRequest request) {
        log.info("Handling 3DS challenge for payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                if (!PaymentStatus.REQUIRES_CUSTOMER_ACTION.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>err(
                        PaymentError.of("INVALID_STATUS", "Payment is not in 3DS challenge state")
                    ));
                }
                
                // Get active attempt
                @SuppressWarnings("null")
                String activeAttemptId = intent.getActiveAttemptId();
                if (activeAttemptId == null) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>err(
                        PaymentError.of("INVALID_STATE", "Payment has no active attempt")
                    ));
                }
                return paymentAttemptRepository.findById(activeAttemptId)
                    .flatMap(attempt -> {
                        // Generate redirect URL for 3DS challenge
                        String redirectUrl = generate3DSRedirectUrl(intent, attempt, request);
                        @SuppressWarnings("null")
                        String authenticationId = request.getAuthenticationId() != null 
                            ? request.getAuthenticationId() 
                            : UUID.randomUUID().toString();
                        
                        // Update attempt with authentication info
                        attempt.setAuthenticationType("THREE_DS");
                        attempt.setModifiedAt(Instant.now());
                        
                        return paymentAttemptRepository.save(attempt)
                            .then(Mono.just(Result.<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>ok(
                                com.hyperswitch.common.dto.ThreeDSResponse.builder()
                                    .redirectUrl(redirectUrl)
                                    .authenticationId(authenticationId)
                                    .status("requires_action")
                                    .message("3DS authentication required")
                                    .build()
                            )));
                    });
            })
            .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>err(
                PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)
            )))
            .onErrorResume(error -> {
                log.error("Error handling 3DS challenge: {}", paymentId, error);
                return Mono.just(Result.<com.hyperswitch.common.dto.ThreeDSResponse, PaymentError>err(
                    PaymentError.of("3DS_CHALLENGE_FAILED", "Failed to handle 3DS challenge: " + error.getMessage())
                ));
            });
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> resumePaymentAfter3DS(
            PaymentId paymentId, 
            String authenticationId) {
        log.info("Resuming payment after 3DS: {}, authenticationId: {}", paymentId, authenticationId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                if (!PaymentStatus.REQUIRES_CUSTOMER_ACTION.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(
                        PaymentError.of("INVALID_STATUS", "Payment is not in 3DS challenge state")
                    ));
                }
                
                // Get active attempt
                @SuppressWarnings("null")
                String activeAttemptId = intent.getActiveAttemptId();
                return paymentAttemptRepository.findById(activeAttemptId)
                    .flatMap(attempt -> 
                        // Verify 3DS authentication with connector
                        connectorService.verify3DS(
                            intent.getPaymentId(),
                            authenticationId,
                            attempt.getConnector(),
                            attempt.getConnectorTransactionId()
                        )
                        .flatMap(verifyResult -> {
                            if (verifyResult.isOk()) {
                                // Update attempt
                                attempt.setStatus(STATUS_SUCCEEDED);
                                attempt.setModifiedAt(Instant.now());
                                
                                return paymentAttemptRepository.save(attempt)
                                    .flatMap(savedAttempt -> {
                                        // Update intent based on capture method
                                        if (intent.getAmountCaptured() != null && intent.getAmountCaptured() > 0) {
                                            intent.setStatus(PaymentStatus.SUCCEEDED.name());
                                        } else {
                                            intent.setStatus(PaymentStatus.REQUIRES_CAPTURE.name());
                                        }
                                        intent.setModifiedAt(Instant.now());
                                        
                                        return paymentIntentRepository.save(intent)
                                            .map(saved -> Result.<PaymentIntent, PaymentError>ok(
                                                paymentMapper.toPaymentIntent(saved)
                                            ));
                                    });
                            } else {
                                // 3DS verification failed
                                attempt.setStatus(STATUS_FAILED);
                                attempt.setErrorMessage(verifyResult.unwrapErr().getMessage());
                                attempt.setErrorCode(verifyResult.unwrapErr().getCode());
                                attempt.setModifiedAt(Instant.now());
                                
                                return paymentAttemptRepository.save(attempt)
                                    .flatMap(savedAttempt -> {
                                        intent.setStatus(PaymentStatus.FAILED.name());
                                        intent.setModifiedAt(Instant.now());
                                        
                                        return paymentIntentRepository.save(intent)
                                            .then(Mono.just(Result.<PaymentIntent, PaymentError>err(
                                                verifyResult.unwrapErr()
                                            )));
                                    });
                            }
                        })
                    );
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(
                PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)
            )))
            .onErrorResume(error -> {
                log.error("Error resuming payment after 3DS: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("3DS_RESUME_FAILED", "Failed to resume payment after 3DS: " + error.getMessage())
                ));
            });
    }

    private String generate3DSRedirectUrl(
            PaymentIntentEntity intent, 
            PaymentAttemptEntity attempt, 
            com.hyperswitch.common.dto.ThreeDSRequest request) {
        // Generate 3DS redirect URL
        // In production, this would be generated based on connector response
        String baseUrl = request.getReturnUrl() != null 
            ? request.getReturnUrl() 
            : "https://api.hyperswitch.io/3ds/callback";
        
        return baseUrl + "?payment_id=" + intent.getPaymentId() 
            + "&attempt_id=" + attempt.getId()
            + "&authentication_id=" + (request.getAuthenticationId() != null 
                ? request.getAuthenticationId() 
                : UUID.randomUUID().toString());
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> cancelPayment(
            PaymentId paymentId, 
            com.hyperswitch.common.dto.CancelPaymentRequest request) {
        log.info("Cancelling payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Only allow cancellation for payments in certain states
                String status = intent.getStatus();
                if (!canCancelPayment(status)) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_STATUS",
                        "Payment cannot be cancelled in current status: " + status
                    )));
                }
                
                // Update payment status to cancelled
                intent.setStatus(PaymentStatus.CANCELLED.name());
                intent.setModifiedAt(Instant.now());
                
                // Update metadata with cancellation reason if provided
                updateMetadata(intent, "cancellation_reason", request.getCancellationReason());
                if (request.getMetadata() != null) {
                    updateMetadata(intent, request.getMetadata());
                }
                
                return paymentIntentRepository.save(intent)
                    .map(saved -> Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(saved)));
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error cancelling payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "PAYMENT_CANCEL_FAILED",
                    "Failed to cancel payment: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> updatePayment(
            PaymentId paymentId, 
            com.hyperswitch.common.dto.UpdatePaymentRequest request) {
        log.info("Updating payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Only allow updates for payments in certain states
                String status = intent.getStatus();
                if (!canUpdatePayment(status)) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_STATUS",
                        "Payment cannot be updated in current status: " + status
                    )));
                }
                
                // Update fields if provided
                if (request.getAmount() != null) {
                    intent.setAmount(convertToMinorUnits(request.getAmount()));
                }
                if (request.getDescription() != null) {
                    intent.setDescription(request.getDescription());
                }
                if (request.getReturnUrl() != null) {
                    intent.setReturnUrl(request.getReturnUrl());
                }
                if (request.getMetadata() != null) {
                    updateMetadata(intent, request.getMetadata());
                }
                
                intent.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(intent)
                    .map(saved -> Result.<PaymentIntent, PaymentError>ok(paymentMapper.toPaymentIntent(saved)));
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error updating payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "PAYMENT_UPDATE_FAILED",
                    "Failed to update payment: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<String, PaymentError>> getClientSecret(PaymentId paymentId) {
        log.info("Getting client secret for payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                String clientSecret = intent.getClientSecret();
                if (clientSecret == null || clientSecret.isEmpty()) {
                    // Generate client secret if not present
                    clientSecret = generateClientSecret(intent);
                    intent.setClientSecret(clientSecret);
                    intent.setModifiedAt(Instant.now());
                    
                    return paymentIntentRepository.save(intent)
                        .map(saved -> Result.<String, PaymentError>ok(saved.getClientSecret()));
                }
                return Mono.just(Result.<String, PaymentError>ok(clientSecret));
            })
            .switchIfEmpty(Mono.just(Result.<String, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error getting client secret: {}", paymentId, error);
                return Mono.just(Result.<String, PaymentError>err(PaymentError.of(
                    "CLIENT_SECRET_FAILED",
                    "Failed to get client secret: " + error.getMessage()
                )));
            });
    }

    private boolean canCancelPayment(String status) {
        return PaymentStatus.REQUIRES_CONFIRMATION.name().equals(status) ||
               PaymentStatus.REQUIRES_CAPTURE.name().equals(status) ||
               PaymentStatus.PARTIALLY_CAPTURED.name().equals(status) ||
               PaymentStatus.REQUIRES_CUSTOMER_ACTION.name().equals(status);
    }

    private boolean canUpdatePayment(String status) {
        return PaymentStatus.REQUIRES_CONFIRMATION.name().equals(status) ||
               PaymentStatus.REQUIRES_CUSTOMER_ACTION.name().equals(status);
    }

    private String generateClientSecret(PaymentIntentEntity intent) {
        // Generate a secure client secret
        // In production, use a cryptographically secure random generator
        return "pi_" + intent.getPaymentId() + "_secret_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateClientSecretForPayment(String paymentId) {
        // Generate a secure client secret for new payment
        // In production, use a cryptographically secure random generator
        return "pi_" + paymentId + "_secret_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Long convertToMinorUnits(Amount amount) {
        return amount.getValue()
            .multiply(java.math.BigDecimal.valueOf(CURRENCY_MULTIPLIER))
            .longValue();
    }

    private void updateMetadata(PaymentIntentEntity intent, String key, String value) {
        if (value != null) {
            if (intent.getMetadata() == null) {
                intent.setMetadata(new java.util.HashMap<>());
            }
            intent.getMetadata().put(key, value);
        }
    }

    private void updateMetadata(PaymentIntentEntity intent, java.util.Map<String, Object> metadata) {
        if (metadata != null && !metadata.isEmpty()) {
            if (intent.getMetadata() == null) {
                intent.setMetadata(new java.util.HashMap<>());
            }
            intent.getMetadata().putAll(metadata);
        }
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> incrementalAuthorization(
            PaymentId paymentId,
            com.hyperswitch.common.dto.IncrementalAuthorizationRequest request) {
        log.info("Incremental authorization for payment: {}, new amount: {}", paymentId, request.getAmount());
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Check if payment is in REQUIRES_CAPTURE status
                if (!PaymentStatus.REQUIRES_CAPTURE.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_PAYMENT_STATUS",
                        "Payment must be in REQUIRES_CAPTURE status for incremental authorization"
                    )));
                }
                
                // Check if new amount is greater than current amount
                Long currentAmount = intent.getAmount() != null ? intent.getAmount() : 0L;
                if (request.getAmount() <= currentAmount) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_AMOUNT",
                        "New amount must be greater than current authorized amount"
                    )));
                }
                
                // Get active payment attempt
                return paymentAttemptRepository
                    .findFirstByPaymentIdAndMerchantIdOrderByCreatedAtDesc(
                        paymentId.getValue(),
                        intent.getMerchantId()
                    )
                    .flatMap(attempt -> {
                        // Update payment intent amount
                        intent.setAmount(request.getAmount());
                        intent.setModifiedAt(Instant.now());
                        
                        // Update payment attempt amount
                        attempt.setAmountToCapture(request.getAmount());
                        attempt.setAmountCapturable(request.getAmount());
                        attempt.setModifiedAt(Instant.now());
                        
                        // Save both entities
                        return paymentIntentRepository.save(intent)
                            .flatMap(savedIntent -> paymentAttemptRepository.save(attempt)
                                .map(savedAttempt -> {
                                    log.info("Incremental authorization successful for payment: {}", paymentId);
                                    return Result.<PaymentIntent, PaymentError>ok(
                                        paymentMapper.toPaymentIntent(savedIntent)
                                    );
                                })
                            );
                    })
                    .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        "Payment attempt not found"
                    ))));
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error in incremental authorization: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "INCREMENTAL_AUTHORIZATION_FAILED",
                    "Failed to perform incremental authorization: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> extendAuthorization(PaymentId paymentId) {
        log.info("Extending authorization for payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Check if payment is in REQUIRES_CAPTURE status
                if (!PaymentStatus.REQUIRES_CAPTURE.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_PAYMENT_STATUS",
                        "Payment must be in REQUIRES_CAPTURE status to extend authorization"
                    )));
                }
                
                // Get active payment attempt
                return paymentAttemptRepository
                    .findFirstByPaymentIdAndMerchantIdOrderByCreatedAtDesc(
                        paymentId.getValue(),
                        intent.getMerchantId()
                    )
                    .flatMap(attempt -> {
                        // In a real implementation, this would call the connector to extend authorization
                        // For now, we just update the metadata to indicate extension was requested
                        if (intent.getMetadata() == null) {
                            intent.setMetadata(new HashMap<>());
                        }
                        intent.getMetadata().put("authorization_extended_at", Instant.now().toString());
                        intent.setModifiedAt(Instant.now());
                        
                        return paymentIntentRepository.save(intent)
                            .map(savedIntent -> {
                                log.info("Authorization extended for payment: {}", paymentId);
                                return Result.<PaymentIntent, PaymentError>ok(
                                    paymentMapper.toPaymentIntent(savedIntent)
                                );
                            });
                    })
                    .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        "Payment attempt not found"
                    ))));
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error extending authorization: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "EXTEND_AUTHORIZATION_FAILED",
                    "Failed to extend authorization: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> voidPayment(
            PaymentId paymentId,
            com.hyperswitch.common.dto.VoidPaymentRequest request) {
        log.info("Voiding payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Check if payment can be voided (must be in REQUIRES_CAPTURE status)
                if (!PaymentStatus.REQUIRES_CAPTURE.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_PAYMENT_STATUS",
                        "Payment must be in REQUIRES_CAPTURE status to be voided"
                    )));
                }
                
                // Get active payment attempt
                return paymentAttemptRepository
                    .findFirstByPaymentIdAndMerchantIdOrderByCreatedAtDesc(
                        paymentId.getValue(),
                        intent.getMerchantId()
                    )
                    .flatMap(attempt -> {
                        // Update payment intent status to CANCELLED
                        intent.setStatus(PaymentStatus.CANCELLED.name());
                        intent.setModifiedAt(Instant.now());
                        
                        // Update payment attempt status to VOIDED
                        attempt.setStatus("VOIDED");
                        if (request.getCancellationReason() != null) {
                            attempt.setErrorMessage(request.getCancellationReason());
                        }
                        attempt.setModifiedAt(Instant.now());
                        
                        // Save both entities
                        return paymentIntentRepository.save(intent)
                            .flatMap(savedIntent -> paymentAttemptRepository.save(attempt)
                                .map(savedAttempt -> {
                                    log.info("Payment voided successfully: {}", paymentId);
                                    return Result.<PaymentIntent, PaymentError>ok(
                                        paymentMapper.toPaymentIntent(savedIntent)
                                    );
                                })
                            );
                    })
                    .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        "Payment attempt not found"
                    ))));
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error voiding payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "VOID_PAYMENT_FAILED",
                    "Failed to void payment: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> scheduleCapture(
            PaymentId paymentId,
            com.hyperswitch.common.dto.ScheduleCaptureRequest request) {
        log.info("Scheduling capture for payment: {} at {}", paymentId, request.getScheduledAt());
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Check if payment is in REQUIRES_CAPTURE status
                if (!PaymentStatus.REQUIRES_CAPTURE.name().equals(intent.getStatus())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_PAYMENT_STATUS",
                        "Payment must be in REQUIRES_CAPTURE status to schedule capture"
                    )));
                }
                
                // Validate scheduled time is in the future
                if (request.getScheduledAt().isBefore(Instant.now())) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "INVALID_SCHEDULE_TIME",
                        "Scheduled capture time must be in the future"
                    )));
                }
                
                // Store scheduled capture information in metadata
                if (intent.getMetadata() == null) {
                    intent.setMetadata(new HashMap<>());
                }
                intent.getMetadata().put("scheduled_capture_at", request.getScheduledAt().toString());
                if (request.getAmount() != null) {
                    intent.getMetadata().put("scheduled_capture_amount", request.getAmount().toString());
                }
                intent.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(intent)
                    .map(savedIntent -> {
                        log.info("Capture scheduled for payment: {} at {}", paymentId, request.getScheduledAt());
                        // In a real implementation, this would schedule a job to capture the payment
                        // For now, we just store the schedule information
                        return Result.<PaymentIntent, PaymentError>ok(
                            paymentMapper.toPaymentIntent(savedIntent)
                        );
                    });
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error scheduling capture: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "SCHEDULE_CAPTURE_FAILED",
                    "Failed to schedule capture: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> approvePayment(
            PaymentId paymentId,
            com.hyperswitch.common.dto.ApprovePaymentRequest request) {
        log.info("Approving payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Update payment intent with approval
                if (intent.getMetadata() == null) {
                    intent.setMetadata(new HashMap<>());
                }
                intent.getMetadata().put("merchant_decision", "APPROVED");
                if (request.getReason() != null) {
                    intent.getMetadata().put("approval_reason", request.getReason());
                }
                intent.getMetadata().put("approved_at", Instant.now().toString());
                
                // If payment is in REQUIRES_CAPTURE, it can be approved to proceed
                if (PaymentStatus.REQUIRES_CAPTURE.name().equals(intent.getStatus())) {
                    // Payment remains in REQUIRES_CAPTURE, but is marked as approved
                    intent.setModifiedAt(Instant.now());
                }
                
                return paymentIntentRepository.save(intent)
                    .map(savedIntent -> {
                        log.info("Payment approved: {}", paymentId);
                        return Result.<PaymentIntent, PaymentError>ok(
                            paymentMapper.toPaymentIntent(savedIntent)
                        );
                    });
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error approving payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "APPROVE_PAYMENT_FAILED",
                    "Failed to approve payment: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> rejectPayment(
            PaymentId paymentId,
            com.hyperswitch.common.dto.RejectPaymentRequest request) {
        log.info("Rejecting payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> {
                // Update payment intent with rejection
                if (intent.getMetadata() == null) {
                    intent.setMetadata(new HashMap<>());
                }
                intent.getMetadata().put("merchant_decision", "REJECTED");
                if (request.getReason() != null) {
                    intent.getMetadata().put("rejection_reason", request.getReason());
                }
                intent.getMetadata().put("rejected_at", Instant.now().toString());
                
                // Update payment status to CANCELLED if it's in a cancellable state
                if (canCancelPayment(intent.getStatus())) {
                    intent.setStatus(PaymentStatus.CANCELLED.name());
                }
                
                intent.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(intent)
                    .map(savedIntent -> {
                        log.info("Payment rejected: {}", paymentId);
                        return Result.<PaymentIntent, PaymentError>ok(
                            paymentMapper.toPaymentIntent(savedIntent)
                        );
                    });
            })
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error rejecting payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "REJECT_PAYMENT_FAILED",
                    "Failed to reject payment: " + error.getMessage()
                )));
            });
    }

    @Override
    public Mono<Result<PaymentIntent, PaymentError>> syncPayment(
            PaymentId paymentId,
            com.hyperswitch.common.dto.SyncPaymentRequest request) {
        log.info("Syncing payment status with connector: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .flatMap(intent -> syncPaymentWithAttempts(intent, paymentId, request))
            .switchIfEmpty(Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "PAYMENT_NOT_FOUND",
                PAYMENT_NOT_FOUND_MSG + ": " + paymentId.getValue()
            ))))
            .onErrorResume(error -> {
                log.error("Error syncing payment: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "SYNC_PAYMENT_FAILED",
                    "Failed to sync payment: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Sync payment with payment attempts
     */
    private Mono<Result<PaymentIntent, PaymentError>> syncPaymentWithAttempts(
            PaymentIntentEntity intent,
            PaymentId paymentId,
            com.hyperswitch.common.dto.SyncPaymentRequest request) {
        return paymentAttemptRepository.findByPaymentIdAndMerchantId(paymentId.getValue(), intent.getMerchantId())
            .sort((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
            .take(1)
            .collectList()
            .flatMap(attempts -> {
                if (attempts.isEmpty()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                        "NO_PAYMENT_ATTEMPT",
                        "No payment attempt found for payment: " + paymentId.getValue()
                    )));
                }
                
                PaymentAttemptEntity latestAttempt = attempts.get(0);
                return validateAndSyncPayment(intent, latestAttempt, paymentId, request);
            });
    }
    
    /**
     * Validate connector info and sync payment
     */
    private Mono<Result<PaymentIntent, PaymentError>> validateAndSyncPayment(
            PaymentIntentEntity intent,
            PaymentAttemptEntity latestAttempt,
            PaymentId paymentId,
            com.hyperswitch.common.dto.SyncPaymentRequest request) {
        String connectorTransactionId = latestAttempt.getConnectorTransactionId();
        String connectorId = latestAttempt.getConnector();
        
        if (connectorTransactionId == null || connectorId == null) {
            return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                "MISSING_CONNECTOR_INFO",
                "Missing connector transaction ID or connector ID for payment: " + paymentId.getValue()
            )));
        }
        
        // Check if we should skip sync
        if (shouldSkipSync(intent, request, paymentId)) {
            return Mono.just(Result.<PaymentIntent, PaymentError>ok(
                paymentMapper.toPaymentIntent(intent)
            ));
        }
        
        return performPaymentSync(intent, latestAttempt, connectorId, connectorTransactionId, paymentId);
    }
    
    /**
     * Check if sync should be skipped
     */
    private boolean shouldSkipSync(
            PaymentIntentEntity intent,
            com.hyperswitch.common.dto.SyncPaymentRequest request,
            PaymentId paymentId) {
        boolean shouldSync = request.getForceSync() != null && request.getForceSync();
        if (shouldSync) {
            return false;
        }
        
        // Check last sync time from metadata
        if (intent.getMetadata() != null && intent.getMetadata().containsKey("last_sync_at")) {
            String lastSyncStr = (String) intent.getMetadata().get("last_sync_at");
            try {
                Instant lastSync = Instant.parse(lastSyncStr);
                // Skip if synced within last 5 minutes
                if (lastSync.isAfter(Instant.now().minusSeconds(300))) {
                    log.debug("Skipping sync for payment {} - recently synced", paymentId);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Error parsing last_sync_at for payment: {}", paymentId, e);
            }
        }
        return false;
    }
    
    /**
     * Perform actual payment sync with connector
     */
    private Mono<Result<PaymentIntent, PaymentError>> performPaymentSync(
            PaymentIntentEntity intent,
            PaymentAttemptEntity latestAttempt,
            String connectorId,
            String connectorTransactionId,
            PaymentId paymentId) {
        return connectorService.syncPayment(connectorId, connectorTransactionId)
            .flatMap(result -> {
                if (result.isErr()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(result.unwrapErr()));
                }
                
                ConnectorResponse connectorResponse = result.unwrap();
                return updatePaymentFromSyncResponse(intent, latestAttempt, connectorResponse, paymentId);
            })
            .onErrorResume(error -> {
                log.error("Error syncing payment with connector: {}", paymentId, error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(PaymentError.of(
                    "SYNC_PAYMENT_FAILED",
                    "Failed to sync payment: " + error.getMessage()
                )));
            });
    }
    
    /**
     * Update payment from sync response
     */
    private Mono<Result<PaymentIntent, PaymentError>> updatePaymentFromSyncResponse(
            PaymentIntentEntity intent,
            PaymentAttemptEntity latestAttempt,
            ConnectorResponse connectorResponse,
            PaymentId paymentId) {
        String newStatus = mapConnectorStatusToPaymentStatus(connectorResponse.getStatus());
        
        if (!intent.getStatus().equals(newStatus)) {
            log.info("Payment status updated from {} to {} for payment: {}", 
                intent.getStatus(), newStatus, paymentId);
            intent.setStatus(newStatus);
            
            // Update latest attempt status
            latestAttempt.setStatus(connectorResponse.getStatus());
            paymentAttemptRepository.save(latestAttempt).subscribe();
        }
        
        updateSyncMetadata(intent);
        intent.setModifiedAt(Instant.now());
        
        return paymentIntentRepository.save(intent)
            .map(savedIntent -> {
                log.info("Payment sync completed for: {}", paymentId);
                return Result.<PaymentIntent, PaymentError>ok(
                    paymentMapper.toPaymentIntent(savedIntent)
                );
            });
    }
    
    /**
     * Update sync metadata
     */
    private void updateSyncMetadata(PaymentIntentEntity intent) {
        if (intent.getMetadata() == null) {
            intent.setMetadata(new HashMap<>());
        }
        intent.getMetadata().put("last_sync_at", Instant.now().toString());
        intent.getMetadata().put("sync_count", 
            String.valueOf(Integer.parseInt(
                intent.getMetadata().getOrDefault("sync_count", "0").toString()) + 1));
    }
    
    private String mapConnectorStatusToPaymentStatus(String connectorStatus) {
        // Map connector status to payment intent status
        if (connectorStatus == null) {
            return PaymentStatus.PROCESSING.name();
        }
        
        String statusLower = connectorStatus.toLowerCase();
        if (statusLower.contains("succeeded") || statusLower.contains("success") || 
            statusLower.contains("completed") || statusLower.contains("captured")) {
            return PaymentStatus.SUCCEEDED.name();
        } else if (statusLower.contains("failed") || statusLower.contains("declined") || 
                   statusLower.contains("rejected")) {
            return PaymentStatus.FAILED.name();
        } else if (statusLower.contains("authorized") || statusLower.contains("pending_capture")) {
            return PaymentStatus.REQUIRES_CAPTURE.name();
        } else if (statusLower.contains("pending") || statusLower.contains("processing")) {
            return PaymentStatus.PROCESSING.name();
        } else {
            return PaymentStatus.PROCESSING.name();
        }
    }
    
    /**
     * Routing decision data holder
     */
    private static class RoutingDecisionData {
        private final String paymentId;
        private final String attemptId;
        private final String merchantId;
        private final String profileId;
        private final String selectedConnector;
        private final String routingAlgorithm;
        private final Long amount;
        private final String currency;
        private final String paymentMethod;
        
        private RoutingDecisionData(Builder builder) {
            this.paymentId = builder.paymentId;
            this.attemptId = builder.attemptId;
            this.merchantId = builder.merchantId;
            this.profileId = builder.profileId;
            this.selectedConnector = builder.selectedConnector;
            this.routingAlgorithm = builder.routingAlgorithm;
            this.amount = builder.amount;
            this.currency = builder.currency;
            this.paymentMethod = builder.paymentMethod;
        }
        
        static class Builder {
            private String paymentId;
            private String attemptId;
            private String merchantId;
            private String profileId;
            private String selectedConnector;
            private String routingAlgorithm;
            private Long amount;
            private String currency;
            private String paymentMethod;
            
            Builder paymentId(String paymentId) { this.paymentId = paymentId; return this; }
            Builder attemptId(String attemptId) { this.attemptId = attemptId; return this; }
            Builder merchantId(String merchantId) { this.merchantId = merchantId; return this; }
            Builder profileId(String profileId) { this.profileId = profileId; return this; }
            Builder selectedConnector(String selectedConnector) { this.selectedConnector = selectedConnector; return this; }
            Builder routingAlgorithm(String routingAlgorithm) { this.routingAlgorithm = routingAlgorithm; return this; }
            Builder amount(Long amount) { this.amount = amount; return this; }
            Builder currency(String currency) { this.currency = currency; return this; }
            Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
            
            RoutingDecisionData build() {
                return new RoutingDecisionData(this);
            }
        }
    }
    
    /**
     * Log routing decision when connector is selected
     */
    @SuppressWarnings("java:S107")
    private Mono<Void> logRoutingDecision(
        String paymentId,
        String attemptId,
        String merchantId,
        String profileId,
        String selectedConnector,
        String routingAlgorithm,
        Long amount,
        String currency,
        String paymentMethod
    ) {
        RoutingDecisionData data = new RoutingDecisionData.Builder()
            .paymentId(paymentId)
            .attemptId(attemptId)
            .merchantId(merchantId)
            .profileId(profileId)
            .selectedConnector(selectedConnector)
            .routingAlgorithm(routingAlgorithm)
            .amount(amount)
            .currency(currency)
            .paymentMethod(paymentMethod)
            .build();
        return logRoutingDecision(data);
    }
    
    /**
     * Log routing decision using data object
     */
    private Mono<Void> logRoutingDecision(RoutingDecisionData data) {
        RoutingDecisionLogEntity logEntity = new RoutingDecisionLogEntity();
        logEntity.setId(UUID.randomUUID().toString());
        logEntity.setPaymentId(data.paymentId);
        logEntity.setAttemptId(data.attemptId);
        logEntity.setMerchantId(data.merchantId);
        logEntity.setProfileId(data.profileId);
        logEntity.setSelectedConnector(data.selectedConnector);
        logEntity.setRoutingAlgorithm(data.routingAlgorithm);
        logEntity.setAmount(data.amount);
        logEntity.setCurrency(data.currency);
        logEntity.setPaymentMethod(data.paymentMethod);
        logEntity.setSuccess(null); // Will be updated when payment completes
        logEntity.setCreatedAt(Instant.now());
        
        return routingDecisionLogRepository.save(logEntity)
            .doOnSuccess(v -> log.debug("Logged routing decision: payment={}, connector={}, algorithm={}", 
                data.paymentId, data.selectedConnector, data.routingAlgorithm))
            .doOnError(error -> log.warn("Failed to log routing decision", error))
            .then();
    }
    
    /**
     * Update routing decision log with success status
     */
    private Mono<Void> updateRoutingDecisionLog(String paymentId, @SuppressWarnings("java:S1172") String attemptId, boolean success) {
        return routingDecisionLogRepository.findByPaymentId(paymentId)
            .flatMap(logEntity -> {
                logEntity.setSuccess(success);
                return routingDecisionLogRepository.save(logEntity);
            })
            .doOnError(error -> log.warn("Failed to update routing decision log for payment: {}", paymentId, error))
            .then();
    }
    
    /**
     * Record payment attempt for success rate analytics
     */
    private Mono<Void> recordPaymentAttemptForAnalytics(
        PaymentIntentEntity intent,
        PaymentAttemptEntity attempt,
        boolean success
    ) {
        // Skip analytics if service is not available
        if (analyticsService == null) {
            return Mono.empty();
        }
        
        if (attempt.getConnector() == null) {
            return Mono.empty();
        }
        
        try {
            Connector connector = Connector.valueOf(attempt.getConnector().toUpperCase());
            
            // Record overall success rate
            Mono<Void> overallRecord = analyticsService.recordPaymentAttempt(
                intent.getMerchantId(),
                intent.getProfileId(),
                connector,
                attempt.getPaymentMethodId(), // Could be null, will default to "unknown"
                intent.getCurrency(),
                success
            );
            
            // Record windowed success rate for real-time tracking (60-minute window)
            Mono<Void> windowedRecord = analyticsService.updateSuccessRateWindow(
                intent.getProfileId(),
                attempt.getConnector(),
                attempt.getPaymentMethodId() != null ? attempt.getPaymentMethodId() : "unknown",
                intent.getCurrency(),
                success,
                60 // 60-minute window for real-time tracking
            );
            
            return Mono.when(overallRecord, windowedRecord)
                .doOnSuccess(v -> log.debug("Recorded payment attempt for analytics: payment={}, connector={}, success={}", 
                    intent.getPaymentId(), attempt.getConnector(), success))
                .doOnError(error -> log.warn("Failed to record payment attempt for analytics", error))
                .then();
        } catch (IllegalArgumentException _) {
            log.warn("Invalid connector name for analytics: {}", attempt.getConnector());
            return Mono.empty();
        }
    }
    
    @Override
    public Mono<Result<reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentAttemptResponse>, PaymentError>> listPaymentAttempts(
            PaymentId paymentId,
            String merchantId) {
        log.info("Listing payment attempts for payment: {}, merchant: {}", paymentId, merchantId);
        
        // Verify payment exists and belongs to merchant
        return paymentIntentRepository.findByPaymentId(paymentId.getValue())
            .filter(intent -> merchantId.equals(intent.getMerchantId()))
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(intent -> {
                // Get all attempts for this payment
                String currency = intent.getCurrency();
                reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentAttemptResponse> attempts = 
                    paymentAttemptRepository.findByPaymentIdAndMerchantId(paymentId.getValue(), merchantId)
                        .map(attempt -> toPaymentAttemptResponse(attempt, currency));
                
                return Mono.just(Result.<reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentAttemptResponse>, PaymentError>ok(attempts));
            })
            .onErrorResume(error -> {
                log.error("Error listing payment attempts", error);
                String errorCode = PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())
                    ? "PAYMENT_NOT_FOUND"
                    : "PAYMENT_ATTEMPT_LIST_FAILED";
                return Mono.just(Result.<reactor.core.publisher.Flux<com.hyperswitch.common.dto.PaymentAttemptResponse>, PaymentError>err(
                    PaymentError.of(errorCode,
                        "Failed to list payment attempts: " + error.getMessage())
                ));
            });
    }
    
    private com.hyperswitch.common.dto.PaymentAttemptResponse toPaymentAttemptResponse(PaymentAttemptEntity entity, String currency) {
        com.hyperswitch.common.dto.PaymentAttemptResponse response = new com.hyperswitch.common.dto.PaymentAttemptResponse();
        response.setId(entity.getId());
        response.setPaymentId(entity.getPaymentId());
        response.setMerchantId(entity.getMerchantId());
        response.setConnector(entity.getConnector());
        response.setStatus(entity.getStatus());
        // Use amountToCapture or amountCapturable as amount
        response.setAmount(entity.getAmountToCapture() != null ? entity.getAmountToCapture() : entity.getAmountCapturable());
        response.setCurrency(currency);
        response.setPaymentMethod(entity.getPaymentMethodId());
        response.setConnectorTransactionId(entity.getConnectorTransactionId());
        response.setErrorCode(entity.getErrorCode());
        response.setErrorMessage(entity.getErrorMessage());
        // Use connectorMetadata as metadata
        response.setMetadata(entity.getConnectorMetadata());
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        return response;
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>> listPayments(
            String merchantId,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        log.info("Listing payments for merchant: {} with filters", merchantId);
        
        try {
            reactor.core.publisher.Flux<PaymentIntentEntity> query = buildPaymentQuery(merchantId, constraints);
            query = applyPaymentFilters(query, constraints);
            query = applyPaymentSorting(query, constraints);
            query = applyPaymentPagination(query, constraints);
            
            return convertPaymentsToResponse(query, merchantId, constraints);
        } catch (Exception e) {
            log.error("Error listing payments", e);
            return Mono.just(Result.<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>err(
                PaymentError.of("PAYMENT_LIST_FAILED",
                    "Failed to list payments: " + e.getMessage())
            ));
        }
    }
    
    /**
     * Build base payment query
     */
    private reactor.core.publisher.Flux<PaymentIntentEntity> buildPaymentQuery(
            String merchantId,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        int defaultLimit = constraints.getLimit() != null ? constraints.getLimit() : 10;
        
        if (constraints.getStartTime() != null && constraints.getEndTime() != null) {
            return paymentIntentRepository.findByMerchantIdAndCreatedAtBetween(
                merchantId, constraints.getStartTime(), constraints.getEndTime());
        }
        return paymentIntentRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, defaultLimit);
    }
    
    /**
     * Apply filters to payment query
     */
    private reactor.core.publisher.Flux<PaymentIntentEntity> applyPaymentFilters(
            reactor.core.publisher.Flux<PaymentIntentEntity> query,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        if (constraints.getStatus() != null && !constraints.getStatus().isEmpty()) {
            query = query.filter(entity -> constraints.getStatus().contains(entity.getStatus()));
        }
        if (constraints.getCurrency() != null && !constraints.getCurrency().isEmpty()) {
            query = query.filter(entity -> constraints.getCurrency().contains(entity.getCurrency()));
        }
        if (constraints.getCustomerId() != null) {
            query = query.filter(entity -> constraints.getCustomerId().equals(entity.getCustomerId()));
        }
        if (constraints.getPaymentId() != null) {
            query = query.filter(entity -> constraints.getPaymentId().equals(entity.getPaymentId()));
        }
        if (constraints.getAmountFilter() != null) {
            query = applyAmountFilter(query, constraints.getAmountFilter());
        }
        return query;
    }
    
    /**
     * Apply amount filter
     */
    private reactor.core.publisher.Flux<PaymentIntentEntity> applyAmountFilter(
            reactor.core.publisher.Flux<PaymentIntentEntity> query,
            com.hyperswitch.common.dto.AmountFilter amountFilter) {
        if (amountFilter.getStartAmount() != null) {
            query = query.filter(entity -> entity.getAmount() >= amountFilter.getStartAmount());
        }
        if (amountFilter.getEndAmount() != null) {
            query = query.filter(entity -> entity.getAmount() <= amountFilter.getEndAmount());
        }
        return query;
    }
    
    /**
     * Apply sorting to payment query
     */
    private reactor.core.publisher.Flux<PaymentIntentEntity> applyPaymentSorting(
            reactor.core.publisher.Flux<PaymentIntentEntity> query,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        if (constraints.getOrder() == null) {
            return query;
        }
        
        com.hyperswitch.common.dto.Order order = constraints.getOrder();
        if (order.getOn() == com.hyperswitch.common.dto.Order.SortOn.CREATED_AT) {
            return order.getBy() == com.hyperswitch.common.dto.Order.SortBy.DESC
                ? query.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                : query.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        }
        if (order.getOn() == com.hyperswitch.common.dto.Order.SortOn.AMOUNT) {
            return order.getBy() == com.hyperswitch.common.dto.Order.SortBy.DESC
                ? query.sort((a, b) -> Long.compare(b.getAmount(), a.getAmount()))
                : query.sort((a, b) -> Long.compare(a.getAmount(), b.getAmount()));
        }
        return query;
    }
    
    /**
     * Apply pagination to payment query
     */
    private reactor.core.publisher.Flux<PaymentIntentEntity> applyPaymentPagination(
            reactor.core.publisher.Flux<PaymentIntentEntity> query,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        int offset = constraints.getOffset() != null ? constraints.getOffset() : 0;
        int limit = constraints.getLimit() != null ? constraints.getLimit() : 10;
        return query.skip(offset).take(limit);
    }
    
    /**
     * Convert payments to response
     */
    private Mono<Result<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>> convertPaymentsToResponse(
            reactor.core.publisher.Flux<PaymentIntentEntity> query,
            String merchantId,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        return query
            .map(paymentMapper::toPaymentIntent)
            .map(this::paymentIntentToMap)
            .collectList()
            .flatMap(payments -> {
                int offset = constraints.getOffset() != null ? constraints.getOffset() : 0;
                int limit = constraints.getLimit() != null ? constraints.getLimit() : 10;
                
                return paymentIntentRepository
                    .findByMerchantIdOrderByCreatedAtDesc(merchantId, 10000)
                    .count()
                    .map(totalCount -> {
                        com.hyperswitch.common.dto.PaymentListResponse response = new com.hyperswitch.common.dto.PaymentListResponse();
                        response.setData(payments);
                        response.setTotalCount(totalCount.intValue());
                        response.setLimit(limit);
                        response.setOffset(offset);
                        return Result.<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>ok(response);
                    });
            })
            .onErrorResume(error -> {
                log.error("Error listing payments", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_LIST_FAILED",
                        "Failed to list payments: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentListFiltersResponse, PaymentError>> getPaymentFilters(
            String merchantId) {
        log.info("Getting payment filters for merchant: {}", merchantId);
        
        try {
            com.hyperswitch.common.dto.PaymentListFiltersResponse response = new com.hyperswitch.common.dto.PaymentListFiltersResponse();
            
            // Get available connectors from merchant connector accounts
            java.util.List<String> connectors = connectorService.getAvailableConnectors();
            return Mono.just(connectors)
                .flatMap(connectorList -> {
                    // Build connector map
                    java.util.Map<String, java.util.List<com.hyperswitch.common.dto.MerchantConnectorInfo>> connectorMap = new java.util.HashMap<>();
                    for (String connector : connectorList) {
                        connectorMap.put(connector, java.util.Collections.emptyList());
                    }
                    response.setConnector(connectorMap);
                    
                    // Set available currencies (common currencies)
                    response.setCurrency(java.util.Arrays.asList("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "SGD"));
                    
                    // Set available statuses
                    response.setStatus(java.util.Arrays.asList(
                        "requires_confirmation", "requires_customer_action", "processing",
                        "succeeded", "failed", "cancelled", "partially_captured"
                    ));
                    
                    // Set payment methods
                    java.util.Map<String, java.util.List<String>> paymentMethodMap = new java.util.HashMap<>();
                    paymentMethodMap.put("card", java.util.Arrays.asList("credit", "debit", "prepaid"));
                    paymentMethodMap.put("wallet", java.util.Arrays.asList("paypal", "apple_pay", "google_pay"));
                    response.setPaymentMethod(paymentMethodMap);
                    
                    // Set authentication types
                    response.setAuthenticationType(java.util.Arrays.asList("three_ds", "no_three_ds", "frictionless"));
                    
                    // Set card networks
                    response.setCardNetwork(java.util.Arrays.asList("visa", "mastercard", "amex", "discover", "jcb", "diners"));
                    
                    // Set card discovery methods
                    response.setCardDiscovery(java.util.Arrays.asList("manual", "automatic"));
                    
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentListFiltersResponse, PaymentError>ok(response));
                })
                .onErrorResume(error -> {
                    log.error("Error getting payment filters", error);
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentListFiltersResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_FILTERS_FAILED",
                            "Failed to get payment filters: " + error.getMessage())
                    ));
                });
        } catch (Exception e) {
            log.error("Error getting payment filters", e);
            return Mono.just(Result.<com.hyperswitch.common.dto.PaymentListFiltersResponse, PaymentError>err(
                PaymentError.of("PAYMENT_FILTERS_FAILED",
                    "Failed to get payment filters: " + e.getMessage())
            ));
        }
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsAggregateResponse, PaymentError>> getPaymentAggregates(
            String merchantId,
            java.time.Instant startTime,
            java.time.Instant endTime) {
        log.info("Getting payment aggregates for merchant: {} from {} to {}", merchantId, startTime, endTime);
        
        try {
            reactor.core.publisher.Flux<PaymentIntentEntity> query = paymentIntentRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, 10000);
            
            // Apply time range if provided
            if (startTime != null && endTime != null) {
                query = paymentIntentRepository.findByMerchantIdAndCreatedAtBetween(merchantId, startTime, endTime);
            }
            
            return query
                .collectList()
                .map(entities -> {
                    java.util.Map<String, Long> statusCounts = new java.util.HashMap<>();
                    
                    // Count by status
                    for (PaymentIntentEntity entity : entities) {
                        String status = entity.getStatus() != null ? entity.getStatus() : "unknown";
                        statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
                    }
                    
                    // Ensure all common statuses are present
                    java.util.List<String> commonStatuses = java.util.Arrays.asList(
                        "requires_confirmation", "requires_customer_action", "processing",
                        "succeeded", "failed", "cancelled", "partially_captured"
                    );
                    for (String status : commonStatuses) {
                        statusCounts.putIfAbsent(status, 0L);
                    }
                    
                    com.hyperswitch.common.dto.PaymentsAggregateResponse response = new com.hyperswitch.common.dto.PaymentsAggregateResponse();
                    response.setStatusWithCount(statusCounts);
                    return Result.<com.hyperswitch.common.dto.PaymentsAggregateResponse, PaymentError>ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error getting payment aggregates", error);
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsAggregateResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_AGGREGATES_FAILED",
                            "Failed to get payment aggregates: " + error.getMessage())
                    ));
                });
        } catch (Exception e) {
            log.error("Error getting payment aggregates", e);
            return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsAggregateResponse, PaymentError>err(
                PaymentError.of("PAYMENT_AGGREGATES_FAILED",
                    "Failed to get payment aggregates: " + e.getMessage())
            ));
        }
    }
    
    private java.util.Map<String, Object> paymentIntentToMap(PaymentIntent intent) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("paymentId", intent.getPaymentId().getValue());
        map.put("amount", intent.getAmount().getValue());
        map.put("currency", intent.getAmount().getCurrency());
        map.put("status", intent.getStatus().name());
        map.put("merchantId", intent.getMerchantId());
        map.put("customerId", intent.getCustomerId());
        map.put("paymentMethod", intent.getPaymentMethod() != null ? intent.getPaymentMethod().name() : null);
        map.put("createdAt", intent.getCreatedAt());
        map.put("metadata", intent.getMetadata());
        return map;
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.RefundListResponse, PaymentError>> listRefunds(
            String merchantId,
            com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        log.info("Listing refunds for merchant: {} with filters", merchantId);
        
        return refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .filter(refund -> matchesRefundFilters(refund, constraints))
            .collectList()
            .flatMap(refunds -> buildRefundListResponse(refunds, constraints))
            .onErrorResume(error -> {
                log.error("Error listing refunds", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.RefundListResponse, PaymentError>err(
                    PaymentError.of("REFUND_LIST_FAILED",
                        "Failed to list refunds: " + error.getMessage())
                ));
            });
    }
    
    /**
     * Check if refund matches filter constraints
     */
    private boolean matchesRefundFilters(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return matchesRefundIdFilter(refund, constraints)
            && matchesPaymentIdFilter(refund, constraints)
            && matchesStatusFilter(refund, constraints)
            && matchesConnectorFilter(refund, constraints)
            && matchesCurrencyFilter(refund, constraints)
            && matchesTimeRangeFilter(refund, constraints)
            && matchesAmountRangeFilter(refund, constraints);
    }
    
    private boolean matchesRefundIdFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return constraints.getRefundId() == null || constraints.getRefundId().isEmpty() 
            || constraints.getRefundId().equals(refund.getRefundId());
    }
    
    private boolean matchesPaymentIdFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return constraints.getPaymentId() == null || constraints.getPaymentId().isEmpty() 
            || constraints.getPaymentId().equals(refund.getPaymentId());
    }
    
    private boolean matchesStatusFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return constraints.getStatus() == null || constraints.getStatus().isEmpty() 
            || constraints.getStatus().equalsIgnoreCase(refund.getRefundStatus());
    }
    
    private boolean matchesConnectorFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return constraints.getConnector() == null || constraints.getConnector().isEmpty() 
            || constraints.getConnector().equalsIgnoreCase(refund.getConnector());
    }
    
    private boolean matchesCurrencyFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        return constraints.getCurrency() == null || constraints.getCurrency().isEmpty() 
            || constraints.getCurrency().equalsIgnoreCase(refund.getCurrency());
    }
    
    private boolean matchesTimeRangeFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        if (constraints.getStartTime() != null && refund.getCreatedAt() != null 
            && refund.getCreatedAt().isBefore(constraints.getStartTime())) {
            return false;
        }
        return constraints.getEndTime() == null || refund.getCreatedAt() == null 
            || !refund.getCreatedAt().isAfter(constraints.getEndTime());
    }
    
    private boolean matchesAmountRangeFilter(RefundEntity refund, com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        if (constraints.getMinAmount() != null && refund.getRefundAmount() != null 
            && refund.getRefundAmount() < constraints.getMinAmount()) {
            return false;
        }
        return constraints.getMaxAmount() == null || refund.getRefundAmount() == null 
            || refund.getRefundAmount() <= constraints.getMaxAmount();
    }
    
    /**
     * Build refund list response with pagination
     */
    private Mono<Result<com.hyperswitch.common.dto.RefundListResponse, PaymentError>> buildRefundListResponse(
            List<RefundEntity> refunds,
            com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        int totalCount = refunds.size();
        int offset = constraints.getOffset() != null ? constraints.getOffset() : 0;
        int limit = constraints.getLimit() != null ? constraints.getLimit() : 100;
        
        List<RefundEntity> paginatedRefunds = refunds.stream()
            .skip(offset)
            .limit(limit)
            .toList();
        
        List<Map<String, Object>> refundData = paginatedRefunds.stream()
            .map(this::refundEntityToMap)
            .toList();
        
        com.hyperswitch.common.dto.RefundListResponse response = new com.hyperswitch.common.dto.RefundListResponse();
        response.setData(refundData);
        response.setTotalCount((long) totalCount);
        response.setLimit(limit);
        response.setOffset(offset);
        
        return Mono.just(Result.<com.hyperswitch.common.dto.RefundListResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>> getRefundFilters(
            String merchantId) {
        log.info("Getting refund filters for merchant: {}", merchantId);
        
        return refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .collectList()
            .map(refunds -> {
                Set<String> connectors = new HashSet<>();
                Set<String> currencies = new HashSet<>();
                Set<String> statuses = new HashSet<>();
                
                for (RefundEntity refund : refunds) {
                    if (refund.getConnector() != null) {
                        connectors.add(refund.getConnector());
                    }
                    if (refund.getCurrency() != null) {
                        currencies.add(refund.getCurrency());
                    }
                    if (refund.getRefundStatus() != null) {
                        statuses.add(refund.getRefundStatus());
                    }
                }
                
                com.hyperswitch.common.dto.RefundFiltersResponse response = new com.hyperswitch.common.dto.RefundFiltersResponse();
                List<com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue> queryData = new ArrayList<>();
                
                // Add connector filter values
                if (!connectors.isEmpty()) {
                    com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue connectorFilter = 
                        new com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue();
                    connectorFilter.setDimension("connector");
                    connectorFilter.setValues(new ArrayList<>(connectors));
                    queryData.add(connectorFilter);
                }
                
                // Add currency filter values
                if (!currencies.isEmpty()) {
                    com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue currencyFilter = 
                        new com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue();
                    currencyFilter.setDimension("currency");
                    currencyFilter.setValues(new ArrayList<>(currencies));
                    queryData.add(currencyFilter);
                }
                
                // Add status filter values
                if (!statuses.isEmpty()) {
                    com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue statusFilter = 
                        new com.hyperswitch.common.dto.RefundFiltersResponse.FilterValue();
                    statusFilter.setDimension("status");
                    statusFilter.setValues(new ArrayList<>(statuses));
                    queryData.add(statusFilter);
                }
                
                response.setQueryData(queryData);
                
                return Result.<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error getting refund filters", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>err(
                    PaymentError.of("REFUND_FILTERS_FAILED",
                        "Failed to get refund filters: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> syncRefund(
            String refundId,
            String paymentId,
            String merchantId,
            Boolean forceSync) {
        log.info("Syncing refund: {} for payment: {}, merchant: {}, force: {}", refundId, paymentId, merchantId, forceSync);
        
        return refundRepository.findByRefundIdAndMerchantId(refundId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Refund not found")))
            .flatMap(refund -> syncRefundWithConnector(refund, refundId, paymentId, merchantId))
            .onErrorResume(error -> {
                log.error("Error syncing refund", error);
                String errorCode = "Refund not found".equals(error.getMessage())
                    ? "REFUND_NOT_FOUND"
                    : "REFUND_SYNC_FAILED";
                return Mono.just(Result.<Refund, PaymentError>err(
                    PaymentError.of(errorCode,
                        "Failed to sync refund: " + error.getMessage())
                ));
            });
    }
    
    /**
     * Sync refund with connector
     */
    private Mono<Result<Refund, PaymentError>> syncRefundWithConnector(
            RefundEntity refund,
            String refundId,
            String paymentId,
            String merchantId) {
        return connectorService.syncRefund(refundId, paymentId, merchantId)
            .flatMap(connectorResponse -> {
                if (connectorResponse.isOk()) {
                    return processSuccessfulRefundSync(refund, connectorResponse.unwrap());
                }
                return Mono.just(Result.<Refund, PaymentError>err(connectorResponse.unwrapErr()));
            });
    }
    
    /**
     * Process successful refund sync
     */
    private Mono<Result<Refund, PaymentError>> processSuccessfulRefundSync(
            RefundEntity refund,
            ConnectorResponse response) {
        String newStatus = response.getStatus() != null ? response.getStatus() : refund.getRefundStatus();
        
        if (newStatus != null && !newStatus.equals(refund.getRefundStatus())) {
            refund.setRefundStatus(newStatus);
            refund.setModifiedAt(Instant.now());
            return refundRepository.save(refund)
                .map(this::toRefundResponse)
                .map(Result::<Refund, PaymentError>ok);
        }
        return Mono.just(Result.<Refund, PaymentError>ok(toRefundResponse(refund)));
    }
    
    /**
     * Convert refund entity to refund response
     */
    private Refund toRefundResponse(RefundEntity refund) {
        return Refund.builder()
            .refundId(refund.getRefundId())
            .paymentId(refund.getPaymentId())
            .amount(com.hyperswitch.common.types.Amount.of(
                java.math.BigDecimal.valueOf(refund.getRefundAmount() != null ? refund.getRefundAmount() : 0)
                    .divide(java.math.BigDecimal.valueOf(100)),
                refund.getCurrency() != null ? refund.getCurrency() : "USD"))
            .status(refund.getRefundStatus())
            .connectorRefundId(refund.getConnectorRefundId())
            .createdAt(refund.getCreatedAt())
            .reason(refund.getRefundReason())
            .build();
    }
    
    private Map<String, Object> refundEntityToMap(RefundEntity refund) {
        Map<String, Object> map = new HashMap<>();
        map.put("refundId", refund.getRefundId());
        map.put("paymentId", refund.getPaymentId());
        map.put("merchantId", refund.getMerchantId());
        map.put("amount", refund.getRefundAmount());
        map.put("currency", refund.getCurrency());
        map.put("status", refund.getRefundStatus());
        map.put("connector", refund.getConnector());
        map.put("connectorTransactionId", refund.getConnectorTransactionId());
        map.put("reason", refund.getRefundReason());
        map.put("createdAt", refund.getCreatedAt());
        map.put("modifiedAt", refund.getModifiedAt());
        return map;
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> getRefund(
            String refundId,
            String merchantId) {
        log.info("Getting refund: {} for merchant: {}", refundId, merchantId);
        
        return refundRepository.findByRefundIdAndMerchantId(refundId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Refund not found")))
            .map(this::toRefundResponse)
            .map(Result::<Refund, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error getting refund", error);
                return Mono.just(Result.<Refund, PaymentError>err(
                    PaymentError.of("REFUND_NOT_FOUND",
                        "Refund not found: " + refundId)
                ));
            });
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> updateRefund(
            String refundId,
            String merchantId,
            UpdateRefundRequest request) {
        log.info("Updating refund: {} for merchant: {}", refundId, merchantId);
        
        return refundRepository.findByRefundIdAndMerchantId(refundId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Refund not found")))
            .flatMap(refund -> {
                if (request.getStatus() != null) {
                    refund.setRefundStatus(request.getStatus());
                }
                if (request.getReason() != null) {
                    refund.setRefundReason(request.getReason());
                }
                refund.setModifiedAt(Instant.now());
                return refundRepository.save(refund)
                    .map(this::toRefundResponse)
                    .map(Result::<Refund, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error updating refund", error);
                return Mono.just(Result.<Refund, PaymentError>err(
                    PaymentError.of("REFUND_UPDATE_FAILED",
                        "Failed to update refund: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<RefundAggregatesResponse, PaymentError>> getRefundAggregates(
            String merchantId,
            java.time.Instant startTime,
            java.time.Instant endTime) {
        log.info("Getting refund aggregates for merchant: {} from {} to {}", merchantId, startTime, endTime);
        
        Flux<RefundEntity> refundsFlux;
        if (startTime != null && endTime != null) {
            refundsFlux = refundRepository.findByMerchantIdAndCreatedAtBetween(merchantId, startTime, endTime);
        } else {
            refundsFlux = refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        }
        
        return refundsFlux
            .collectList()
            .map(refunds -> {
                Map<String, Long> statusCounts = new HashMap<>();
                long total = 0;
                
                for (RefundEntity refund : refunds) {
                    String status = refund.getRefundStatus() != null ? refund.getRefundStatus() : "UNKNOWN";
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
                    total++;
                }
                
                return new RefundAggregatesResponse(statusCounts, total);
            })
            .map(Result::<RefundAggregatesResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error getting refund aggregates", error);
                return Mono.just(Result.<RefundAggregatesResponse, PaymentError>err(
                    PaymentError.of("REFUND_AGGREGATES_FAILED",
                        "Failed to get refund aggregates: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> getPaymentByMerchantReferenceId(
            String merchantId,
            String merchantReferenceId) {
        log.info("Getting payment by merchant reference ID: {} for merchant: {}", merchantReferenceId, merchantId);
        
        // Search in metadata for merchant_reference_id
        return paymentIntentRepository.findAll()
            .filter(intent -> intent.getMerchantId().equals(merchantId))
            .filter(intent -> {
                if (intent.getMetadata() != null) {
                    Object refId = intent.getMetadata().get("merchant_reference_id");
                    return refId != null && refId.toString().equals(merchantReferenceId);
                }
                return false;
            })
            .next()
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error getting payment by merchant reference ID", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("PAYMENT_NOT_FOUND",
                        "Payment not found for merchant reference ID: " + merchantReferenceId)
                ));
            });
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> createRefundV2(
            String merchantId,
            com.hyperswitch.common.dto.RefundsCreateRequest request) {
        log.info("Creating refund (v2) for payment: {} for merchant: {}", request.getPaymentId(), merchantId);
        
        // Convert v2 request to internal RefundRequest
        RefundRequest refundRequest = RefundRequest.builder()
            .amount(request.getAmount())
            .reason(request.getReason())
            .build();
        
        PaymentId paymentId = PaymentId.of(request.getPaymentId());
        
        // Use existing refund logic
        return refundPayment(paymentId, refundRequest)
            .flatMap(refundResult -> {
                if (refundResult.isOk()) {
                    Refund refund = refundResult.unwrap();
                    
                    // Update refund with v2-specific fields if needed
                    // (merchant_reference_id, metadata, etc.)
                    return refundRepository.findByRefundIdAndMerchantId(refund.getRefundId(), merchantId)
                        .flatMap(refundEntity -> {
                            if (request.getMerchantReferenceId() != null) {
                                // Store merchant reference ID in metadata
                                if (refundEntity.getMetadata() == null) {
                                    refundEntity.setMetadata(new HashMap<>());
                                }
                                refundEntity.getMetadata().put("merchant_reference_id", request.getMerchantReferenceId());
                            }
                            
                            if (request.getMetadata() != null) {
                                if (refundEntity.getMetadata() == null) {
                                    refundEntity.setMetadata(new HashMap<>());
                                }
                                refundEntity.getMetadata().putAll(request.getMetadata());
                            }
                            
                            return refundRepository.save(refundEntity)
                                .map(this::toRefundResponse)
                                .map(Result::<Refund, PaymentError>ok);
                        })
                        .switchIfEmpty(Mono.just(refundResult));
                }
                return Mono.just(refundResult);
            });
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> retrieveRefundV2(
            String refundId,
            String merchantId,
            com.hyperswitch.common.dto.RefundsRetrieveRequest request) {
        log.info("Retrieving refund (v2): {} for merchant: {}", refundId, merchantId);
        
        // If force sync is requested, sync with connector first
        if (Boolean.TRUE.equals(request.getForceSync())) {
            return syncRefund(refundId, null, merchantId, true)
                .flatMap(syncResult -> {
                    if (syncResult.isOk()) {
                        return Mono.just(syncResult);
                    }
                    // If sync fails, still try to retrieve
                    return getRefund(refundId, merchantId);
                });
        }
        
        // Regular retrieval
        return getRefund(refundId, merchantId);
    }
    
    @Override
    public Mono<Result<Refund, PaymentError>> updateRefundMetadataV2(
            String refundId,
            String merchantId,
            com.hyperswitch.common.dto.RefundMetadataUpdateRequest request) {
        log.info("Updating refund metadata (v2): {} for merchant: {}", refundId, merchantId);
        
        return refundRepository.findByRefundIdAndMerchantId(refundId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Refund not found")))
            .flatMap(refund -> {
                if (request.getReason() != null) {
                    refund.setRefundReason(request.getReason());
                }
                
                if (request.getMetadata() != null) {
                    if (refund.getMetadata() == null) {
                        refund.setMetadata(new HashMap<>());
                    }
                    refund.getMetadata().putAll(request.getMetadata());
                }
                
                refund.setModifiedAt(Instant.now());
                
                return refundRepository.save(refund)
                    .map(this::toRefundResponse)
                    .map(Result::<Refund, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error updating refund metadata", error);
                String errorCode = "Refund not found".equals(error.getMessage())
                    ? "REFUND_NOT_FOUND"
                    : "REFUND_UPDATE_FAILED";
                return Mono.just(Result.<Refund, PaymentError>err(
                    PaymentError.of(errorCode,
                        "Failed to update refund metadata: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.RefundListResponse, PaymentError>> listRefundsV2(
            String merchantId,
            com.hyperswitch.common.dto.RefundListFilterConstraints constraints) {
        // v2 list refunds uses the same implementation as v1
        return listRefunds(merchantId, constraints);
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> createPaymentIntentV2(
            String merchantId,
            com.hyperswitch.common.dto.PaymentsCreateIntentRequest request) {
        log.info("Creating payment intent (v2) for merchant: {}", merchantId);
        
        // Convert v2 request to v1 CreatePaymentRequest using builder
        CreatePaymentRequest.Builder builder = CreatePaymentRequest.builder()
            .merchantId(merchantId)
            .confirm(false);
        
        if (request.getAmountDetails() != null && request.getAmountDetails().getAmount() != null) {
            builder.amount(request.getAmountDetails().getAmount());
        }
        
        if (request.getCustomerId() != null) {
            builder.customerId(request.getCustomerId());
        }
        
        if (request.getDescription() != null) {
            builder.description(request.getDescription());
        }
        
        if (request.getReturnUrl() != null) {
            builder.returnUrl(request.getReturnUrl());
        }
        
        if (request.getMetadata() != null) {
            builder.metadata(request.getMetadata());
        }
        
        if (request.getCaptureMethod() != null) {
            try {
                builder.captureMethod(com.hyperswitch.common.enums.CaptureMethod.valueOf(
                    request.getCaptureMethod().toUpperCase()));
            } catch (IllegalArgumentException _) {
                // Invalid capture method, will use default
            }
        }
        
        if (request.getSetupFutureUsage() != null && "off_session".equalsIgnoreCase(request.getSetupFutureUsage())) {
            // Setup future usage is handled via offSession field
            builder.offSession(true);
        }
        
        CreatePaymentRequest createRequest = builder.build();
        
        return createPayment(createRequest)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    // Get full entity to convert to response
                    return paymentIntentRepository.findByPaymentId(intent.getPaymentId().getValue())
                        .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                            convertPaymentIntentEntityToIntentResponse(entity)))
                        .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                            PaymentError.of("PAYMENT_NOT_FOUND", "Payment intent not found after creation"))));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                        result.unwrapErr()));
                }
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> getPaymentIntentV2(
            String paymentId,
            String merchantId) {
        log.info("Getting payment intent (v2): {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                convertPaymentIntentEntityToIntentResponse(entity)))
            .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG))))
            .onErrorResume(error -> {
                log.error("Error getting payment intent (v2)", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_GET_FAILED", "Failed to get payment intent: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> updatePaymentIntentV2(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.PaymentsUpdateIntentRequest request) {
        log.info("Updating payment intent (v2): {} for merchant: {}", paymentId, merchantId);
        
        // Get existing payment intent
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(entity -> {
                // Update fields from request
                if (request.getAmountDetails() != null && request.getAmountDetails().getAmount() != null) {
                    Long amountInMinorUnits = convertToMinorUnits(request.getAmountDetails().getAmount());
                    entity.setAmount(amountInMinorUnits);
                    if (request.getAmountDetails().getCurrency() != null) {
                        entity.setCurrency(request.getAmountDetails().getCurrency());
                    }
                }
                
                if (request.getDescription() != null) {
                    entity.setDescription(request.getDescription());
                }
                
                if (request.getReturnUrl() != null) {
                    entity.setReturnUrl(request.getReturnUrl());
                }
                
                if (request.getMetadata() != null) {
                    entity.setMetadata(request.getMetadata());
                }
                
                if (request.getCaptureMethod() != null) {
                    // Capture method is stored in metadata or as separate field
                    // For now, we'll store it in metadata
                    if (entity.getMetadata() == null) {
                        entity.setMetadata(new HashMap<>());
                    }
                    entity.getMetadata().put("capture_method", request.getCaptureMethod());
                }
                
                entity.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(entity)
                    .map(saved -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                        convertPaymentIntentEntityToIntentResponse(saved)))
                    .onErrorResume(error -> {
                        log.error("Error updating payment intent (v2)", error);
                        return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                            PaymentError.of("PAYMENT_UPDATE_FAILED", "Failed to update payment intent: " + error.getMessage())));
                    });
            })
            .onErrorResume(error -> {
                if (error instanceof RuntimeException && PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)));
                }
                log.error("Error updating payment intent (v2)", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                    PaymentError.of("PAYMENT_UPDATE_FAILED", "Failed to update payment intent: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> confirmPaymentIntentV2(
            String paymentId,
            String merchantId,
            ConfirmPaymentRequest request) {
        log.info("Confirming payment intent (v2): {} for merchant: {}", paymentId, merchantId);
        
        return confirmPayment(PaymentId.of(paymentId), request)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    // Get full entity to convert to response
                    return paymentIntentRepository.findByPaymentId(intent.getPaymentId().getValue())
                        .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                            convertPaymentIntentEntityToIntentResponse(entity)))
                        .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                            PaymentError.of("PAYMENT_NOT_FOUND", "Payment intent not found after confirmation"))));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                        result.unwrapErr()));
                }
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> createAndConfirmPaymentIntentV2(
            String merchantId,
            CreatePaymentRequest request) {
        log.info("Creating and confirming payment intent (v2) for merchant: {}", merchantId);
        
        // Ensure merchant ID is set and confirm is true
        CreatePaymentRequest.Builder builder = CreatePaymentRequest.builder()
            .merchantId(merchantId)
            .amount(request.getAmount())
            .confirm(true);
        
        if (request.getCustomerId() != null) {
            builder.customerId(request.getCustomerId());
        }
        if (request.getDescription() != null) {
            builder.description(request.getDescription());
        }
        if (request.getReturnUrl() != null) {
            builder.returnUrl(request.getReturnUrl());
        }
        if (request.getMetadata() != null) {
            builder.metadata(request.getMetadata());
        }
        if (request.getCaptureMethod() != null) {
            builder.captureMethod(request.getCaptureMethod());
        }
        if (request.getPaymentMethod() != null) {
            builder.paymentMethod(request.getPaymentMethod());
        }
        if (request.getOffSession() != null) {
            builder.offSession(request.getOffSession());
        }
        if (request.getRecurringDetails() != null) {
            builder.recurringDetails(request.getRecurringDetails());
        }
        
        CreatePaymentRequest createRequest = builder.build();
        
        return createPayment(createRequest)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    // Get full entity to convert to response
                    return paymentIntentRepository.findByPaymentId(intent.getPaymentId().getValue())
                        .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                            convertPaymentIntentEntityToIntentResponse(entity)))
                        .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                            PaymentError.of("PAYMENT_NOT_FOUND", "Payment intent not found after creation"))));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                        result.unwrapErr()));
                }
            });
    }
    
    /**
     * Convert PaymentIntentEntity to PaymentsIntentResponse
     */
    private com.hyperswitch.common.dto.PaymentsIntentResponse convertPaymentIntentEntityToIntentResponse(
            PaymentIntentEntity entity) {
        com.hyperswitch.common.dto.PaymentsIntentResponse response = 
            new com.hyperswitch.common.dto.PaymentsIntentResponse();
        
        response.setId(entity.getPaymentId());
        response.setStatus(entity.getStatus() != null ? entity.getStatus() : "UNKNOWN");
        response.setClientSecret(entity.getClientSecret());
        response.setMerchantId(entity.getMerchantId());
        response.setProfileId(entity.getProfileId());
        response.setCustomerId(entity.getCustomerId());
        response.setDescription(entity.getDescription());
        response.setReturnUrl(entity.getReturnUrl());
        response.setMetadata(entity.getMetadata());
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        
        // Set amount details
        if (entity.getAmount() != null) {
            com.hyperswitch.common.dto.AmountDetailsResponse amountDetails = 
                new com.hyperswitch.common.dto.AmountDetailsResponse();
            amountDetails.setAmount(Amount.of(
                java.math.BigDecimal.valueOf(entity.getAmount()).divide(java.math.BigDecimal.valueOf(100)),
                java.util.Currency.getInstance(entity.getCurrency() != null ? entity.getCurrency() : "USD")));
            amountDetails.setCurrency(entity.getCurrency());
            amountDetails.setAmountCaptured(entity.getAmountCaptured() != null ? entity.getAmountCaptured() : 0L);
            amountDetails.setAmountAuthorized(entity.getAmount());
            response.setAmountDetails(amountDetails);
        }
        
        // Payment attempts will be loaded separately if needed
        // For now, return response without attempts (can be loaded via separate endpoint)
        
        return response;
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsStartRedirectionResponse, PaymentError>> startPaymentRedirectionV2(
            String paymentId,
            String merchantId) {
        log.info("Starting payment redirection (v2): {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(entity -> {
                // Generate redirect URL based on payment status and connector
                String redirectUrl = generateRedirectUrl(entity);
                
                com.hyperswitch.common.dto.PaymentsStartRedirectionResponse response = 
                    new com.hyperswitch.common.dto.PaymentsStartRedirectionResponse();
                response.setRedirectUrl(redirectUrl);
                response.setPaymentId(paymentId);
                response.setStatus(entity.getStatus());
                response.setMessage("Redirect URL generated successfully");
                
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsStartRedirectionResponse, PaymentError>ok(response));
            })
            .onErrorResume(error -> {
                if (error instanceof RuntimeException && PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsStartRedirectionResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)));
                }
                log.error("Error starting payment redirection (v2)", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsStartRedirectionResponse, PaymentError>err(
                    PaymentError.of("REDIRECT_START_FAILED", "Failed to start payment redirection: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse, PaymentError>> finishPaymentRedirectionV2(
            String paymentId,
            String publishableKey,
            String profileId) {
        log.info("Finishing payment redirection (v2): {} with publishable key: {}", paymentId, publishableKey);
        
        // Get payment intent and update status based on redirect completion
        return paymentIntentRepository.findByPaymentId(paymentId)
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(entity -> {
                // Update payment status after redirect completion
                // In a real implementation, this would verify the redirect response from the connector
                entity.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(entity)
                    .flatMap(saved -> {
                        com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse response = 
                            new com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse();
                        response.setPaymentId(paymentId);
                        response.setStatus(saved.getStatus());
                        response.setMessage("Payment redirection completed successfully");
                        response.setPaymentIntent(convertPaymentIntentEntityToIntentResponse(saved));
                        
                        return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse, PaymentError>ok(response));
                    });
            })
            .onErrorResume(error -> {
                if (error instanceof RuntimeException && PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)));
                }
                log.error("Error finishing payment redirection (v2)", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsFinishRedirectionResponse, PaymentError>err(
                    PaymentError.of("REDIRECT_FINISH_FAILED", "Failed to finish payment redirection: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>> createExternalSdkTokensV2(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.PaymentsSessionRequest request) {
        log.info("Creating external SDK tokens (v2): {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(entity -> {
                // Generate session tokens for external SDKs (e.g., Stripe, PayPal)
                Map<String, String> sessionTokens = generateSessionTokens(entity, request.getConnector());
                
                com.hyperswitch.common.dto.PaymentsSessionResponse response = 
                    new com.hyperswitch.common.dto.PaymentsSessionResponse();
                response.setPaymentId(paymentId);
                response.setSessionTokens(sessionTokens);
                response.setStatus("success");
                response.setMessage("Session tokens generated successfully");
                
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>ok(response));
            })
            .onErrorResume(error -> {
                if (error instanceof RuntimeException && PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)));
                }
                log.error("Error creating external SDK tokens (v2)", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>err(
                    PaymentError.of("SESSION_TOKEN_CREATE_FAILED", "Failed to create session tokens: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse, PaymentError>> postSessionTokens(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.PaymentsPostSessionTokensRequest request) {
        log.info("Posting session tokens: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException(PAYMENT_NOT_FOUND_MSG)))
            .flatMap(entity -> {
                // Store session tokens in payment metadata
                if (entity.getMetadata() == null) {
                    entity.setMetadata(new HashMap<>());
                }
                entity.getMetadata().put("session_tokens", request.getSessionTokens());
                entity.setModifiedAt(Instant.now());
                
                return paymentIntentRepository.save(entity)
                    .map(saved -> {
                        com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse response = 
                            new com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse();
                        response.setPaymentId(paymentId);
                        response.setStatus("success");
                        response.setMessage("Session tokens posted successfully");
                        
                        return Result.<com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse, PaymentError>ok(response);
                    });
            })
            .onErrorResume(error -> {
                if (error instanceof RuntimeException && PAYMENT_NOT_FOUND_MSG.equals(error.getMessage())) {
                    return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse, PaymentError>err(
                        PaymentError.of("PAYMENT_NOT_FOUND", PAYMENT_NOT_FOUND_MSG)));
                }
                log.error("Error posting session tokens", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsPostSessionTokensResponse, PaymentError>err(
                    PaymentError.of("SESSION_TOKEN_POST_FAILED", "Failed to post session tokens: " + error.getMessage())));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>> createSessionTokens(
            String merchantId,
            com.hyperswitch.common.dto.PaymentsSessionRequest request) {
        log.info("Creating session tokens for merchant: {}", merchantId);
        
        // Generate session tokens for a new payment session
        Map<String, String> sessionTokens = generateSessionTokens(null, request.getConnector());
        
        com.hyperswitch.common.dto.PaymentsSessionResponse response = 
            new com.hyperswitch.common.dto.PaymentsSessionResponse();
        response.setPaymentId(request.getPaymentId());
        response.setSessionTokens(sessionTokens);
        response.setStatus("success");
        response.setMessage("Session tokens created successfully");
        
        return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsSessionResponse, PaymentError>ok(response));
    }
    
    /**
     * Generate redirect URL for payment
     */
    private String generateRedirectUrl(PaymentIntentEntity entity) {
        // In a real implementation, this would generate a redirect URL based on the connector
        // For now, return a placeholder URL
        String baseUrl = System.getenv().getOrDefault("PAYMENT_REDIRECT_BASE_URL", "https://api.hyperswitch.io");
        return baseUrl + "/redirect/" + entity.getPaymentId() + "/" + entity.getMerchantId();
    }
    
    /**
     * Generate session tokens for external SDKs
     */
    @SuppressWarnings("unused")
    private Map<String, String> generateSessionTokens(@SuppressWarnings("unused") PaymentIntentEntity entity, String connector) {
        Map<String, String> tokens = new HashMap<>();
        
        // Generate tokens based on connector type
        if (connector != null) {
            switch (connector.toLowerCase()) {
                case "stripe":
                    tokens.put("publishable_key", "pk_test_" + UUID.randomUUID().toString().replace("-", ""));
                    tokens.put("client_secret", "sk_test_" + UUID.randomUUID().toString().replace("-", ""));
                    break;
                case "paypal":
                    tokens.put("client_id", "client_" + UUID.randomUUID().toString().replace("-", ""));
                    tokens.put("access_token", "token_" + UUID.randomUUID().toString().replace("-", ""));
                    break;
                default:
                    tokens.put("session_token", UUID.randomUUID().toString());
                    break;
            }
        } else {
            tokens.put("session_token", UUID.randomUUID().toString());
        }
        
        return tokens;
    }
    
    // ========== Payment Redirect Flows (v1) ==========
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> handleRedirectResponse(
            String paymentId,
            String merchantId,
            String connector,
            java.util.Map<String, String> queryParams) {
        log.info("Handling redirect response for payment: {}, connector: {}", paymentId, connector);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    // In production, this would process the redirect response from the connector
                    return Mono.just(Result.<PaymentIntent, PaymentError>ok(intent));
                } else {
                    return Mono.just(result);
                }
            })
            .onErrorResume(error -> {
                log.error("Error handling redirect response", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("REDIRECT_RESPONSE_FAILED",
                        "Failed to handle redirect response: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> handleRedirectResponseWithCreds(
            String paymentId,
            String merchantId,
            String connector,
            String credsIdentifier,
            java.util.Map<String, String> queryParams) {
        log.info("Handling redirect response with creds for payment: {}, connector: {}, creds: {}", 
            paymentId, connector, credsIdentifier);
        return handleRedirectResponse(paymentId, merchantId, connector, queryParams);
    }
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> completeAuthorizationRedirect(
            String paymentId,
            String merchantId,
            String connector,
            java.util.Map<String, String> queryParams) {
        log.info("Completing authorization redirect for payment: {}, connector: {}", paymentId, connector);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    return Mono.just(Result.<PaymentIntent, PaymentError>ok(intent));
                } else {
                    return Mono.just(result);
                }
            })
            .onErrorResume(error -> {
                log.error("Error completing authorization redirect", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("AUTHORIZATION_REDIRECT_FAILED",
                        "Failed to complete authorization redirect: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> completeAuthorizationRedirectWithCreds(
            String paymentId,
            String merchantId,
            String connector,
            String credsIdentifier,
            java.util.Map<String, String> queryParams) {
        log.info("Completing authorization redirect with creds for payment: {}, connector: {}, creds: {}", 
            paymentId, connector, credsIdentifier);
        return completeAuthorizationRedirect(paymentId, merchantId, connector, queryParams);
    }
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> completeAuthorize(
            String paymentId,
            String merchantId,
            java.util.Map<String, Object> request) {
        log.info("Completing authorization for payment: {}", paymentId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    return Mono.just(Result.<PaymentIntent, PaymentError>ok(intent));
                } else {
                    return Mono.just(result);
                }
            })
            .onErrorResume(error -> {
                log.error("Error completing authorization", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("COMPLETE_AUTHORIZE_FAILED",
                        "Failed to complete authorization: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Manual Update ==========
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> manualUpdatePayment(
            String paymentId,
            String merchantId,
            java.util.Map<String, Object> updateRequest) {
        log.info("Manually updating payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .flatMap(entity -> {
                if (updateRequest.containsKey("status")) {
                    entity.setStatus(updateRequest.get("status").toString());
                }
                if (updateRequest.containsKey("amount")) {
                    Object amountObj = updateRequest.get("amount");
                    if (amountObj instanceof Number number) {
                        entity.setAmount(number.longValue());
                    }
                }
                entity.setModifiedAt(Instant.now());
                return paymentIntentRepository.save(entity)
                    .map(paymentMapper::toPaymentIntent)
                    .map(Result::<PaymentIntent, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error manually updating payment", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("MANUAL_UPDATE_FAILED",
                        "Failed to manually update payment: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Metadata Update ==========
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> updatePaymentMetadata(
            String paymentId,
            String merchantId,
            java.util.Map<String, Object> metadata) {
        log.info("Updating payment metadata for payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .flatMap(entity -> {
                if (entity.getMetadata() == null) {
                    entity.setMetadata(new HashMap<>());
                }
                entity.getMetadata().putAll(metadata);
                entity.setModifiedAt(Instant.now());
                return paymentIntentRepository.save(entity)
                    .map(paymentMapper::toPaymentIntent)
                    .map(Result::<PaymentIntent, PaymentError>ok);
            })
            .onErrorResume(error -> {
                log.error("Error updating payment metadata", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("METADATA_UPDATE_FAILED",
                        "Failed to update payment metadata: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Dynamic Tax Calculation ==========
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.TaxCalculationResponse, PaymentError>> calculateTax(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.TaxCalculationRequest request) {
        log.info("Calculating tax for payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    com.hyperswitch.common.dto.TaxCalculationResponse response = 
                        new com.hyperswitch.common.dto.TaxCalculationResponse();
                    response.setTotalTax(0L);
                    response.setCurrency(intent.getAmount().getCurrencyCode());
                    response.setTaxBreakdown(new java.util.ArrayList<>());
                    return Mono.just(Result.<com.hyperswitch.common.dto.TaxCalculationResponse, PaymentError>ok(response));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.TaxCalculationResponse, PaymentError>err(result.unwrapErr()));
                }
            })
            .onErrorResume(error -> {
                log.error("Error calculating tax", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.TaxCalculationResponse, PaymentError>err(
                    PaymentError.of("TAX_CALCULATION_FAILED",
                        "Failed to calculate tax: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Extended Card Info ==========
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.ExtendedCardInfoResponse, PaymentError>> getExtendedCardInfo(
            String paymentId,
            String merchantId) {
        log.info("Getting extended card info for payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    com.hyperswitch.common.dto.ExtendedCardInfoResponse response = 
                        new com.hyperswitch.common.dto.ExtendedCardInfoResponse();
                    response.setLast4("****");
                    response.setBrand("unknown");
                    response.setMetadata(new HashMap<>());
                    return Mono.just(Result.<com.hyperswitch.common.dto.ExtendedCardInfoResponse, PaymentError>ok(response));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.ExtendedCardInfoResponse, PaymentError>err(result.unwrapErr()));
                }
            })
            .onErrorResume(error -> {
                log.error("Error getting extended card info", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.ExtendedCardInfoResponse, PaymentError>err(
                    PaymentError.of("EXTENDED_CARD_INFO_FAILED",
                        "Failed to get extended card info: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Eligibility ==========
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.EligibilityResponse, PaymentError>> checkBalanceAndApplyPmData(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.EligibilityRequest request) {
        log.info("Checking balance and applying PM data for payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    com.hyperswitch.common.dto.EligibilityResponse response = 
                        new com.hyperswitch.common.dto.EligibilityResponse();
                    response.setEligible(true);
                    response.setAvailableBalance(intent.getAmount().getValue().longValue());
                    response.setCurrency(intent.getAmount().getCurrencyCode());
                    response.setMetadata(new HashMap<>());
                    return Mono.just(Result.<com.hyperswitch.common.dto.EligibilityResponse, PaymentError>ok(response));
                } else {
                    return Mono.just(Result.<com.hyperswitch.common.dto.EligibilityResponse, PaymentError>err(result.unwrapErr()));
                }
            })
            .onErrorResume(error -> {
                log.error("Error checking balance and applying PM data", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.EligibilityResponse, PaymentError>err(
                    PaymentError.of("ELIGIBILITY_CHECK_FAILED",
                        "Failed to check balance and apply PM data: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.EligibilityResponse, PaymentError>> submitEligibility(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.EligibilityRequest request) {
        log.info("Submitting eligibility for payment: {} for merchant: {}", paymentId, merchantId);
        return checkBalanceAndApplyPmData(paymentId, merchantId, request);
    }
    
    // ========== Payment Cancel Post Capture ==========
    
    @Override
    public Mono<Result<PaymentIntent, PaymentError>> cancelPostCapture(
            String paymentId,
            String merchantId,
            com.hyperswitch.common.dto.CancelPostCaptureRequest request) {
        log.info("Canceling post capture for payment: {} for merchant: {}", paymentId, merchantId);
        
        return paymentIntentRepository.findByPaymentIdAndMerchantId(paymentId, merchantId)
            .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
            .map(paymentMapper::toPaymentIntent)
            .map(Result::<PaymentIntent, PaymentError>ok)
            .flatMap(result -> {
                if (result.isOk()) {
                    PaymentIntent intent = result.unwrap();
                    return Mono.just(Result.<PaymentIntent, PaymentError>ok(intent));
                } else {
                    return Mono.just(result);
                }
            })
            .onErrorResume(error -> {
                log.error("Error canceling post capture", error);
                return Mono.just(Result.<PaymentIntent, PaymentError>err(
                    PaymentError.of("CANCEL_POST_CAPTURE_FAILED",
                        "Failed to cancel post capture: " + error.getMessage())
                ));
            });
    }
    
    // ========== Payment Profile Endpoints ==========
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>> listPaymentsForProfile(
            String merchantId,
            String profileId,
            Integer limit,
            Integer offset) {
        log.info("Listing payments for profile: {} for merchant: {}", profileId, merchantId);
        
        com.hyperswitch.common.dto.PaymentListFilterConstraints constraints = 
            new com.hyperswitch.common.dto.PaymentListFilterConstraints();
        constraints.setProfileId(profileId);
        constraints.setLimit(limit);
        constraints.setOffset(offset);
        return listPayments(merchantId, constraints);
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentListResponse, PaymentError>> listPaymentsForProfileWithFilters(
            String merchantId,
            com.hyperswitch.common.dto.PaymentListFilterConstraints constraints) {
        log.info("Listing payments for profile with filters for merchant: {}", merchantId);
        return listPayments(merchantId, constraints);
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentListFiltersResponse, PaymentError>> getPaymentFiltersForProfile(
            String merchantId,
            String profileId) {
        log.info("Getting payment filters for profile: {} for merchant: {}", profileId, merchantId);
        return getPaymentFilters(merchantId);
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.PaymentsAggregateResponse, PaymentError>> getPaymentAggregatesForProfile(
            String merchantId,
            String profileId,
            java.time.Instant startTime,
            java.time.Instant endTime) {
        log.info("Getting payment aggregates for profile: {} for merchant: {}", profileId, merchantId);
        return getPaymentAggregates(merchantId, startTime, endTime);
    }


     // ========== Payment Intent v2 API Additional Methods ==========
    
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> proxyConfirmIntent(
             String paymentId,
             String merchantId,
             java.util.Map<String, Object> request) {
         log.info("Proxy confirming intent for payment: {} for merchant: {}", paymentId, merchantId);
         // In production, this would proxy the confirmation through an external service
         return confirmPaymentIntentV2(paymentId, merchantId, null);
     }
     
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> confirmIntentWithExternalVaultProxy(
             String paymentId,
             String merchantId,
             java.util.Map<String, Object> request) {
         log.info("Confirming intent with external vault proxy for payment: {} for merchant: {}", paymentId, merchantId);
         // In production, this would confirm using an external vault proxy
         return confirmPaymentIntentV2(paymentId, merchantId, null);
     }
     
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> getRevenueRecoveryIntent(
             String paymentId,
             String merchantId) {
         log.info("Getting revenue recovery intent for payment: {} for merchant: {}", paymentId, merchantId);
         // In production, this would retrieve the revenue recovery intent
         return getPaymentIntentV2(paymentId, merchantId);
     }
     
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> getPaymentStatusV2(
             String paymentId,
             String merchantId,
             Boolean forceSync) {
         log.info("Getting payment status (v2) for payment: {} for merchant: {}, forceSync: {}", paymentId, merchantId, forceSync);
         // In production, this would sync with gateway credentials if forceSync is true
         if (Boolean.TRUE.equals(forceSync)) {
             com.hyperswitch.common.dto.SyncPaymentRequest syncRequest = new com.hyperswitch.common.dto.SyncPaymentRequest();
             syncRequest.setPaymentId(paymentId);
             syncRequest.setForceSync(true);
             return syncPayment(PaymentId.of(paymentId), syncRequest)
                 .flatMap(result -> {
                     if (result.isOk()) {
                         return getPaymentIntentV2(paymentId, merchantId);
                     } else {
                         return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(result.unwrapErr()));
                     }
                 });
         } else {
             return getPaymentIntentV2(paymentId, merchantId);
         }
     }
     
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> getPaymentIntentByMerchantReferenceIdV2(
             String merchantId,
             String merchantReferenceId) {
         log.info("Getting payment intent by merchant reference ID (v2): {} for merchant: {}", merchantReferenceId, merchantId);
         // Use the existing getPaymentByMerchantReferenceId and convert to v2 response
         return getPaymentByMerchantReferenceId(merchantId, merchantReferenceId)
             .flatMap(result -> {
                 if (result.isOk()) {
                     PaymentIntent intent = result.unwrap();
                     // Get entity from repository to convert to PaymentsIntentResponse
                     return paymentIntentRepository.findByPaymentId(intent.getPaymentId().getValue())
                         .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                             convertPaymentIntentEntityToIntentResponse(entity)))
                         .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                             PaymentError.of("PAYMENT_NOT_FOUND", "Payment intent not found"))));
                 } else {
                     return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(result.unwrapErr()));
                 }
             });
     }
     
     @Override
     public Mono<Result<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>> createRecoveryPayment(
             String merchantId,
             com.hyperswitch.common.dto.CreatePaymentRequest request) {
         log.info("Creating recovery payment for merchant: {}", merchantId);
         // In production, this would create a recovery payment for a failed payment
         // For now, use the standard create payment flow
         return createPayment(request)
             .flatMap(result -> {
                 if (result.isOk()) {
                     PaymentIntent intent = result.unwrap();
                     // Get entity from repository to convert to PaymentsIntentResponse
                     return paymentIntentRepository.findByPaymentId(intent.getPaymentId().getValue())
                         .map(entity -> Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>ok(
                             convertPaymentIntentEntityToIntentResponse(entity)))
                         .switchIfEmpty(Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(
                             PaymentError.of("PAYMENT_NOT_FOUND", "Payment intent not found after creation"))));
                 } else {
                     return Mono.just(Result.<com.hyperswitch.common.dto.PaymentsIntentResponse, PaymentError>err(result.unwrapErr()));
                 }
             });
     }
 }

