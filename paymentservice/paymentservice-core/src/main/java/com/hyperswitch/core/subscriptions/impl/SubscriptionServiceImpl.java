package com.hyperswitch.core.subscriptions.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.RecurringDetails;
import com.hyperswitch.common.dto.SubscriptionRequest;
import com.hyperswitch.common.dto.SubscriptionResponse;
import com.hyperswitch.common.dto.SubscriptionItemsResponse;
import com.hyperswitch.common.dto.SubscriptionEstimateResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Amount;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.common.types.SubscriptionId;
import com.hyperswitch.common.types.SubscriptionStatus;
import com.hyperswitch.core.payments.ConfirmPaymentRequest;
import com.hyperswitch.core.payments.PaymentIntent;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.subscriptions.SubscriptionService;
import com.hyperswitch.scheduler.ScheduledTask;
import com.hyperswitch.scheduler.SchedulerService;
import com.hyperswitch.storage.entity.SubscriptionEntity;
import com.hyperswitch.storage.repository.SubscriptionRepository;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of SubscriptionService
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private static final String SUBSCRIPTION_NOT_FOUND = "Subscription not found";
    private static final String TASK_TYPE_SUBSCRIPTION_BILLING = "subscription_billing";

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final SchedulerService schedulerService;

    @Value("${hyperswitch.subscription.default-billing-amount:100.00}")
    private BigDecimal defaultBillingAmount;

    @Value("${hyperswitch.subscription.default-currency:USD}")
    private String defaultCurrency;

    @Value("${hyperswitch.subscription.billing-cycle-days:30}")
    private int billingCycleDays;

    @Autowired
    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            ObjectMapper objectMapper,
            PaymentService paymentService,
            @Lazy SchedulerService schedulerService) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.schedulerService = schedulerService;
    }

    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> createSubscription(String merchantId, SubscriptionRequest request) {
        log.info("Creating subscription for merchant: {}, customer: {}", merchantId, request.getCustomerId());
        
        return Mono.fromCallable(() -> {
            SubscriptionId subscriptionId = SubscriptionId.generate();
            Instant now = Instant.now();
            
            SubscriptionEntity entity = new SubscriptionEntity();
            entity.setSubscriptionId(subscriptionId.getValue());
            entity.setMerchantId(merchantId);
            entity.setCustomerId(request.getCustomerId());
            entity.setPaymentMethodId(request.getPaymentMethodId());
            entity.setPlanId(request.getPlanId());
            entity.setItemPriceId(request.getItemPriceId());
            entity.setMerchantReferenceId(request.getMerchantReferenceId());
            entity.setBillingProcessor(request.getBillingProcessor());
            entity.setMerchantConnectorId(request.getMerchantConnectorId());
            entity.setProfileId(request.getProfileId());
            entity.setStatus(SubscriptionStatus.CREATED.name());
            entity.setCreatedAt(now);
            entity.setModifiedAt(now);
            
            // Generate client secret
            String clientSecret = "sub_" + subscriptionId.getValue() + "_secret_" + UUID.randomUUID().toString().replace("-", "");
            entity.setClientSecret(clientSecret);
            
            // Serialize metadata
            if (request.getMetadata() != null) {
                try {
                    entity.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
                } catch (Exception e) {
                    log.warn("Failed to serialize metadata", e);
                }
            }
            
            return entity;
        })
        .flatMap(subscriptionRepository::save)
        .map(SubscriptionMapper::toSubscriptionResponse)
        .map(Either::<PaymentError, SubscriptionResponse>right)
        .onErrorResume(error -> {
            log.error("Error creating subscription", error);
            return Mono.just(Either.left(PaymentError.of("SUBSCRIPTION_CREATION_FAILED", error.getMessage())));
        });
    }

    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> getSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Retrieving subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .map(SubscriptionMapper::toSubscriptionResponse)
            .map(Either::<PaymentError, SubscriptionResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }

    @Override
    public Flux<SubscriptionResponse> listSubscriptions(String merchantId) {
        log.info("Listing subscriptions for merchant: {}", merchantId);
        
        return subscriptionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .map(SubscriptionMapper::toSubscriptionResponse);
    }

    @Override
    public Flux<SubscriptionResponse> listSubscriptionsByCustomer(String merchantId, String customerId) {
        log.info("Listing subscriptions for customer: {} and merchant: {}", customerId, merchantId);
        
        return subscriptionRepository.findByMerchantIdAndCustomerId(merchantId, customerId)
            .map(SubscriptionMapper::toSubscriptionResponse);
    }

    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> updateSubscription(String merchantId, SubscriptionId subscriptionId, SubscriptionRequest request) {
        log.info("Updating subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                if (request.getPaymentMethodId() != null) {
                    subscription.setPaymentMethodId(request.getPaymentMethodId());
                }
                if (request.getPlanId() != null) {
                    subscription.setPlanId(request.getPlanId());
                }
                if (request.getItemPriceId() != null) {
                    subscription.setItemPriceId(request.getItemPriceId());
                }
                if (request.getMetadata() != null) {
                    try {
                        subscription.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize metadata", e);
                    }
                }
                subscription.setModifiedAt(Instant.now());
                
                return subscriptionRepository.save(subscription);
            })
            .map(SubscriptionMapper::toSubscriptionResponse)
            .map(Either::<PaymentError, SubscriptionResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }

    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> cancelSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Cancelling subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                subscription.setStatus(SubscriptionStatus.CANCELLED.name());
                subscription.setModifiedAt(Instant.now());
                return subscriptionRepository.save(subscription);
            })
            .map(SubscriptionMapper::toSubscriptionResponse)
            .map(Either::<PaymentError, SubscriptionResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }

    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> activateSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Activating subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE.name());
                subscription.setModifiedAt(Instant.now());
                return subscriptionRepository.save(subscription);
            })
            .map(SubscriptionMapper::toSubscriptionResponse)
            .map(Either::<PaymentError, SubscriptionResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> processBillingCycle(String merchantId, SubscriptionId subscriptionId) {
        log.info("Processing billing cycle for subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                // Check if subscription is active
                if (!SubscriptionStatus.ACTIVE.name().equals(subscription.getStatus())) {
                    log.warn("Cannot process billing for non-active subscription: {}", subscriptionId.getValue());
                    return Mono.just(Either.<PaymentError, SubscriptionResponse>left(
                        PaymentError.of("INVALID_STATUS", "Subscription is not active")));
                }
                
                // Validate payment method exists
                if (subscription.getPaymentMethodId() == null) {
                    log.error("Subscription {} has no payment method", subscriptionId.getValue());
                    return Mono.just(Either.<PaymentError, SubscriptionResponse>left(
                        PaymentError.of("MISSING_PAYMENT_METHOD", "Subscription has no payment method")));
                }
                
                // Get billing amount (in production, this would come from plan/item_price)
                // For now, use default amount or amount from metadata
                BigDecimal billingAmount = getBillingAmount(subscription);
                String currency = getCurrency(subscription);
                
                // Create MIT payment for subscription billing
                return createMitPaymentForSubscription(subscription, billingAmount, currency)
                    .flatMap(paymentResult -> {
                        if (paymentResult.isErr()) {
                            log.error("Failed to create payment for subscription {}: {}", 
                                subscriptionId.getValue(), paymentResult.unwrapErr().getMessage());
                            // Update subscription status on payment failure
                            subscription.setStatus(SubscriptionStatus.UNPAID.name());
                            subscription.setModifiedAt(Instant.now());
                            return subscriptionRepository.save(subscription)
                                .map(SubscriptionMapper::toSubscriptionResponse)
                                .map(response -> Either.<PaymentError, SubscriptionResponse>left(
                                    PaymentError.of("BILLING_FAILED", 
                                        "Failed to process billing: " + paymentResult.unwrapErr().getMessage())));
                        }
                        
                        // Payment created successfully
                        PaymentIntent paymentIntent = paymentResult.unwrap();
                        log.info("Payment {} created successfully for subscription {}", 
                            paymentIntent.getPaymentId().getValue(), subscriptionId.getValue());
                        
                        // Update subscription metadata with last billing info
                        updateSubscriptionBillingMetadata(subscription, paymentIntent.getPaymentId().getValue(), Instant.now());
                        subscription.setModifiedAt(Instant.now());
                        
                        return subscriptionRepository.save(subscription)
                            .map(SubscriptionMapper::toSubscriptionResponse)
                            .map(Either::<PaymentError, SubscriptionResponse>right);
                    });
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    /**
     * Create MIT payment for subscription billing
     */
    private Mono<Result<PaymentIntent, PaymentError>> createMitPaymentForSubscription(
            SubscriptionEntity subscription,
            BigDecimal amount,
            String currency) {
        log.info("Creating MIT payment for subscription: {}, amount: {} {}", 
            subscription.getSubscriptionId(), amount, currency);
        
        // Create payment request with off_session=true and recurring_details
        com.hyperswitch.common.dto.CreatePaymentRequest paymentRequest = 
            com.hyperswitch.common.dto.CreatePaymentRequest.builder()
                .merchantId(subscription.getMerchantId())
                .customerId(subscription.getCustomerId())
                .amount(Amount.of(amount, currency))
                .offSession(true)
                .description("Subscription billing for " + subscription.getSubscriptionId())
                .metadata(createSubscriptionPaymentMetadata(subscription))
                .build();
        
        // Create payment
        return paymentService.createPayment(paymentRequest)
            .flatMap(createResult -> {
                if (createResult.isErr()) {
                    return Mono.just(Result.<PaymentIntent, PaymentError>err(createResult.unwrapErr()));
                }
                
                PaymentIntent paymentIntent = createResult.unwrap();
                
                // Confirm payment with recurring_details for MIT
                RecurringDetails recurringDetails = RecurringDetails.paymentMethodId(subscription.getPaymentMethodId());
                
                ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
                    .offSession(true)
                    .recurringDetails(recurringDetails)
                    .build();
                
                return paymentService.confirmPayment(paymentIntent.getPaymentId(), confirmRequest);
            });
    }
    
    /**
     * Get billing amount from subscription (from metadata or use default)
     */
    private BigDecimal getBillingAmount(SubscriptionEntity subscription) {
        // In production, this would fetch from plan/item_price table
        // For now, check metadata or use default
        if (subscription.getMetadata() != null) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                    subscription.getMetadata(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                if (metadata.containsKey("billing_amount")) {
                    Object amountObj = metadata.get("billing_amount");
                    if (amountObj instanceof Number number) {
                        return BigDecimal.valueOf(number.doubleValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse billing amount from metadata", e);
            }
        }
        return defaultBillingAmount;
    }
    
    /**
     * Get currency from subscription (from metadata or use default)
     */
    private String getCurrency(SubscriptionEntity subscription) {
        // In production, this would fetch from plan/item_price table
        if (subscription.getMetadata() != null) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                    subscription.getMetadata(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                if (metadata.containsKey("currency") && metadata.get("currency") instanceof String currency) {
                    return currency;
                }
            } catch (Exception e) {
                log.warn("Failed to parse currency from metadata", e);
            }
        }
        return defaultCurrency;
    }
    
    /**
     * Create metadata for subscription payment
     */
    private Map<String, Object> createSubscriptionPaymentMetadata(SubscriptionEntity subscription) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("subscription_id", subscription.getSubscriptionId());
        metadata.put("billing_cycle", "recurring");
        if (subscription.getPlanId() != null) {
            metadata.put("plan_id", subscription.getPlanId());
        }
        if (subscription.getItemPriceId() != null) {
            metadata.put("item_price_id", subscription.getItemPriceId());
        }
        return metadata;
    }
    
    /**
     * Update subscription metadata with last billing information
     */
    private void updateSubscriptionBillingMetadata(SubscriptionEntity subscription, String paymentId, Instant billingDate) {
        try {
            Map<String, Object> metadata = subscription.getMetadata() != null
                ? objectMapper.readValue(subscription.getMetadata(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {})
                : new HashMap<>();
            
            metadata.put("last_billing_date", billingDate.toString());
            metadata.put("last_billing_payment_id", paymentId);
            
            subscription.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.warn("Failed to update subscription billing metadata", e);
        }
    }
    
    @Override
    public Mono<Either<PaymentError, String>> scheduleRecurringPayment(String merchantId, SubscriptionId subscriptionId, String cronExpression) {
        log.info("Scheduling recurring payment for subscription: {} with cron: {} for merchant: {}", 
            subscriptionId.getValue(), cronExpression, merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                // Calculate next billing date based on cron expression or default cycle
                Instant nextBillingDate = calculateNextBillingDate(subscription, cronExpression);
                
                // Create scheduled task for subscription billing
                String taskId = "sub_billing_" + subscriptionId.getValue() + "_" + UUID.randomUUID().toString().replace("-", "");
                
                Map<String, Object> taskData = new HashMap<>();
                taskData.put("merchant_id", merchantId);
                taskData.put("subscription_id", subscriptionId.getValue());
                taskData.put("customer_id", subscription.getCustomerId());
                taskData.put("payment_method_id", subscription.getPaymentMethodId());
                taskData.put("cron_expression", cronExpression);
                
                ScheduledTask task = ScheduledTask.builder()
                    .taskId(taskId)
                    .taskType(TASK_TYPE_SUBSCRIPTION_BILLING)
                    .scheduledAt(nextBillingDate)
                    .taskData(taskData)
                    .merchantId(merchantId)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();
                
                return schedulerService.scheduleTask(task)
                    .map(Either::<PaymentError, String>right)
                    .onErrorResume(error -> {
                        log.error("Failed to schedule recurring payment", error);
                        return Mono.just(Either.left(PaymentError.of("SCHEDULING_FAILED", error.getMessage())));
                    });
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    /**
     * Calculate next billing date based on subscription and cron expression
     * @param subscription the subscription entity
     * @param cronExpression the cron expression (currently unused, reserved for future implementation)
     * @return the next billing date
     */
    @SuppressWarnings("unused")
    private Instant calculateNextBillingDate(SubscriptionEntity subscription, String cronExpression) {
        // In production, this would parse cron expression and calculate next execution time
        // For now, use default billing cycle
        Instant lastBillingDate = getLastBillingDate(subscription);
        if (lastBillingDate == null) {
            // First billing - use subscription creation date + billing cycle
            return subscription.getCreatedAt().plusSeconds(billingCycleDays * 24L * 60L * 60L);
        } else {
            // Subsequent billing - use last billing date + billing cycle
            return lastBillingDate.plusSeconds(billingCycleDays * 24L * 60L * 60L);
        }
    }
    
    /**
     * Get last billing date from subscription metadata
     */
    private Instant getLastBillingDate(SubscriptionEntity subscription) {
        if (subscription.getMetadata() != null) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                    subscription.getMetadata(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                if (metadata.containsKey("last_billing_date")) {
                    String lastBillingDateStr = metadata.get("last_billing_date").toString();
                    return Instant.parse(lastBillingDateStr);
                }
            } catch (Exception e) {
                log.warn("Failed to parse last billing date from metadata", e);
            }
        }
        return null;
    }
    
    @Override
    public Mono<Void> executeScheduledBilling(String merchantId) {
        log.info("Executing scheduled billing for merchant: {}", merchantId);
        
        return subscriptionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .filter(subscription -> SubscriptionStatus.ACTIVE.name().equals(subscription.getStatus()))
            .filter(this::isSubscriptionDueForBilling)
            .flatMap(subscription -> {
                SubscriptionId subId = SubscriptionId.of(subscription.getSubscriptionId());
                return processBillingCycle(merchantId, subId)
                    .flatMap(result -> {
                        if (result.isLeft()) {
                            log.error("Error processing billing for subscription: {} - {}", 
                                subscription.getSubscriptionId(), result.getLeft().getMessage());
                            // Schedule retry if needed
                            scheduleBillingRetry(merchantId, subscription, result.getLeft());
                        } else {
                            log.info("Successfully processed billing for subscription: {}", 
                                subscription.getSubscriptionId());
                            // Schedule next billing cycle
                            scheduleNextBillingCycle(merchantId, subscription);
                        }
                        return Mono.empty();
                    })
                    .onErrorResume(error -> {
                        log.error("Error processing billing for subscription: {}", 
                            subscription.getSubscriptionId(), error);
                        scheduleBillingRetry(merchantId, subscription, 
                            PaymentError.of("BILLING_ERROR", error.getMessage()));
                        return Mono.empty(); // Continue with other subscriptions
                    });
            })
            .then();
    }
    
    /**
     * Check if subscription is due for billing
     */
    private boolean isSubscriptionDueForBilling(SubscriptionEntity subscription) {
        Instant lastBillingDate = getLastBillingDate(subscription);
        Instant now = Instant.now();
        
        if (lastBillingDate == null) {
            // First billing - check if subscription is old enough
            long daysSinceCreation = java.time.Duration.between(subscription.getCreatedAt(), now).toDays();
            return daysSinceCreation >= billingCycleDays;
        } else {
            // Subsequent billing - check if billing cycle has passed
            long daysSinceLastBilling = java.time.Duration.between(lastBillingDate, now).toDays();
            return daysSinceLastBilling >= billingCycleDays;
        }
    }
    
    /**
     * Schedule retry for failed billing
     */
    private void scheduleBillingRetry(String merchantId, SubscriptionEntity subscription, PaymentError error) {
        // Schedule retry with exponential backoff
        Instant retryDate = Instant.now().plusSeconds(3600); // Retry after 1 hour
        
        String taskId = "sub_billing_retry_" + subscription.getSubscriptionId() + "_" + UUID.randomUUID().toString().replace("-", "");
        
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("merchant_id", merchantId);
        taskData.put("subscription_id", subscription.getSubscriptionId());
        taskData.put("error", error.getMessage());
        
        ScheduledTask retryTask = ScheduledTask.builder()
            .taskId(taskId)
            .taskType(TASK_TYPE_SUBSCRIPTION_BILLING)
            .scheduledAt(retryDate)
            .taskData(taskData)
            .merchantId(merchantId)
            .retryCount(1)
            .maxRetries(3)
            .build();
        
        schedulerService.scheduleTask(retryTask)
            .doOnSuccess(id -> log.info("Scheduled billing retry for subscription: {}", subscription.getSubscriptionId()))
            .doOnError(e -> log.error("Failed to schedule billing retry", e))
            .subscribe();
    }
    
    /**
     * Schedule next billing cycle
     */
    private void scheduleNextBillingCycle(String merchantId, SubscriptionEntity subscription) {
        Instant nextBillingDate = calculateNextBillingDate(subscription, null);
        
        String taskId = "sub_billing_" + subscription.getSubscriptionId() + "_" + UUID.randomUUID().toString().replace("-", "");
        
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("merchant_id", merchantId);
        taskData.put("subscription_id", subscription.getSubscriptionId());
        taskData.put("customer_id", subscription.getCustomerId());
        taskData.put("payment_method_id", subscription.getPaymentMethodId());
        
        ScheduledTask task = ScheduledTask.builder()
            .taskId(taskId)
            .taskType(TASK_TYPE_SUBSCRIPTION_BILLING)
            .scheduledAt(nextBillingDate)
            .taskData(taskData)
            .merchantId(merchantId)
            .retryCount(0)
            .maxRetries(3)
            .build();
        
        schedulerService.scheduleTask(task)
            .doOnSuccess(id -> log.info("Scheduled next billing cycle for subscription: {}", subscription.getSubscriptionId()))
            .doOnError(e -> log.error("Failed to schedule next billing cycle", e))
            .subscribe();
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> pauseSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Pausing subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                if (!SubscriptionStatus.ACTIVE.name().equals(subscription.getStatus())) {
                    return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("INVALID_STATUS", 
                        "Subscription must be active to pause. Current status: " + subscription.getStatus())));
                }
                
                subscription.setStatus(SubscriptionStatus.PAUSED.name());
                subscription.setModifiedAt(Instant.now());
                
                return subscriptionRepository.save(subscription)
                    .map(SubscriptionMapper::toSubscriptionResponse)
                    .map(Either::<PaymentError, SubscriptionResponse>right)
                    .onErrorResume(error -> {
                        log.error("Failed to pause subscription", error);
                        return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("PAUSE_FAILED", error.getMessage())));
                    });
            })
            .switchIfEmpty(Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> resumeSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Resuming subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                if (!SubscriptionStatus.PAUSED.name().equals(subscription.getStatus())) {
                    return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("INVALID_STATUS", 
                        "Subscription must be paused to resume. Current status: " + subscription.getStatus())));
                }
                
                subscription.setStatus(SubscriptionStatus.ACTIVE.name());
                subscription.setModifiedAt(Instant.now());
                
                return subscriptionRepository.save(subscription)
                    .map(SubscriptionMapper::toSubscriptionResponse)
                    .map(Either::<PaymentError, SubscriptionResponse>right)
                    .onErrorResume(error -> {
                        log.error("Failed to resume subscription", error);
                        return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("RESUME_FAILED", error.getMessage())));
                    });
            })
            .switchIfEmpty(Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> confirmSubscription(String merchantId, SubscriptionId subscriptionId) {
        log.info("Confirming subscription: {} for merchant: {}", subscriptionId.getValue(), merchantId);
        
        return subscriptionRepository.findByMerchantIdAndSubscriptionId(merchantId, subscriptionId.getValue())
            .flatMap(subscription -> {
                if (!SubscriptionStatus.CREATED.name().equals(subscription.getStatus())) {
                    return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("INVALID_STATUS", 
                        "Subscription must be in CREATED status to confirm. Current status: " + subscription.getStatus())));
                }
                
                subscription.setStatus(SubscriptionStatus.ACTIVE.name());
                subscription.setModifiedAt(Instant.now());
                
                return subscriptionRepository.save(subscription)
                    .map(SubscriptionMapper::toSubscriptionResponse)
                    .map(Either::<PaymentError, SubscriptionResponse>right)
                    .onErrorResume(error -> {
                        log.error("Failed to confirm subscription", error);
                        return Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("CONFIRM_FAILED", error.getMessage())));
                    });
            })
            .switchIfEmpty(Mono.just(Either.<PaymentError, SubscriptionResponse>left(PaymentError.of("NOT_FOUND", SUBSCRIPTION_NOT_FOUND))));
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionResponse>> createAndConfirmSubscription(String merchantId, SubscriptionRequest request) {
        log.info("Creating and confirming subscription for merchant: {}, customer: {}", merchantId, request.getCustomerId());
        
        // First create the subscription
        return createSubscription(merchantId, request)
            .flatMap(result -> {
                if (result.isLeft()) {
                    return Mono.just(result);
                }
                
                SubscriptionResponse subscription = result.get();
                SubscriptionId subscriptionId = SubscriptionId.of(subscription.getSubscriptionId());
                
                // Then confirm it
                return confirmSubscription(merchantId, subscriptionId);
            });
    }
    
    @Override
    public Mono<Either<PaymentError, Flux<SubscriptionItemsResponse>>> getSubscriptionItems(
            String merchantId,
            String itemType,
            Integer limit,
            Integer offset) {
        log.info("Getting subscription items for merchant: {}, itemType: {}", merchantId, itemType);
        
        // In production, this would fetch items from a subscription items/plans repository
        // For now, return empty flux with a placeholder implementation
        Flux<SubscriptionItemsResponse> items = Flux.<SubscriptionItemsResponse>empty()
            .onErrorResume(error -> {
                log.error("Error getting subscription items", error);
                return Flux.<SubscriptionItemsResponse>empty();
            });
        
        return Mono.just(Either.<PaymentError, Flux<SubscriptionItemsResponse>>right(items))
            .onErrorResume(error -> {
                log.error("Error getting subscription items", error);
                return Mono.just(Either.<PaymentError, Flux<SubscriptionItemsResponse>>left(
                    PaymentError.of("SUBSCRIPTION_ITEMS_FAILED",
                        "Failed to get subscription items: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Either<PaymentError, SubscriptionEstimateResponse>> getSubscriptionEstimate(
            String merchantId,
            String itemPriceId,
            String planId,
            String couponCode) {
        log.info("Getting subscription estimate for merchant: {}, itemPriceId: {}, planId: {}", 
            merchantId, itemPriceId, planId);
        
        // In production, this would calculate the estimate based on plan, price, and coupon
        // For now, return a placeholder estimate
        SubscriptionEstimateResponse estimate = new SubscriptionEstimateResponse();
        estimate.setItemPriceId(itemPriceId);
        estimate.setPlanId(planId);
        estimate.setCouponCode(couponCode);
        estimate.setAmount(1000L); // Default amount in minor units
        estimate.setCurrency("USD");
        estimate.setLineItems(new java.util.ArrayList<>());
        
        return Mono.just(Either.<PaymentError, SubscriptionEstimateResponse>right(estimate))
            .onErrorResume(error -> {
                log.error("Error getting subscription estimate", error);
                return Mono.just(Either.<PaymentError, SubscriptionEstimateResponse>left(
                    PaymentError.of("SUBSCRIPTION_ESTIMATE_FAILED",
                        "Failed to get subscription estimate: " + error.getMessage())
                ));
            });
    }
}

