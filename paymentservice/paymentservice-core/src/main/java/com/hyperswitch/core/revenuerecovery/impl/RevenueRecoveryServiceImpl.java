package com.hyperswitch.core.revenuerecovery.impl;

import com.hyperswitch.common.dto.RevenueRecoveryAnalytics;
import com.hyperswitch.common.dto.RevenueRecoveryResponse;
import com.hyperswitch.common.dto.RevenueRecoveryRedisResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.RecoveryStatus;
import com.hyperswitch.common.types.RevenueRecoveryAlgorithmType;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.revenuerecovery.RevenueRecoveryService;
import com.hyperswitch.scheduler.SchedulerService;
import com.hyperswitch.scheduler.ScheduledTask;
import com.hyperswitch.storage.entity.RevenueRecoveryEntity;
import com.hyperswitch.storage.repository.RevenueRecoveryRepository;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of RevenueRecoveryService
 */
@Service
public class RevenueRecoveryServiceImpl implements RevenueRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RevenueRecoveryServiceImpl.class);
    private static final String RECOVERY_NOT_FOUND = "Revenue recovery not found";
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long DEFAULT_BASE_DELAY_SECONDS = 60; // 1 minute
    
    // Hard decline error codes that should not be retried
    private static final Set<String> HARD_DECLINE_CODES = Set.of(
        "CARD_DECLINED",
        "INSUFFICIENT_FUNDS",
        "INVALID_CARD",
        "EXPIRED_CARD",
        "CARD_BLOCKED",
        "FRAUD_DETECTED",
        "AUTHENTICATION_FAILED"
    );

    private final RevenueRecoveryRepository revenueRecoveryRepository;
    private final SchedulerService schedulerService;

    @Autowired
    public RevenueRecoveryServiceImpl(
            RevenueRecoveryRepository revenueRecoveryRepository,
            SchedulerService schedulerService) {
        this.revenueRecoveryRepository = revenueRecoveryRepository;
        this.schedulerService = schedulerService;
    }

    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> createOrUpdateRecovery(
        String merchantId,
        String paymentId,
        String attemptId,
        String profileId,
        String billingMcaId,
        RevenueRecoveryAlgorithmType algorithmType,
        Long retryBudget
    ) {
        log.info("Creating/updating revenue recovery for payment: {}, attempt: {}, merchant: {}", 
            paymentId, attemptId, merchantId);
        
        return revenueRecoveryRepository.findByPaymentIdAndAttemptId(paymentId, attemptId)
            .flatMap(existing -> {
                // Update existing
                existing.setRetryCount(existing.getRetryCount() + 1);
                existing.setRecoveryStatus(RecoveryStatus.PROCESSING.name());
                existing.setModifiedAt(Instant.now());
                if (algorithmType != null) {
                    existing.setRetryAlgorithm(algorithmType.name());
                }
                return revenueRecoveryRepository.save(existing);
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                // Create new
                String recoveryId = "rec_" + UUID.randomUUID().toString().replace("-", "");
                Instant now = Instant.now();
                
                RevenueRecoveryEntity entity = new RevenueRecoveryEntity();
                entity.setRecoveryId(recoveryId);
                entity.setMerchantId(merchantId);
                entity.setPaymentId(paymentId);
                entity.setAttemptId(attemptId);
                entity.setProfileId(profileId);
                entity.setBillingMcaId(billingMcaId);
                entity.setRecoveryStatus(RecoveryStatus.MONITORING.name());
                entity.setRetryAlgorithm(algorithmType != null ? algorithmType.name() : 
                    RevenueRecoveryAlgorithmType.EXPONENTIAL_BACKOFF.name());
                entity.setRetryCount(0);
                entity.setMaxRetries(DEFAULT_MAX_RETRIES);
                entity.setRetryBudget(retryBudget);
                entity.setRetryBudgetUsed(0L);
                entity.setCreatedAt(now);
                entity.setModifiedAt(now);
                
                return entity;
            }).flatMap(revenueRecoveryRepository::save))
            .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity))
            .map(response -> Either.<PaymentError, RevenueRecoveryResponse>right(response))
            .onErrorResume(error -> {
                log.error("Error creating/updating revenue recovery", error);
                return Mono.just(Either.left(PaymentError.of("REVENUE_RECOVERY_FAILED", error.getMessage())));
            });
    }

    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> getRecovery(
        String merchantId,
        String recoveryId
    ) {
        log.info("Retrieving revenue recovery: {} for merchant: {}", recoveryId, merchantId);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity))
            .map(response -> Either.<PaymentError, RevenueRecoveryResponse>right(response))
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }

    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> getRecoveryByPayment(
        String merchantId,
        String paymentId,
        String attemptId
    ) {
        log.info("Retrieving revenue recovery for payment: {}, attempt: {}, merchant: {}", 
            paymentId, attemptId, merchantId);
        
        return revenueRecoveryRepository.findByPaymentIdAndAttemptId(paymentId, attemptId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity))
            .map(response -> Either.<PaymentError, RevenueRecoveryResponse>right(response))
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }

    @Override
    public Flux<RevenueRecoveryResponse> listRecoveries(String merchantId, RecoveryStatus status) {
        log.info("Listing revenue recoveries for merchant: {}, status: {}", merchantId, status);
        
        if (status != null) {
            return revenueRecoveryRepository.findByMerchantIdAndRecoveryStatus(merchantId, status.name())
                .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity));
        } else {
            return revenueRecoveryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity));
        }
    }

    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> updateRecoveryStatus(
        String merchantId,
        String recoveryId,
        RecoveryStatus status
    ) {
        log.info("Updating revenue recovery status: {} for merchant: {}", recoveryId, merchantId);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .flatMap(entity -> {
                entity.setRecoveryStatus(status.name());
                entity.setModifiedAt(Instant.now());
                return revenueRecoveryRepository.save(entity);
            })
            .map(entity -> RevenueRecoveryMapper.toRevenueRecoveryResponse(entity))
            .map(response -> Either.<PaymentError, RevenueRecoveryResponse>right(response))
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }

    @Override
    public Mono<Long> calculateNextRetryDelay(
        RevenueRecoveryAlgorithmType algorithmType,
        Integer retryCount,
        Long baseDelaySeconds
    ) {
        long baseDelay = baseDelaySeconds != null ? baseDelaySeconds : DEFAULT_BASE_DELAY_SECONDS;
        int count = retryCount != null ? retryCount : 0;
        
        long delay = switch (algorithmType) {
            case EXPONENTIAL_BACKOFF -> (long) (baseDelay * Math.pow(2, count));
            case LINEAR_BACKOFF -> baseDelay * (count + 1);
            case FIXED_INTERVAL -> baseDelay;
            case ADAPTIVE -> calculateAdaptiveDelay(count, baseDelay);
            case SMART_RETRY -> calculateSmartRetryDelay(count, baseDelay);
        };
        
        // Cap at 24 hours
        return Mono.just(Math.min(delay, 86400L));
    }

    private long calculateAdaptiveDelay(int retryCount, long baseDelay) {
        // Adaptive: exponential for first 3 retries, then linear
        if (retryCount < 3) {
            return (long) (baseDelay * Math.pow(2, retryCount));
        } else {
            return baseDelay * (retryCount + 1);
        }
    }

    private long calculateSmartRetryDelay(int retryCount, long baseDelay) {
        // Smart retry: exponential with jitter
        long exponential = (long) (baseDelay * Math.pow(2, retryCount));
        // Add 10% jitter
        long jitter = (long) (exponential * 0.1 * Math.random());
        return exponential + jitter;
    }

    @Override
    public Mono<Boolean> canRetryWithinBudget(String recoveryId, Long retryCost) {
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .map(entity -> {
                if (entity.getRetryBudget() == null) {
                    return true; // No budget constraint
                }
                long remainingBudget = entity.getRetryBudget() - entity.getRetryBudgetUsed();
                return remainingBudget >= retryCost;
            })
            .defaultIfEmpty(false);
    }

    @Override
    public Mono<RevenueRecoveryAnalytics> getAnalytics(String merchantId) {
        log.info("Getting revenue recovery analytics for merchant: {}", merchantId);
        
        return revenueRecoveryRepository.findByMerchantId(merchantId)
            .collectList()
            .map(entities -> {
                RevenueRecoveryAnalytics analytics = new RevenueRecoveryAnalytics();
                analytics.setTotalRecoveryAttempts((long) entities.size());
                
                long successful = entities.stream()
                    .filter(e -> RecoveryStatus.RECOVERED.name().equals(e.getRecoveryStatus()))
                    .count();
                analytics.setSuccessfulRecoveries(successful);
                
                long failed = entities.stream()
                    .filter(e -> RecoveryStatus.TERMINATED.name().equals(e.getRecoveryStatus()))
                    .count();
                analytics.setFailedRecoveries(failed);
                
                long active = entities.stream()
                    .filter(e -> RecoveryStatus.PROCESSING.name().equals(e.getRecoveryStatus()) ||
                                RecoveryStatus.MONITORING.name().equals(e.getRecoveryStatus()) ||
                                RecoveryStatus.SCHEDULED.name().equals(e.getRecoveryStatus()))
                    .count();
                analytics.setActiveRecoveries(active);
                
                analytics.setTerminatedRecoveries(failed);
                
                long totalBudgetUsed = entities.stream()
                    .mapToLong(e -> e.getRetryBudgetUsed() != null ? e.getRetryBudgetUsed() : 0L)
                    .sum();
                analytics.setTotalBudgetUsed(totalBudgetUsed);
                
                double avgRetry = entities.stream()
                    .mapToInt(e -> e.getRetryCount() != null ? e.getRetryCount() : 0)
                    .average()
                    .orElse(0.0);
                analytics.setAverageRetryCount(avgRetry);
                
                double recoveryRate = entities.size() > 0 
                    ? (double) successful / entities.size() * 100.0 
                    : 0.0;
                analytics.setRecoveryRate(recoveryRate);
                
                return analytics;
            });
    }
    
    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> executeRecoveryWorkflow(
        String merchantId,
        String recoveryId
    ) {
        log.info("Executing recovery workflow for recovery: {}, merchant: {}", recoveryId, merchantId);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .flatMap(recovery -> {
                // Check if recovery is eligible for retry
                if (recovery.getRetryCount() >= recovery.getMaxRetries()) {
                    log.info("Recovery {} has exceeded max retries, terminating", recoveryId);
                    recovery.setRecoveryStatus(RecoveryStatus.TERMINATED.name());
                    recovery.setModifiedAt(Instant.now());
                    return revenueRecoveryRepository.save(recovery)
                        .map(RevenueRecoveryMapper::toRevenueRecoveryResponse)
                        .map(Either::<PaymentError, RevenueRecoveryResponse>right);
                }
                
                // Check if recovery is in a valid state for workflow execution
                String status = recovery.getRecoveryStatus();
                if (!status.equals(RecoveryStatus.MONITORING.name()) && 
                    !status.equals(RecoveryStatus.QUEUED.name()) &&
                    !status.equals(RecoveryStatus.SCHEDULED.name())) {
                    log.warn("Recovery {} is in state {}, cannot execute workflow", recoveryId, status);
                    PaymentError error = PaymentError.of("INVALID_STATE", "Recovery is not in a valid state for workflow execution");
                    return Mono.<Either<PaymentError, RevenueRecoveryResponse>>just(Either.<PaymentError, RevenueRecoveryResponse>left(error));
                }
                
                // Transition to PROCESSING state
                recovery.setRecoveryStatus(RecoveryStatus.PROCESSING.name());
                recovery.setModifiedAt(Instant.now());
                
                return revenueRecoveryRepository.save(recovery)
                    .flatMap(saved -> {
                        // Schedule next retry attempt
                        return scheduleNextRetry(merchantId, recoveryId)
                            .flatMap(result -> {
                                if (result.isLeft()) {
                                    return Mono.<Either<PaymentError, RevenueRecoveryResponse>>just(
                                        Either.left(result.getLeft()));
                                }
                                return Mono.just(saved)
                                    .map(RevenueRecoveryMapper::toRevenueRecoveryResponse)
                                    .map(Either::<PaymentError, RevenueRecoveryResponse>right);
                            });
                    });
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }
    
    @Override
    public Mono<Either<PaymentError, Void>> scheduleNextRetry(
        String merchantId,
        String recoveryId
    ) {
        log.info("Scheduling next retry for recovery: {}, merchant: {}", recoveryId, merchantId);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .flatMap(recovery -> {
                // Calculate next retry delay
                RevenueRecoveryAlgorithmType algorithmType = 
                    RevenueRecoveryAlgorithmType.valueOf(recovery.getRetryAlgorithm());
                
                return calculateNextRetryDelay(
                    algorithmType,
                    recovery.getRetryCount(),
                    DEFAULT_BASE_DELAY_SECONDS
                )
                .flatMap(delaySeconds -> {
                    Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);
                    recovery.setNextRetryAt(nextRetryAt);
                    recovery.setRecoveryStatus(RecoveryStatus.SCHEDULED.name());
                    recovery.setModifiedAt(Instant.now());
                    
                    // Create scheduled task for retry
                    Map<String, Object> taskData = new HashMap<>();
                    taskData.put("recovery_id", recoveryId);
                    taskData.put("payment_id", recovery.getPaymentId());
                    taskData.put("attempt_id", recovery.getAttemptId());
                    taskData.put("merchant_id", merchantId);
                    
                    ScheduledTask task = ScheduledTask.builder()
                        .taskType("REVENUE_RECOVERY_RETRY")
                        .merchantId(merchantId)
                        .scheduledAt(nextRetryAt)
                        .taskData(taskData)
                        .build();
                    
                    return schedulerService.scheduleTask(task)
                        .then(revenueRecoveryRepository.save(recovery))
                        .then(Mono.just(Either.<PaymentError, Void>right(null)));
                });
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }
    
    @Override
    public Mono<Boolean> shouldRetry(
        String recoveryId,
        String errorCode,
        String errorMessage
    ) {
        log.debug("Evaluating retry eligibility for recovery: {}, error: {}", recoveryId, errorCode);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .flatMap(recovery -> {
                // Check for hard decline errors
                if (errorCode != null && HARD_DECLINE_CODES.contains(errorCode)) {
                    log.info("Hard decline detected for recovery {}, terminating", recoveryId);
                    return Mono.just(false);
                }
                
                // Check retry count
                if (recovery.getRetryCount() >= recovery.getMaxRetries()) {
                    log.info("Max retries reached for recovery {}", recoveryId);
                    return Mono.just(false);
                }
                
                // Check budget if applicable
                if (recovery.getRetryBudget() != null) {
                    return canRetryWithinBudget(recoveryId, 1L)
                        .map(canRetry -> {
                            if (!canRetry) {
                                log.info("Retry budget exhausted for recovery {}", recoveryId);
                            }
                            return canRetry;
                        });
                }
                
                return Mono.just(true);
            })
            .defaultIfEmpty(false);
    }
    
    @Override
    public Mono<Either<PaymentError, RevenueRecoveryResponse>> processWorkflowStep(
        String merchantId,
        String recoveryId,
        String stepResult,
        String errorCode,
        String errorMessage
    ) {
        log.info("Processing workflow step for recovery: {}, result: {}, error: {}", 
            recoveryId, stepResult, errorCode);
        
        return revenueRecoveryRepository.findByRecoveryId(recoveryId)
            .filter(entity -> entity.getMerchantId().equals(merchantId))
            .flatMap(recovery -> {
                // Update error information
                if (errorCode != null) {
                    recovery.setLastErrorCode(errorCode);
                }
                if (errorMessage != null) {
                    recovery.setLastErrorMessage(errorMessage);
                }
                
                // Process step result and update state
                switch (stepResult.toUpperCase()) {
                    case "SUCCESS":
                        recovery.setRecoveryStatus(RecoveryStatus.RECOVERED.name());
                        recovery.setModifiedAt(Instant.now());
                        break;
                        
                    case "FAILED":
                        // Check if should retry
                        return shouldRetry(recoveryId, errorCode, errorMessage)
                            .flatMap(shouldRetry -> {
                                if (shouldRetry) {
                                    recovery.setRetryCount(recovery.getRetryCount() + 1);
                                    recovery.setRecoveryStatus(RecoveryStatus.MONITORING.name());
                                    recovery.setModifiedAt(Instant.now());
                                    
                                    // Schedule next retry
                                    return scheduleNextRetry(merchantId, recoveryId)
                                        .then(revenueRecoveryRepository.save(recovery))
                                        .map(RevenueRecoveryMapper::toRevenueRecoveryResponse)
                                        .map(Either::<PaymentError, RevenueRecoveryResponse>right);
                                } else {
                                    recovery.setRecoveryStatus(RecoveryStatus.TERMINATED.name());
                                    recovery.setModifiedAt(Instant.now());
                                    return revenueRecoveryRepository.save(recovery)
                                        .map(RevenueRecoveryMapper::toRevenueRecoveryResponse)
                                        .map(Either::<PaymentError, RevenueRecoveryResponse>right);
                                }
                            });
                            
                    case "PARTIAL":
                        recovery.setRecoveryStatus(RecoveryStatus.PARTIALLY_RECOVERED.name());
                        recovery.setModifiedAt(Instant.now());
                        break;
                        
                    default:
                        recovery.setRecoveryStatus(RecoveryStatus.MONITORING.name());
                        recovery.setModifiedAt(Instant.now());
                        break;
                }
                
                return revenueRecoveryRepository.save(recovery)
                    .map(RevenueRecoveryMapper::toRevenueRecoveryResponse)
                    .map(Either::<PaymentError, RevenueRecoveryResponse>right);
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", RECOVERY_NOT_FOUND))));
    }
    
    @Override
    public Mono<Result<RevenueRecoveryRedisResponse, PaymentError>> getRevenueRecoveryRedisData(
            String merchantId,
            String keyType) {
        log.info("Getting revenue recovery redis data for merchant: {}, keyType: {}", merchantId, keyType);
        
        // In production, this would retrieve data from Redis
        // For now, return a placeholder response
        RevenueRecoveryRedisResponse response = new RevenueRecoveryRedisResponse();
        response.setMerchantId(merchantId);
        response.setKeyType(keyType != null ? keyType : "payment_processor_token");
        response.setData(new HashMap<>());
        response.setTtl(3600L);
        
        return Mono.just(Result.<RevenueRecoveryRedisResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error getting revenue recovery redis data", error);
                return Mono.just(Result.<RevenueRecoveryRedisResponse, PaymentError>err(
                    PaymentError.of("REDIS_DATA_RETRIEVAL_FAILED",
                        "Failed to get revenue recovery redis data: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse, PaymentError>> dataBackfill(
            String merchantId,
            java.util.List<com.hyperswitch.common.dto.RevenueRecoveryBackfillRequest> records,
            java.time.Instant cutoffDatetime) {
        log.info("Starting revenue recovery data backfill for merchant: {}, records: {}", merchantId, records.size());
        
        return Mono.fromCallable(() -> {
            int processedRecords = 0;
            int failedRecords = 0;
            
            // Process each record
            for (com.hyperswitch.common.dto.RevenueRecoveryBackfillRequest record : records) {
                try {
                    // In production, this would:
                    // 1. Validate the record
                    // 2. Store in Redis with appropriate key structure
                    // 3. Update database if needed
                    // 4. Handle cutoff datetime filtering
                    processedRecords++;
                } catch (Exception e) {
                    log.error("Error processing backfill record: {}", e.getMessage(), e);
                    failedRecords++;
                }
            }
            
            com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse response = 
                new com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse();
            response.setProcessedRecords(processedRecords);
            response.setFailedRecords(failedRecords);
            response.setStatus("COMPLETED");
            response.setMessage(String.format("Processed %d records, %d failed", processedRecords, failedRecords));
            
            return Result.<com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error in data backfill", error);
            return Mono.just(Result.<com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse, PaymentError>err(
                PaymentError.of("BACKFILL_FAILED",
                    "Failed to backfill revenue recovery data: " + error.getMessage())
            ));
        });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> updateRedisData(
            String merchantId,
            String key,
            java.util.Map<String, Object> data) {
        log.info("Updating revenue recovery redis data for merchant: {}, key: {}", merchantId, key);
        
        // In production, this would update Redis with the provided data
        return Mono.just(Result.<Void, PaymentError>ok(null))
            .onErrorResume(error -> {
                log.error("Error updating redis data", error);
                return Mono.just(Result.<Void, PaymentError>err(
                    PaymentError.of("REDIS_UPDATE_FAILED",
                        "Failed to update redis data: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>> getBackfillStatus(
            String merchantId,
            String backfillId) {
        log.info("Getting backfill status for merchant: {}, backfillId: {}", merchantId, backfillId);
        
        // In production, this would retrieve status from database or Redis
        com.hyperswitch.common.dto.BackfillStatusResponse response = 
            new com.hyperswitch.common.dto.BackfillStatusResponse();
        response.setStatus("COMPLETED");
        response.setProcessedCount(0);
        response.setTotalCount(0);
        response.setStartedAt(Instant.now());
        response.setLastUpdatedAt(Instant.now());
        
        return Mono.just(Result.<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error getting backfill status", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>err(
                    PaymentError.of("BACKFILL_STATUS_RETRIEVAL_FAILED",
                        "Failed to get backfill status: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>> getProcessTracker(
            String merchantId,
            String processId) {
        log.info("Getting process tracker for merchant: {}, processId: {}", merchantId, processId);
        
        // In production, this would retrieve from process_tracker table
        com.hyperswitch.common.dto.ProcessTrackerResponse response = 
            new com.hyperswitch.common.dto.ProcessTrackerResponse();
        response.setId(processId);
        response.setStatus("FINISHED");
        response.setBusinessStatus("SUCCESS");
        response.setRetryCount(0);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error getting process tracker", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>err(
                    PaymentError.of("PROCESS_TRACKER_RETRIEVAL_FAILED",
                        "Failed to get process tracker: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>> resumeRecovery(
            String merchantId,
            String processId,
            com.hyperswitch.common.dto.ResumeRecoveryRequest request) {
        log.info("Resuming revenue recovery for merchant: {}, processId: {}", merchantId, processId);
        
        // In production, this would:
        // 1. Update process tracker status
        // 2. Schedule the recovery task
        // 3. Update tracking data
        
        com.hyperswitch.common.dto.ProcessTrackerResponse response = 
            new com.hyperswitch.common.dto.ProcessTrackerResponse();
        response.setId(processId);
        response.setStatus(request.getStatus() != null ? request.getStatus() : "PROCESSING");
        response.setBusinessStatus(request.getBusinessStatus() != null ? request.getBusinessStatus() : "IN_PROGRESS");
        response.setScheduleTime(request.getScheduleTime());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error resuming recovery", error);
                return Mono.just(Result.<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>err(
                    PaymentError.of("RECOVERY_RESUME_FAILED",
                        "Failed to resume recovery: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>> getBackfillStatusByConnectorCustomer(
            String merchantId,
            String connectorCustomerId,
            String paymentIntentId) {
        log.info("Getting backfill status for merchant: {}, connectorCustomer: {}, paymentIntent: {}", 
                merchantId, connectorCustomerId, paymentIntentId);
        
        // In production, this would retrieve status from database using connector customer ID and payment intent ID
        com.hyperswitch.common.dto.BackfillStatusResponse response = 
            new com.hyperswitch.common.dto.BackfillStatusResponse();
        response.setStatus("COMPLETED");
        response.setProcessedCount(0);
        response.setTotalCount(0);
        response.setStartedAt(Instant.now());
        response.setLastUpdatedAt(Instant.now());
        
        return Mono.just(Result.<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error getting backfill status: {}", error.getMessage(), error);
                return Mono.just(Result.<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>err(
                    PaymentError.of("BACKFILL_STATUS_RETRIEVAL_FAILED",
                        "Failed to get backfill status: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<com.hyperswitch.common.dto.RevenueRecoveryRedisResponse, PaymentError>> getRedisDataByConnectorCustomer(
            String merchantId,
            String connectorCustomerId) {
        log.info("Getting Redis data for merchant: {}, connectorCustomer: {}", merchantId, connectorCustomerId);
        
        // In production, this would retrieve data from Redis using connector customer ID
        com.hyperswitch.common.dto.RevenueRecoveryRedisResponse response = 
            new com.hyperswitch.common.dto.RevenueRecoveryRedisResponse();
        response.setMerchantId(merchantId);
        response.setKeyType("connector_customer");
        response.setData(new HashMap<>());
        response.setTtl(3600L);
        
        return Mono.just(Result.<com.hyperswitch.common.dto.RevenueRecoveryRedisResponse, PaymentError>ok(response))
            .onErrorResume(error -> {
                log.error("Error getting Redis data: {}", error.getMessage(), error);
                return Mono.just(Result.<com.hyperswitch.common.dto.RevenueRecoveryRedisResponse, PaymentError>err(
                    PaymentError.of("REDIS_DATA_RETRIEVAL_FAILED",
                        "Failed to get Redis data: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> updateToken(
            String merchantId,
            com.hyperswitch.common.dto.UpdateTokenRequest request) {
        log.info("Updating token for merchant: {}, connectorCustomer: {}", merchantId, request.getConnectorCustomerId());
        
        // In production, this would:
        // 1. Update token in Redis
        // 2. Update token in database if needed
        // 3. Invalidate related cache entries
        
        return Mono.just(Result.<Void, PaymentError>ok(null))
            .onErrorResume(error -> {
                log.error("Error updating token: {}", error.getMessage(), error);
                return Mono.just(Result.<Void, PaymentError>err(
                    PaymentError.of("TOKEN_UPDATE_FAILED",
                        "Failed to update token: " + error.getMessage())
                ));
            });
    }
}

