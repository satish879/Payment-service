package com.hyperswitch.scheduler.impl;

import com.hyperswitch.common.types.PaymentId;
import com.hyperswitch.common.types.SubscriptionId;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.payments.ConfirmPaymentRequest;
import com.hyperswitch.core.subscriptions.SubscriptionService;
import com.hyperswitch.core.revenuerecovery.RevenueRecoveryService;
import com.hyperswitch.scheduler.ScheduledTask;
import com.hyperswitch.scheduler.SchedulerService;
import com.hyperswitch.storage.entity.ScheduledTaskEntity;
import com.hyperswitch.storage.repository.ScheduledTaskRepository;
import com.hyperswitch.connectors.ConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of SchedulerService
 * Handles producer/consumer pattern for background jobs
 */
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final PaymentService paymentService;
    private final ConnectorService connectorService;
    private final SubscriptionService subscriptionService;
    private final RevenueRecoveryService revenueRecoveryService;
    
    @Value("${hyperswitch.scheduler.producer.batch-size:50}")
    private int batchSize;
    
    @Value("${hyperswitch.scheduler.producer.loop-interval-ms:30000}")
    private long producerInterval;
    
    @Value("${hyperswitch.scheduler.consumer.loop-interval-ms:30000}")
    private long consumerInterval;
    
    private static final String SCHEDULER_STREAM = "scheduler_stream";
    private static final String TASK_STATUS_PENDING = "pending";
    private static final String TASK_STATUS_PROCESSING = "processing";
    private static final String TASK_STATUS_COMPLETED = "completed";
    private static final String TASK_STATUS_FAILED = "failed";
    private static final String TASK_TYPE_PAYMENT_RETRY = "payment_retry";
    private static final String TASK_TYPE_WEBHOOK_RETRY = "webhook_retry";
    private static final String TASK_TYPE_REFUND_RETRY = "refund_retry";
    private static final String TASK_TYPE_PAYMENT_SYNC = "payment_sync";
    private static final String TASK_TYPE_SUBSCRIPTION_BILLING = "subscription_billing";
    private static final String TASK_TYPE_REVENUE_RECOVERY_RETRY = "REVENUE_RECOVERY_RETRY";
    private static final int BASE_RETRY_DELAY_SECONDS = 30;
    private static final int MAX_RETRY_DELAY_SECONDS = 3600; // 1 hour
    private static final String ERROR_CODE_INSUFFICIENT_FUNDS = "insufficient_funds";
    private static final String ERROR_CODE_CARD_DECLINED = "card_declined";
    private static final String ERROR_CODE_EXPIRED_CARD = "expired_card";
    private static final String ERROR_CODE_INVALID_CVC = "invalid_cvc";

    @Autowired
    public SchedulerServiceImpl(
            ReactiveRedisTemplate<String, String> redisTemplate,
            ScheduledTaskRepository scheduledTaskRepository,
            PaymentService paymentService,
            ConnectorService connectorService,
            @Lazy SubscriptionService subscriptionService,
            @Lazy RevenueRecoveryService revenueRecoveryService) {
        this.redisTemplate = redisTemplate;
        this.scheduledTaskRepository = scheduledTaskRepository;
        this.paymentService = paymentService;
        this.connectorService = connectorService;
        this.subscriptionService = subscriptionService;
        this.revenueRecoveryService = revenueRecoveryService;
    }

    @Override
    public Mono<Void> startProducer() {
        log.info("Starting scheduler producer");
        // Producer logic will run via @Scheduled annotation
        return Mono.empty();
    }

    @Override
    public Mono<Void> startConsumer() {
        log.info("Starting scheduler consumer");
        // Consumer logic will run via @Scheduled annotation
        return Mono.empty();
    }

    @Override
    public Mono<String> scheduleTask(ScheduledTask task) {
        log.info("Scheduling task: {}", task.getTaskId());
        
        // Create task entity
        ScheduledTaskEntity entity = new ScheduledTaskEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTaskId(task.getTaskId());
        entity.setTaskType(task.getTaskType());
        entity.setMerchantId(task.getTaskData().get("merchant_id") != null 
            ? task.getTaskData().get("merchant_id").toString() 
            : null);
        entity.setPaymentId(task.getTaskData().get("payment_id") != null 
            ? task.getTaskData().get("payment_id").toString() 
            : null);
        entity.setTaskData(task.getTaskData());
        entity.setScheduledAt(task.getScheduledAt());
        entity.setStatus(TASK_STATUS_PENDING);
        entity.setRetryCount(0);
        entity.setMaxRetries(3);
        entity.setCreatedAt(Instant.now());
        entity.setModifiedAt(Instant.now());
        
        // Save to database
        return scheduledTaskRepository.save(entity)
            .doOnSuccess(saved -> log.info("Task saved to database: {}", saved.getTaskId()))
            .thenReturn(task.getTaskId());
    }

    /**
     * Producer: Fetches scheduled tasks and adds them to Redis stream
     */
    @Scheduled(fixedDelayString = "${hyperswitch.scheduler.producer.loop-interval-ms}")
    public void producerLoop() {
        log.debug("Producer loop running");
        
        Instant now = Instant.now();
        scheduledTaskRepository
            .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(TASK_STATUS_PENDING, now)
            .onErrorResume(error -> {
                if (error.getMessage() != null && error.getMessage().contains("does not exist")) {
                    log.warn("scheduled_task table does not exist yet. Flyway migrations may not have run. Skipping producer loop.");
                    return Flux.empty();
                }
                log.error("Error in producer loop", error);
                return Flux.empty();
            })
            .take(batchSize)
            .flatMap(task -> {
                // Update status to processing
                task.setStatus(TASK_STATUS_PROCESSING);
                task.setModifiedAt(Instant.now());
                
                return scheduledTaskRepository.save(task)
                    .flatMap(saved -> {
                        // Add to Redis stream for consumer processing
                        Map<String, String> streamData = new java.util.HashMap<>();
                        streamData.put("task_id", saved.getTaskId());
                        streamData.put("task_type", saved.getTaskType());
                        streamData.put("scheduled_at", saved.getScheduledAt().toString());
                        streamData.put("entity_id", saved.getId());
                        
                        @SuppressWarnings({"unchecked", "null"})
                        Map<Object, Object> streamDataForRedis = (Map<Object, Object>) (Map<?, ?>) streamData;
                        return redisTemplate.opsForStream()
                            .add(SCHEDULER_STREAM, streamDataForRedis)
                            .doOnSuccess(recordId -> log.debug("Task added to Redis stream: {}", saved.getTaskId()))
                            .thenReturn(saved);
                    });
            })
            .doOnComplete(() -> log.debug("Producer loop completed"))
            .subscribe();
    }

    /**
     * Consumer: Reads from Redis stream and executes tasks
     */
    @Scheduled(fixedDelayString = "${hyperswitch.scheduler.consumer.loop-interval-ms}")
    public void consumerLoop() {
        log.debug("Consumer loop running");
        
        // Read pending tasks from database that are in processing status
        // In production, this would read from Redis stream with consumer groups
        scheduledTaskRepository
            .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(TASK_STATUS_PROCESSING, Instant.now())
            .onErrorResume(error -> {
                if (error.getMessage() != null && error.getMessage().contains("does not exist")) {
                    log.warn("scheduled_task table does not exist yet. Flyway migrations may not have run. Skipping consumer loop.");
                    return Flux.empty();
                }
                log.error("Error in consumer loop", error);
                return Flux.empty();
            })
            .take(batchSize)
            .flatMap(task -> {
                log.debug("Processing task: {}", task.getTaskId());
                
                // Execute task based on type
                return executeTask(task)
                    .then(Mono.defer(() -> {
                        task.setStatus(TASK_STATUS_COMPLETED);
                        task.setExecutedAt(Instant.now());
                        task.setModifiedAt(Instant.now());
                        return scheduledTaskRepository.save(task)
                            .doOnSuccess(saved -> log.info("Task completed: {}", task.getTaskId()))
                            .doOnError(error -> log.error("Error saving completed task: {}", task.getTaskId(), error))
                            .then();
                    }))
                    .onErrorResume(error -> {
                        log.error("Task execution failed: {}", task.getTaskId(), error);
                        task.setRetryCount(task.getRetryCount() + 1);
                        task.setErrorMessage(error.getMessage());
                        task.setModifiedAt(Instant.now());
                        
                        if (task.getRetryCount() >= task.getMaxRetries()) {
                            task.setStatus(TASK_STATUS_FAILED);
                        } else {
                            task.setStatus(TASK_STATUS_PENDING);
                            // Calculate exponential backoff delay
                            long delaySeconds = calculateExponentialBackoff(task.getRetryCount());
                            task.setScheduledAt(Instant.now().plusSeconds(delaySeconds));
                        }
                        
                        return scheduledTaskRepository.save(task).then();
                    });
            })
            .doOnComplete(() -> log.debug("Consumer loop completed"))
            .subscribe();
    }
    
    /**
     * Execute a scheduled task based on its type
     */
    private Mono<Void> executeTask(ScheduledTaskEntity task) {
        log.info("Executing task: {} of type: {}", task.getTaskId(), task.getTaskType());
        
        String taskType = task.getTaskType();
        if (TASK_TYPE_PAYMENT_RETRY.equals(taskType)) {
            return executePaymentRetry(task);
        } else if (TASK_TYPE_WEBHOOK_RETRY.equals(taskType)) {
            return executeWebhookRetry(task);
        } else if (TASK_TYPE_REFUND_RETRY.equals(taskType)) {
            return executeRefundRetry(task);
        } else if (TASK_TYPE_PAYMENT_SYNC.equals(taskType)) {
            return executePaymentSync(task);
        } else if (TASK_TYPE_SUBSCRIPTION_BILLING.equals(taskType)) {
            return executeSubscriptionBilling(task);
        } else if (TASK_TYPE_REVENUE_RECOVERY_RETRY.equals(taskType)) {
            return executeRevenueRecoveryRetry(task);
        } else {
            log.warn("Unknown task type: {}", taskType);
            return Mono.empty();
        }
    }
    
    private Mono<Void> executePaymentRetry(ScheduledTaskEntity task) {
        log.info("Executing payment retry for task: {} (attempt {})", task.getTaskId(), task.getRetryCount() + 1);
        
        if (task.getPaymentId() == null) {
            log.warn("Payment retry task missing payment ID: {}", task.getTaskId());
            return Mono.empty();
        }
        
        try {
            PaymentId paymentId = PaymentId.of(task.getPaymentId());
            
            // Get payment details from task data
            Map<String, Object> taskData = task.getTaskData();
            if (taskData == null || !taskData.containsKey("confirm_request")) {
                log.warn("Payment retry task missing confirm request data: {}", task.getTaskId());
                return Mono.empty();
            }
            
            // Check for hard decline from previous attempt
            if (isHardDeclineFromTask(task)) {
                log.warn("Payment {} has hard decline error. Skipping retry.", paymentId);
                return Mono.empty();
            }
            
            // Get payment and retry confirmation
            return paymentService.getPayment(paymentId)
                .flatMap(result -> {
                    if (result.isOk()) {
                        return retryPaymentConfirmation(paymentId, taskData, task);
                    } else {
                        log.warn("Payment not found for retry: {}", paymentId);
                        return Mono.empty();
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error executing payment retry for task: {}", task.getTaskId(), error);
                    return Mono.empty();
                });
        } catch (Exception e) {
            log.error("Invalid payment ID in retry task: {}", task.getPaymentId(), e);
            return Mono.empty();
        }
    }
    
    private boolean isHardDeclineFromTask(ScheduledTaskEntity task) {
        String lastErrorCode = task.getErrorMessage();
        return lastErrorCode != null && isHardDecline(lastErrorCode);
    }
    
    private Mono<Void> retryPaymentConfirmation(
            PaymentId paymentId, 
            Map<String, Object> taskData, 
            ScheduledTaskEntity task) {
        log.info("Retrying payment confirmation for: {}", paymentId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> confirmRequestData = (Map<String, Object>) taskData.get("confirm_request");
        ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
            .paymentMethodData(confirmRequestData != null ? confirmRequestData : new java.util.HashMap<>())
            .build();
        
        return paymentService.confirmPayment(paymentId, confirmRequest)
            .flatMap(confirmResult -> {
                if (confirmResult.isErr()) {
                    return handlePaymentRetryError(confirmResult.unwrapErr(), paymentId, task);
                }
                log.info("Payment retry succeeded for: {}", paymentId);
                return Mono.empty();
            });
    }
    
    private Mono<Void> handlePaymentRetryError(
            com.hyperswitch.common.errors.PaymentError error, 
            PaymentId paymentId, 
            ScheduledTaskEntity task) {
        String errorCode = error.getCode();
        if (isHardDecline(errorCode)) {
            log.warn("Payment {} failed with hard decline: {}", paymentId, errorCode);
            task.setStatus(TASK_STATUS_FAILED);
            task.setErrorMessage("Hard decline: " + errorCode);
            return scheduledTaskRepository.save(task).then();
        }
        // Soft decline - will retry
        return Mono.error(new RuntimeException("Payment confirmation failed: " + errorCode));
    }
    
    private Mono<Void> executeWebhookRetry(ScheduledTaskEntity task) {
        log.info("Executing webhook retry for task: {} (attempt {})", task.getTaskId(), task.getRetryCount() + 1);
        
        Map<String, Object> taskData = task.getTaskData();
        if (taskData == null) {
            log.warn("Webhook retry task missing data: {}", task.getTaskId());
            return Mono.empty();
        }
        
        String connector = (String) taskData.get("connector");
        String payload = (String) taskData.get("payload");
        String signature = (String) taskData.get("signature");
        String secret = (String) taskData.get("secret");
        
        if (connector == null || payload == null) {
            log.warn("Webhook retry task missing required data: {}", task.getTaskId());
            return Mono.empty();
        }
        
        // Track webhook delivery attempt
        @SuppressWarnings("unchecked")
        Map<String, Object> deliveryTrackingRaw = (Map<String, Object>) taskData.get("delivery_tracking");
        final Map<String, Object> deliveryTracking = deliveryTrackingRaw != null 
            ? deliveryTrackingRaw 
            : new java.util.HashMap<>();
        if (deliveryTrackingRaw == null) {
            taskData.put("delivery_tracking", deliveryTracking);
        }
        
        Integer attemptCount = (Integer) deliveryTracking.getOrDefault("attempt_count", 0);
        deliveryTracking.put("attempt_count", attemptCount + 1);
        deliveryTracking.put("last_attempt_at", Instant.now().toString());
        
        // Retry webhook processing
        String signatureValue = signature != null ? signature : "";
        String secretValue = secret != null ? secret : "";
        return connectorService.getConnector(connector)
            .verifyWebhook(payload, signatureValue, secretValue)
            .flatMap(verified -> {
                if (Boolean.TRUE.equals(verified)) {
                    return connectorService.getConnector(connector)
                        .parseWebhook(payload)
                        .doOnSuccess(webhookPayload -> {
                            // Mark delivery as successful
                            deliveryTracking.put("delivered_at", Instant.now().toString());
                            deliveryTracking.put("status", "delivered");
                            log.info("Webhook retry succeeded for task: {}", task.getTaskId());
                        })
                        .then();
                } else {
                    log.warn("Webhook verification failed in retry: {}", task.getTaskId());
                    deliveryTracking.put("status", "verification_failed");
                    return Mono.empty();
                }
            })
            .onErrorResume(error -> {
                log.error("Error executing webhook retry for task: {}", task.getTaskId(), error);
                deliveryTracking.put("status", "failed");
                deliveryTracking.put("last_error", error.getMessage());
                return Mono.empty();
            });
    }
    
    private Mono<Void> executeRefundRetry(ScheduledTaskEntity task) {
        log.info("Executing refund retry for task: {} (attempt {})", task.getTaskId(), task.getRetryCount() + 1);
        
        if (task.getPaymentId() == null) {
            log.warn("Refund retry task missing payment ID: {}", task.getTaskId());
            return Mono.empty();
        }
        
        try {
            PaymentId paymentId = PaymentId.of(task.getPaymentId());
            
            // Get refund request from task data
            Map<String, Object> taskData = task.getTaskData();
            if (taskData == null) {
                log.warn("Refund retry task missing data: {}", task.getTaskId());
                return Mono.empty();
            }
            
            // Reconstruct RefundRequest from task data
            @SuppressWarnings("unchecked")
            Map<String, Object> refundData = (Map<String, Object>) taskData.get("refund_request");
            if (refundData == null) {
                log.warn("Refund retry task missing refund request data: {}", task.getTaskId());
                return Mono.empty();
            }
            
            // Extract refund amount and reason
            String reason = (String) refundData.get("reason");
            @SuppressWarnings("unchecked")
            Map<String, Object> amountData = (Map<String, Object>) refundData.get("amount");
            
            com.hyperswitch.common.types.Amount amount = null;
            if (amountData != null) {
                java.math.BigDecimal value = new java.math.BigDecimal(amountData.get("value").toString());
                String currency = (String) amountData.get("currencyCode");
                amount = com.hyperswitch.common.types.Amount.of(value, currency);
            }
            
            com.hyperswitch.core.payments.RefundRequest refundRequest = 
                com.hyperswitch.core.payments.RefundRequest.builder()
                    .amount(amount)
                    .reason(reason)
                    .build();
            
            log.info("Processing refund retry for payment: {}", paymentId);
            return paymentService.refundPayment(paymentId, refundRequest)
                .flatMap(refundResult -> {
                    if (refundResult.isErr()) {
                        log.warn("Refund retry failed for payment {}: {}", paymentId, refundResult.unwrapErr().getMessage());
                        return Mono.<Void>error(new RuntimeException("Refund failed: " + refundResult.unwrapErr().getCode()));
                    }
                    log.info("Refund retry succeeded for payment: {}", paymentId);
                    return Mono.<Void>empty();
                })
                .onErrorResume(error -> {
                    log.error("Error executing refund retry for task: {}", task.getTaskId(), error);
                    return Mono.empty();
                });
        } catch (Exception e) {
            log.error("Invalid payment ID in refund retry task: {}", task.getPaymentId(), e);
            return Mono.empty();
        }
    }

    /**
     * Execute payment sync job - sync payment status with connector
     */
    private Mono<Void> executePaymentSync(ScheduledTaskEntity task) {
        log.info("Executing payment sync for task: {}", task.getTaskId());
        
        if (task.getPaymentId() == null) {
            log.warn("Payment sync task missing payment ID: {}", task.getTaskId());
            return Mono.empty();
        }
        
        try {
            PaymentId paymentId = PaymentId.of(task.getPaymentId());
            
            // Get payment to check current status
            return paymentService.getPayment(paymentId)
                .flatMap(result -> {
                    if (result.isOk()) {
                        log.info("Syncing payment status for: {}", paymentId);
                        
                        // In a full implementation, we would call connector's sync method
                        // For now, we log the sync attempt
                        // The actual sync would update payment status from connector
                        log.debug("Payment sync completed for: {}", paymentId);
                        return Mono.<Void>empty();
                    } else {
                        log.warn("Payment not found for sync: {}", paymentId);
                        return Mono.<Void>empty();
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error executing payment sync for task: {}", task.getTaskId(), error);
                    return Mono.empty();
                });
        } catch (Exception e) {
            log.error("Invalid payment ID in sync task: {}", task.getPaymentId(), e);
            return Mono.empty();
        }
    }
    
    /**
     * Execute subscription billing task
     */
    private Mono<Void> executeSubscriptionBilling(ScheduledTaskEntity task) {
        log.info("Executing subscription billing for task: {}", task.getTaskId());
        
        Map<String, Object> taskData = task.getTaskData();
        if (taskData == null) {
            log.warn("Subscription billing task missing task data: {}", task.getTaskId());
            return Mono.empty();
        }
        
        String merchantId = task.getMerchantId();
        String subscriptionIdStr = (String) taskData.get("subscription_id");
        
        if (merchantId == null || subscriptionIdStr == null) {
            log.warn("Subscription billing task missing merchant_id or subscription_id: {}", task.getTaskId());
            return Mono.empty();
        }
        
        try {
            SubscriptionId subscriptionId = SubscriptionId.of(subscriptionIdStr);
            
            return subscriptionService.processBillingCycle(merchantId, subscriptionId)
                .flatMap(result -> {
                    if (result.isLeft()) {
                        log.error("Subscription billing failed for subscription: {} - {}", 
                            subscriptionIdStr, result.getLeft().getMessage());
                    } else {
                        log.info("Subscription billing completed successfully for subscription: {}", subscriptionIdStr);
                    }
                    return Mono.<Void>empty();
                })
                .onErrorResume(error -> {
                    log.error("Error executing subscription billing for task: {}", task.getTaskId(), error);
                    return Mono.empty();
                });
        } catch (Exception e) {
            log.error("Invalid subscription ID in billing task: {}", subscriptionIdStr, e);
            return Mono.empty();
        }
    }
    
    private Mono<Void> executeRevenueRecoveryRetry(ScheduledTaskEntity task) {
        log.info("Executing revenue recovery retry for task: {}", task.getTaskId());
        
        Map<String, Object> taskData = task.getTaskData();
        if (taskData == null) {
            log.warn("Revenue recovery retry task missing task data: {}", task.getTaskId());
            return Mono.empty();
        }
        
        String merchantId = task.getMerchantId();
        String recoveryId = (String) taskData.get("recovery_id");
        
        if (merchantId == null || recoveryId == null) {
            log.warn("Revenue recovery retry task missing merchant_id or recovery_id: {}", task.getTaskId());
            return Mono.empty();
        }
        
        return revenueRecoveryService.executeRecoveryWorkflow(merchantId, recoveryId)
            .flatMap(result -> {
                if (result.isLeft()) {
                    log.error("Revenue recovery workflow failed for recovery: {} - {}", 
                        recoveryId, result.getLeft().getMessage());
                } else {
                    log.info("Revenue recovery workflow completed successfully for recovery: {}", recoveryId);
                }
                return Mono.<Void>empty();
            })
            .onErrorResume(error -> {
                log.error("Error executing revenue recovery retry for task: {}", task.getTaskId(), error);
                return Mono.empty();
            });
    }
    
    /**
     * Calculate exponential backoff delay in seconds
     * Formula: baseDelay * (2 ^ retryCount), capped at maxDelay
     */
    private long calculateExponentialBackoff(int retryCount) {
        long delay = BASE_RETRY_DELAY_SECONDS * (long) Math.pow(2, retryCount);
        return Math.min(delay, MAX_RETRY_DELAY_SECONDS);
    }
    
    /**
     * Check if error code indicates a hard decline (non-retryable)
     */
    private boolean isHardDecline(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        String lowerErrorCode = errorCode.toLowerCase();
        return lowerErrorCode.contains(ERROR_CODE_INSUFFICIENT_FUNDS) ||
               lowerErrorCode.contains(ERROR_CODE_CARD_DECLINED) ||
               lowerErrorCode.contains(ERROR_CODE_EXPIRED_CARD) ||
               lowerErrorCode.contains(ERROR_CODE_INVALID_CVC);
    }

    @Override
    public Mono<Void> stop() {
        log.info("Stopping scheduler");
        return Mono.empty();
    }
}

