package com.hyperswitch.core.revenuerecovery;

import com.hyperswitch.common.dto.RevenueRecoveryAnalytics;
import com.hyperswitch.common.dto.RevenueRecoveryResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.RecoveryStatus;
import com.hyperswitch.common.types.RevenueRecoveryAlgorithmType;
import io.vavr.control.Either;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for revenue recovery management
 */
public interface RevenueRecoveryService {

    /**
     * Create or update a revenue recovery record
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> createOrUpdateRecovery(
        String merchantId,
        String paymentId,
        String attemptId,
        String profileId,
        String billingMcaId,
        RevenueRecoveryAlgorithmType algorithmType,
        Long retryBudget
    );

    /**
     * Get revenue recovery by ID
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> getRecovery(
        String merchantId,
        String recoveryId
    );

    /**
     * Get revenue recovery by payment and attempt
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> getRecoveryByPayment(
        String merchantId,
        String paymentId,
        String attemptId
    );

    /**
     * List revenue recoveries for a merchant
     */
    Flux<RevenueRecoveryResponse> listRecoveries(String merchantId, RecoveryStatus status);

    /**
     * Update recovery status
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> updateRecoveryStatus(
        String merchantId,
        String recoveryId,
        RecoveryStatus status
    );

    /**
     * Calculate next retry time based on algorithm
     */
    Mono<Long> calculateNextRetryDelay(
        RevenueRecoveryAlgorithmType algorithmType,
        Integer retryCount,
        Long baseDelaySeconds
    );

    /**
     * Check if retry budget allows retry
     */
    Mono<Boolean> canRetryWithinBudget(
        String recoveryId,
        Long retryCost
    );

    /**
     * Get revenue recovery analytics
     */
    Mono<RevenueRecoveryAnalytics> getAnalytics(String merchantId);
    
    /**
     * Execute recovery workflow - Orchestrates the entire recovery process
     * This includes evaluating retry eligibility, scheduling retries, and managing workflow state
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> executeRecoveryWorkflow(
        String merchantId,
        String recoveryId
    );
    
    /**
     * Schedule next retry based on recovery configuration
     * This integrates with the scheduler service to queue retry attempts
     */
    Mono<Either<PaymentError, Void>> scheduleNextRetry(
        String merchantId,
        String recoveryId
    );
    
    /**
     * Evaluate if payment should be retried based on error type and recovery rules
     * Returns true if retry should proceed, false if it should be terminated
     */
    Mono<Boolean> shouldRetry(
        String recoveryId,
        String errorCode,
        String errorMessage
    );
    
    /**
     * Process recovery workflow step - Handles state transitions and next actions
     */
    Mono<Either<PaymentError, RevenueRecoveryResponse>> processWorkflowStep(
        String merchantId,
        String recoveryId,
        String stepResult,
        String errorCode,
        String errorMessage
    );
    
    /**
     * Get revenue recovery redis data
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.RevenueRecoveryRedisResponse, PaymentError>> getRevenueRecoveryRedisData(
        String merchantId,
        String keyType
    );
    
    /**
     * Backfill revenue recovery data
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.RevenueRecoveryBackfillResponse, PaymentError>> dataBackfill(
        String merchantId,
        java.util.List<com.hyperswitch.common.dto.RevenueRecoveryBackfillRequest> records,
        java.time.Instant cutoffDatetime
    );
    
    /**
     * Update Redis data for revenue recovery
     */
    Mono<com.hyperswitch.common.types.Result<Void, PaymentError>> updateRedisData(
        String merchantId,
        String key,
        java.util.Map<String, Object> data
    );
    
    /**
     * Get backfill status
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>> getBackfillStatus(
        String merchantId,
        String backfillId
    );
    
    /**
     * Get process tracker data
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>> getProcessTracker(
        String merchantId,
        String processId
    );
    
    /**
     * Resume revenue recovery
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.ProcessTrackerResponse, PaymentError>> resumeRecovery(
        String merchantId,
        String processId,
        com.hyperswitch.common.dto.ResumeRecoveryRequest request
    );
    
    /**
     * Get backfill status by connector customer and payment intent
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.BackfillStatusResponse, PaymentError>> getBackfillStatusByConnectorCustomer(
        String merchantId,
        String connectorCustomerId,
        String paymentIntentId
    );
    
    /**
     * Get Redis data by connector customer
     */
    Mono<com.hyperswitch.common.types.Result<com.hyperswitch.common.dto.RevenueRecoveryRedisResponse, PaymentError>> getRedisDataByConnectorCustomer(
        String merchantId,
        String connectorCustomerId
    );
    
    /**
     * Update token in recovery data backfill
     */
    Mono<com.hyperswitch.common.types.Result<Void, PaymentError>> updateToken(
        String merchantId,
        com.hyperswitch.common.dto.UpdateTokenRequest request
    );
}

