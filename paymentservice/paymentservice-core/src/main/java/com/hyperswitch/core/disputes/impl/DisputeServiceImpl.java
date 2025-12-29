package com.hyperswitch.core.disputes.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.DisputeId;
import com.hyperswitch.common.types.DisputeStage;
import com.hyperswitch.common.types.DisputeStatus;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.connectors.ConnectorInterface;
import com.hyperswitch.connectors.ConnectorResponse;
import com.hyperswitch.connectors.ConnectorService;
import com.hyperswitch.core.disputes.DisputeService;
import com.hyperswitch.storage.entity.DisputeEntity;
import com.hyperswitch.storage.repository.DisputeRepository;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DisputeService
 */
@Service
public class DisputeServiceImpl implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);
    private static final String DISPUTE_NOT_FOUND = "Dispute not found";

    private final DisputeRepository disputeRepository;
    private final ObjectMapper objectMapper;
    private final ConnectorService connectorService;

    @Autowired
    public DisputeServiceImpl(
            DisputeRepository disputeRepository, 
            ObjectMapper objectMapper,
            ConnectorService connectorService) {
        this.disputeRepository = disputeRepository;
        this.objectMapper = objectMapper;
        this.connectorService = connectorService;
    }

    @Override
    public Mono<Either<PaymentError, DisputeResponse>> getDispute(String merchantId, DisputeId disputeId) {
        log.info("Retrieving dispute: {} for merchant: {}", disputeId.getValue(), merchantId);
        
        return disputeRepository.findByMerchantIdAndDisputeId(merchantId, disputeId.getValue())
            .map(DisputeMapper::toDisputeResponse)
            .map(Either::<PaymentError, DisputeResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", DISPUTE_NOT_FOUND))));
    }

    @Override
    public Flux<DisputeResponse> listDisputes(String merchantId) {
        log.info("Listing disputes for merchant: {}", merchantId);
        
        return disputeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .map(DisputeMapper::toDisputeResponse);
    }

    @Override
    public Flux<DisputeResponse> listDisputesByPayment(String merchantId, String paymentId) {
        log.info("Listing disputes for payment: {} and merchant: {}", paymentId, merchantId);
        
        return disputeRepository.findByMerchantIdAndPaymentId(merchantId, paymentId)
            .map(DisputeMapper::toDisputeResponse);
    }

    @Override
    public Mono<Either<PaymentError, DisputeResponse>> acceptDispute(String merchantId, DisputeId disputeId) {
        log.info("Accepting dispute: {} for merchant: {}", disputeId.getValue(), merchantId);
        
        return disputeRepository.findByMerchantIdAndDisputeId(merchantId, disputeId.getValue())
            .flatMap(dispute -> {
                dispute.setDisputeStatus(DisputeStatus.DISPUTE_ACCEPTED.name());
                dispute.setModifiedAt(Instant.now());
                return disputeRepository.save(dispute);
            })
            .map(DisputeMapper::toDisputeResponse)
            .map(Either::<PaymentError, DisputeResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", DISPUTE_NOT_FOUND))));
    }

    @Override
    public Mono<Either<PaymentError, DisputeResponse>> submitEvidence(String merchantId, SubmitEvidenceRequest request) {
        log.info("Submitting evidence for dispute: {} for merchant: {}", request.getDisputeId(), merchantId);
        
        return disputeRepository.findByMerchantIdAndDisputeId(merchantId, request.getDisputeId())
            .flatMap(dispute -> {
                try {
                    // Convert evidence request to JSON
                    Map<String, Object> evidenceMap = new HashMap<>();
                    if (request.getAccessActivityLog() != null) {
                        evidenceMap.put("access_activity_log", request.getAccessActivityLog());
                    }
                    if (request.getBillingAddress() != null) {
                        evidenceMap.put("billing_address", request.getBillingAddress());
                    }
                    if (request.getCancellationPolicy() != null) {
                        evidenceMap.put("cancellation_policy", request.getCancellationPolicy());
                    }
                    if (request.getCustomerCommunication() != null) {
                        evidenceMap.put("customer_communication", request.getCustomerCommunication());
                    }
                    if (request.getReceipt() != null) {
                        evidenceMap.put("receipt", request.getReceipt());
                    }
                    if (request.getShippingDocumentation() != null) {
                        evidenceMap.put("shipping_documentation", request.getShippingDocumentation());
                    }
                    // Add other evidence fields as needed
                    
                    String evidenceJson = objectMapper.writeValueAsString(evidenceMap);
                    dispute.setEvidence(evidenceJson);
                    dispute.setDisputeStatus(DisputeStatus.DISPUTE_CHALLENGED.name());
                    dispute.setModifiedAt(Instant.now());
                    
                    return disputeRepository.save(dispute);
                } catch (Exception e) {
                    log.error("Error serializing evidence", e);
                    return Mono.error(e);
                }
            })
            .map(DisputeMapper::toDisputeResponse)
            .map(Either::<PaymentError, DisputeResponse>right)
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", DISPUTE_NOT_FOUND))));
    }

    @Override
    public Mono<Either<PaymentError, DisputeResponse>> createOrUpdateDispute(
        String merchantId,
        String paymentId,
        String attemptId,
        String connector,
        String connectorDisputeId,
        String amount,
        String currency,
        String connectorStatus,
        String disputeStage,
        String disputeStatus,
        String connectorReason,
        String connectorReasonCode
    ) {
        log.info("Creating or updating dispute from webhook: connector={}, dispute_id={}, merchant={}", 
            connector, connectorDisputeId, merchantId);
        
        return disputeRepository.findByMerchantIdAndPaymentIdAndConnectorDisputeId(
            merchantId, paymentId, connectorDisputeId
        )
        .switchIfEmpty(Mono.defer(() -> {
            // Create new dispute
            DisputeEntity newDispute = new DisputeEntity();
            newDispute.setDisputeId(DisputeId.generate().getValue());
            newDispute.setPaymentId(paymentId);
            newDispute.setAttemptId(attemptId);
            newDispute.setMerchantId(merchantId);
            newDispute.setConnector(connector);
            newDispute.setConnectorDisputeId(connectorDisputeId);
            newDispute.setAmount(amount);
            newDispute.setCurrency(currency);
            newDispute.setConnectorStatus(connectorStatus);
            newDispute.setDisputeStage(disputeStage);
            newDispute.setDisputeStatus(disputeStatus);
            newDispute.setConnectorReason(connectorReason);
            newDispute.setConnectorReasonCode(connectorReasonCode);
            newDispute.setCreatedAt(Instant.now());
            newDispute.setModifiedAt(Instant.now());
            return Mono.just(newDispute);
        }))
        .flatMap(dispute -> {
            // Update existing dispute
            dispute.setConnectorStatus(connectorStatus);
            dispute.setDisputeStage(disputeStage);
            dispute.setDisputeStatus(disputeStatus);
            dispute.setConnectorReason(connectorReason);
            dispute.setConnectorReasonCode(connectorReasonCode);
            dispute.setConnectorUpdatedAt(Instant.now());
            dispute.setModifiedAt(Instant.now());
            return disputeRepository.save(dispute);
        })
        .map(DisputeMapper::toDisputeResponse)
        .map(Either::<PaymentError, DisputeResponse>right);
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeResponse>> defendDispute(
        String merchantId,
        DisputeId disputeId,
        SubmitEvidenceRequest evidenceRequest
    ) {
        log.info("Defending dispute: {} for merchant: {}", disputeId.getValue(), merchantId);
        
        // Defending a dispute involves submitting evidence and marking it as challenged
        return getDispute(merchantId, disputeId)
            .flatMap(disputeResult -> {
                if (disputeResult.isLeft()) {
                    return Mono.just(disputeResult);
                }
                
                // Submit evidence first
                evidenceRequest.setDisputeId(disputeId.getValue());
                return submitEvidence(merchantId, evidenceRequest)
                    .flatMap(submitResult -> {
                        if (submitResult.isLeft()) {
                            return Mono.just(submitResult);
                        }
                        
                        // Update dispute status to indicate it's being defended
                        return disputeRepository.findByMerchantIdAndDisputeId(merchantId, disputeId.getValue())
                            .flatMap(dispute -> {
                                // Status is already set to DISPUTE_CHALLENGED by submitEvidence
                                // But we can add additional defense metadata
                                dispute.setModifiedAt(Instant.now());
                                return disputeRepository.save(dispute);
                            })
                            .map(DisputeMapper::toDisputeResponse)
                            .map(Either::<PaymentError, DisputeResponse>right);
                    });
            });
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeResponse>> syncDispute(
        String merchantId,
        DisputeId disputeId
    ) {
        log.info("Syncing dispute: {} with connector for merchant: {}", disputeId.getValue(), merchantId);
        
        return disputeRepository.findByMerchantIdAndDisputeId(merchantId, disputeId.getValue())
            .flatMap(dispute -> {
                // Get connector implementation
                ConnectorInterface connector = connectorService.getConnector(dispute.getConnector());
                if (connector == null) {
                    log.error("Connector not found: {}", dispute.getConnector());
                    return Mono.just(Either.<PaymentError, DisputeResponse>left(
                        PaymentError.of("CONNECTOR_NOT_FOUND", "Connector not found: " + dispute.getConnector())));
                }
                
                // Call connector's dispute retrieval API
                // Note: This assumes the connector interface has a method to retrieve dispute status
                // In a full implementation, we would add getDisputeStatus to ConnectorInterface
                log.info("Fetching dispute status from connector: {} for dispute: {}", 
                    dispute.getConnector(), dispute.getConnectorDisputeId());
                
                // For now, we'll use a generic sync method if available
                // In production, connectors would implement dispute-specific sync methods
                return syncDisputeFromConnector(dispute)
                    .flatMap(syncResult -> {
                        if (syncResult.isErr()) {
                            log.error("Failed to sync dispute from connector: {}", syncResult.unwrapErr().getMessage());
                            return Mono.just(Either.<PaymentError, DisputeResponse>left(
                                PaymentError.of("SYNC_FAILED", "Failed to sync dispute: " + syncResult.unwrapErr().getMessage())));
                        }
                        
                        // Update dispute with latest status from connector
                        ConnectorResponse connectorResponse = syncResult.unwrap();
                        updateDisputeFromConnectorResponse(dispute, connectorResponse);
                        
                        dispute.setConnectorUpdatedAt(Instant.now());
                        dispute.setModifiedAt(Instant.now());
                        
                        return disputeRepository.save(dispute)
                            .map(DisputeMapper::toDisputeResponse)
                            .map(Either::<PaymentError, DisputeResponse>right);
                    });
            })
            .switchIfEmpty(Mono.just(Either.left(PaymentError.of("NOT_FOUND", DISPUTE_NOT_FOUND))));
    }
    
    /**
     * Sync dispute status from connector
     * In production, this would call connector-specific dispute retrieval API
     */
    private Mono<Result<ConnectorResponse, PaymentError>> syncDisputeFromConnector(
            DisputeEntity dispute) {
        // In production, this would:
        // 1. Call connector.getDisputeStatus(dispute.getConnectorDisputeId())
        // 2. Parse the response
        // 3. Return the dispute status
        
        // For now, simulate by returning a success response
        // In a full implementation, each connector would implement dispute retrieval
        log.info("Syncing dispute {} from connector {}", dispute.getConnectorDisputeId(), dispute.getConnector());
        
        // Create a mock connector response
        // In production, this would be the actual response from the connector API
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("dispute_id", dispute.getConnectorDisputeId());
        responseData.put("status", dispute.getConnectorStatus());
        responseData.put("stage", dispute.getDisputeStage());
        
        ConnectorResponse connectorResponse = ConnectorResponse.builder()
            .status("success")
            .additionalData(responseData)
            .build();
        
        return Mono.just(Result.<ConnectorResponse, PaymentError>ok(connectorResponse));
    }
    
    /**
     * Update dispute entity from connector response
     */
    private void updateDisputeFromConnectorResponse(DisputeEntity dispute, ConnectorResponse connectorResponse) {
        if (connectorResponse.getAdditionalData() != null) {
            Map<String, Object> data = connectorResponse.getAdditionalData();
            
            if (data.containsKey("status")) {
                String status = data.get("status").toString();
                dispute.setConnectorStatus(status);
                
                // Map connector status to internal dispute status if needed
                // This would be connector-specific logic
            }
            
            if (data.containsKey("stage")) {
                String stage = data.get("stage").toString();
                dispute.setDisputeStage(stage);
            }
            
            if (data.containsKey("reason")) {
                String reason = data.get("reason").toString();
                dispute.setConnectorReason(reason);
            }
            
            if (data.containsKey("reason_code")) {
                String reasonCode = data.get("reason_code").toString();
                dispute.setConnectorReasonCode(reasonCode);
            }
        }
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeListResponse>> listDisputesWithFilters(
            String merchantId,
            DisputeListFilterConstraints constraints) {
        log.info("Listing disputes for merchant: {} with filters", merchantId);
        
        constraints.setMerchantId(merchantId);
        
        return disputeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .filter(dispute -> matchesDisputeFilters(dispute, constraints))
            .collectList()
            .flatMap(disputes -> buildDisputeListResponse(disputes, constraints))
            .onErrorResume(error -> {
                log.error("Error listing disputes", error);
                return Mono.just(Either.<PaymentError, DisputeListResponse>left(
                    PaymentError.of("DISPUTE_LIST_FAILED",
                        "Failed to list disputes: " + error.getMessage())
                ));
            });
    }
    
    /**
     * Check if dispute matches filter constraints
     */
    private boolean matchesDisputeFilters(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return matchesDisputeIdFilter(dispute, constraints)
            && matchesPaymentIdFilter(dispute, constraints)
            && matchesStatusFilter(dispute, constraints)
            && matchesStageFilter(dispute, constraints)
            && matchesConnectorFilter(dispute, constraints)
            && matchesCurrencyFilter(dispute, constraints)
            && matchesReasonFilter(dispute, constraints)
            && matchesTimeRangeFilter(dispute, constraints);
    }
    
    private boolean matchesDisputeIdFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return constraints.getDisputeId() == null || constraints.getDisputeId().isEmpty() 
            || constraints.getDisputeId().equals(dispute.getDisputeId());
    }
    
    private boolean matchesPaymentIdFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return constraints.getPaymentId() == null || constraints.getPaymentId().isEmpty() 
            || constraints.getPaymentId().equals(dispute.getPaymentId());
    }
    
    private boolean matchesStatusFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        if (constraints.getDisputeStatus() == null) {
            return true;
        }
        return constraints.getDisputeStatus().name().equals(dispute.getDisputeStatus());
    }
    
    private boolean matchesStageFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        if (constraints.getDisputeStage() == null) {
            return true;
        }
        return constraints.getDisputeStage().name().equals(dispute.getDisputeStage());
    }
    
    private boolean matchesConnectorFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return constraints.getConnector() == null || constraints.getConnector().isEmpty() 
            || constraints.getConnector().equalsIgnoreCase(dispute.getConnector());
    }
    
    private boolean matchesCurrencyFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return constraints.getCurrency() == null || constraints.getCurrency().isEmpty() 
            || constraints.getCurrency().equalsIgnoreCase(dispute.getCurrency());
    }
    
    private boolean matchesReasonFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        return constraints.getReason() == null || constraints.getReason().isEmpty() 
            || constraints.getReason().equalsIgnoreCase(dispute.getConnectorReason());
    }
    
    private boolean matchesTimeRangeFilter(DisputeEntity dispute, DisputeListFilterConstraints constraints) {
        Instant receivedTime = dispute.getConnectorCreatedAt() != null 
            ? dispute.getConnectorCreatedAt() 
            : dispute.getCreatedAt();
        
        if (receivedTime == null) {
            return true;
        }
        
        // Check receivedTime (exact match)
        if (constraints.getReceivedTime() != null && !receivedTime.equals(constraints.getReceivedTime())) {
            return false;
        }
        
        // Check receivedTimeLt (less than)
        if (constraints.getReceivedTimeLt() != null && !receivedTime.isBefore(constraints.getReceivedTimeLt())) {
            return false;
        }
        
        // Check receivedTimeGt (greater than)
        if (constraints.getReceivedTimeGt() != null && !receivedTime.isAfter(constraints.getReceivedTimeGt())) {
            return false;
        }
        
        // Check receivedTimeLte (less than or equal)
        if (constraints.getReceivedTimeLte() != null && receivedTime.isAfter(constraints.getReceivedTimeLte())) {
            return false;
        }
        
        // Check receivedTimeGte (greater than or equal)
        if (constraints.getReceivedTimeGte() != null && receivedTime.isBefore(constraints.getReceivedTimeGte())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Build dispute list response with pagination
     */
    private Mono<Either<PaymentError, DisputeListResponse>> buildDisputeListResponse(
            List<DisputeEntity> disputes,
            DisputeListFilterConstraints constraints) {
        int totalCount = disputes.size();
        int offset = constraints.getOffset() != null ? constraints.getOffset() : 0;
        int limit = constraints.getLimit() != null ? constraints.getLimit() : 100;
        
        List<DisputeEntity> paginatedDisputes = disputes.stream()
            .skip(offset)
            .limit(limit)
            .toList();
        
        List<DisputeResponse> disputeData = paginatedDisputes.stream()
            .map(DisputeMapper::toDisputeResponse)
            .toList();
        
        DisputeListResponse response = new DisputeListResponse();
        response.setData(disputeData);
        response.setTotalCount((long) totalCount);
        response.setLimit(limit);
        response.setOffset(offset);
        
        return Mono.just(Either.<PaymentError, DisputeListResponse>right(response));
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeFiltersResponse>> getDisputeFilters(String merchantId) {
        log.info("Getting dispute filters for merchant: {}", merchantId);
        
        return disputeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            .collectList()
            .map(disputes -> {
                Set<String> connectors = new HashSet<>();
                Set<String> currencies = new HashSet<>();
                Set<String> statuses = new HashSet<>();
                Set<String> stages = new HashSet<>();
                Set<String> reasons = new HashSet<>();
                
                for (DisputeEntity dispute : disputes) {
                    if (dispute.getConnector() != null) {
                        connectors.add(dispute.getConnector());
                    }
                    if (dispute.getCurrency() != null) {
                        currencies.add(dispute.getCurrency());
                    }
                    if (dispute.getDisputeStatus() != null) {
                        statuses.add(dispute.getDisputeStatus());
                    }
                    if (dispute.getDisputeStage() != null) {
                        stages.add(dispute.getDisputeStage());
                    }
                    if (dispute.getConnectorReason() != null) {
                        reasons.add(dispute.getConnectorReason());
                    }
                }
                
                DisputeFiltersResponse response = new DisputeFiltersResponse();
                List<DisputeFiltersResponse.FilterValue> queryData = new ArrayList<>();
                
                // Add connector filter values
                if (!connectors.isEmpty()) {
                    DisputeFiltersResponse.FilterValue connectorFilter = 
                        new DisputeFiltersResponse.FilterValue();
                    connectorFilter.setDimension("connector");
                    connectorFilter.setValues(new ArrayList<>(connectors));
                    queryData.add(connectorFilter);
                }
                
                // Add currency filter values
                if (!currencies.isEmpty()) {
                    DisputeFiltersResponse.FilterValue currencyFilter = 
                        new DisputeFiltersResponse.FilterValue();
                    currencyFilter.setDimension("currency");
                    currencyFilter.setValues(new ArrayList<>(currencies));
                    queryData.add(currencyFilter);
                }
                
                // Add status filter values
                if (!statuses.isEmpty()) {
                    DisputeFiltersResponse.FilterValue statusFilter = 
                        new DisputeFiltersResponse.FilterValue();
                    statusFilter.setDimension("status");
                    statusFilter.setValues(new ArrayList<>(statuses));
                    queryData.add(statusFilter);
                }
                
                // Add stage filter values
                if (!stages.isEmpty()) {
                    DisputeFiltersResponse.FilterValue stageFilter = 
                        new DisputeFiltersResponse.FilterValue();
                    stageFilter.setDimension("stage");
                    stageFilter.setValues(new ArrayList<>(stages));
                    queryData.add(stageFilter);
                }
                
                // Add reason filter values
                if (!reasons.isEmpty()) {
                    DisputeFiltersResponse.FilterValue reasonFilter = 
                        new DisputeFiltersResponse.FilterValue();
                    reasonFilter.setDimension("reason");
                    reasonFilter.setValues(new ArrayList<>(reasons));
                    queryData.add(reasonFilter);
                }
                
                response.setQueryData(queryData);
                
                return Either.<PaymentError, DisputeFiltersResponse>right(response);
            })
            .onErrorResume(error -> {
                log.error("Error getting dispute filters", error);
                return Mono.just(Either.<PaymentError, DisputeFiltersResponse>left(
                    PaymentError.of("DISPUTE_FILTERS_FAILED",
                        "Failed to get dispute filters: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeAggregatesResponse>> getDisputeAggregates(
            String merchantId,
            Instant startTime,
            Instant endTime) {
        log.info("Getting dispute aggregates for merchant: {} from {} to {}", merchantId, startTime, endTime);
        
        Flux<DisputeEntity> disputesFlux = disputeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        
        return disputesFlux
            .filter(dispute -> {
                if (startTime == null && endTime == null) {
                    return true;
                }
                Instant disputeTime = dispute.getConnectorCreatedAt() != null 
                    ? dispute.getConnectorCreatedAt() 
                    : dispute.getCreatedAt();
                if (disputeTime == null) {
                    return false;
                }
                if (startTime != null && disputeTime.isBefore(startTime)) {
                    return false;
                }
                return endTime == null || !disputeTime.isAfter(endTime);
            })
            .collectList()
            .map(disputes -> {
                Map<String, Long> statusCounts = new HashMap<>();
                Map<String, Long> stageCounts = new HashMap<>();
                long total = 0;
                
                for (DisputeEntity dispute : disputes) {
                    String status = dispute.getDisputeStatus() != null ? dispute.getDisputeStatus() : "UNKNOWN";
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
                    
                    String stage = dispute.getDisputeStage() != null ? dispute.getDisputeStage() : "UNKNOWN";
                    stageCounts.put(stage, stageCounts.getOrDefault(stage, 0L) + 1);
                    
                    total++;
                }
                
                return new DisputeAggregatesResponse(statusCounts, stageCounts, total);
            })
            .map(Either::<PaymentError, DisputeAggregatesResponse>right)
            .onErrorResume(error -> {
                log.error("Error getting dispute aggregates", error);
                return Mono.just(Either.<PaymentError, DisputeAggregatesResponse>left(
                    PaymentError.of("DISPUTE_AGGREGATES_FAILED",
                        "Failed to get dispute aggregates: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeResponse>> attachEvidence(
            String merchantId,
            SubmitEvidenceRequest request) {
        log.info("Attaching evidence for dispute: {} for merchant: {}", request.getDisputeId(), merchantId);
        
        // Attach evidence is similar to submit evidence
        return submitEvidence(merchantId, request);
    }
    
    @Override
    public Mono<Either<PaymentError, DisputeEvidenceResponse>> retrieveEvidence(
            String merchantId,
            DisputeId disputeId) {
        log.info("Retrieving evidence for dispute: {} for merchant: {}", disputeId, merchantId);
        
        return getDispute(merchantId, disputeId)
            .flatMap(disputeResult -> {
                if (disputeResult.isRight()) {
                    DisputeResponse dispute = disputeResult.get();
                    DisputeEvidenceResponse evidenceResponse = new DisputeEvidenceResponse();
                    evidenceResponse.setDisputeId(disputeId.getValue());
                    // In production, this would fetch actual evidence from storage
                    // For now, return empty evidence blocks
                    evidenceResponse.setEvidenceBlocks(new java.util.ArrayList<>());
                    return Mono.just(Either.<PaymentError, DisputeEvidenceResponse>right(evidenceResponse));
                } else {
                    return Mono.just(Either.<PaymentError, DisputeEvidenceResponse>left(disputeResult.getLeft()));
                }
            })
            .onErrorResume(error -> {
                log.error("Error retrieving dispute evidence", error);
                return Mono.just(Either.<PaymentError, DisputeEvidenceResponse>left(
                    PaymentError.of("DISPUTE_EVIDENCE_RETRIEVE_FAILED",
                        "Failed to retrieve dispute evidence: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Either<PaymentError, Void>> deleteEvidence(
            String merchantId,
            String fileId) {
        log.info("Deleting evidence file: {} for merchant: {}", fileId, merchantId);
        
        // In production, this would delete the evidence file from storage
        // For now, return success
        return Mono.just(Either.<PaymentError, Void>right(null))
            .onErrorResume(error -> {
                log.error("Error deleting dispute evidence", error);
                return Mono.just(Either.<PaymentError, Void>left(
                    PaymentError.of("DISPUTE_EVIDENCE_DELETE_FAILED",
                        "Failed to delete dispute evidence: " + error.getMessage())
                ));
            });
    }
    
    @Override
    public Flux<DisputeResponse> fetchDisputesFromConnector(
            String merchantId,
            String connectorId) {
        log.info("Fetching disputes from connector: {} for merchant: {}", connectorId, merchantId);
        
        // In production, this would call the connector API to fetch disputes
        // For now, return disputes filtered by connector from existing disputes
        return listDisputes(merchantId)
            .filter(dispute -> connectorId.equals(dispute.getConnector()))
            .onErrorResume(error -> {
                log.error("Error fetching disputes from connector", error);
                return Flux.empty();
            });
    }
}

