package com.hyperswitch.core.gsm.impl;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.gsm.GsmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementation of GsmService
 */
@Service
public class GsmServiceImpl implements GsmService {
    
    private static final Logger log = LoggerFactory.getLogger(GsmServiceImpl.class);
    
    @Override
    public Mono<Result<GsmResponse, PaymentError>> createGsmRule(GsmCreateRequest request) {
        log.info("Creating GSM rule: connector={}, flow={}, code={}", 
                request.getConnector(), request.getFlow(), request.getCode());
        
        return Mono.fromCallable(() -> {
            GsmResponse response = new GsmResponse();
            response.setConnector(request.getConnector());
            response.setFlow(request.getFlow());
            response.setSubFlow(request.getSubFlow());
            response.setCode(request.getCode());
            response.setMessage(request.getMessage());
            response.setStatus(request.getStatus());
            response.setRouterError(request.getRouterError());
            response.setDecision(request.getDecision() != null ? request.getDecision() : "do_default");
            response.setStepUpPossible(request.getStepUpPossible() != null ? request.getStepUpPossible() : false);
            response.setUnifiedCode(request.getUnifiedCode());
            response.setUnifiedMessage(request.getUnifiedMessage());
            response.setErrorCategory(request.getErrorCategory());
            response.setClearPanPossible(request.getClearPanPossible() != null ? request.getClearPanPossible() : false);
            response.setFeature(request.getFeature() != null ? request.getFeature() : "retry");
            response.setFeatureData(request.getFeatureData());
            response.setStandardisedCode(request.getStandardisedCode());
            response.setDescription(request.getDescription());
            response.setUserGuidanceMessage(request.getUserGuidanceMessage());
            
            // In production, this would:
            // 1. Validate connector, flow, code, message combination
            // 2. Check for duplicate rules
            // 3. Store GSM rule in database
            // 4. Update cache if applicable
            // 5. Return created GSM rule
            
            return Result.<GsmResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error creating GSM rule: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("GSM_RULE_CREATE_FAILED",
                "Failed to create GSM rule: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<GsmResponse, PaymentError>> getGsmRule(GsmRetrieveRequest request) {
        log.info("Getting GSM rule: connector={}, flow={}, code={}", 
                request.getConnector(), request.getFlow(), request.getCode());
        
        return Mono.fromCallable(() -> {
            GsmResponse response = new GsmResponse();
            response.setConnector(request.getConnector());
            response.setFlow(request.getFlow());
            response.setSubFlow(request.getSubFlow());
            response.setCode(request.getCode());
            response.setMessage(request.getMessage());
            response.setStatus("failed");
            response.setDecision("do_default");
            response.setStepUpPossible(false);
            response.setClearPanPossible(false);
            response.setFeature("retry");
            
            // In production, this would query GSM rule from database based on connector, flow, sub_flow, code, message
            
            return Result.<GsmResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error getting GSM rule: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("GSM_RULE_RETRIEVAL_FAILED",
                "Failed to get GSM rule: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<GsmResponse, PaymentError>> updateGsmRule(GsmUpdateRequest request) {
        log.info("Updating GSM rule: connector={}, flow={}, code={}", 
                request.getConnector(), request.getFlow(), request.getCode());
        
        return Mono.fromCallable(() -> {
            GsmResponse response = new GsmResponse();
            response.setConnector(request.getConnector());
            response.setFlow(request.getFlow());
            response.setSubFlow(request.getSubFlow());
            response.setCode(request.getCode());
            response.setMessage(request.getMessage());
            response.setStatus(request.getStatus());
            response.setRouterError(request.getRouterError());
            response.setDecision(request.getDecision());
            response.setStepUpPossible(request.getStepUpPossible());
            response.setUnifiedCode(request.getUnifiedCode());
            response.setUnifiedMessage(request.getUnifiedMessage());
            response.setErrorCategory(request.getErrorCategory());
            response.setClearPanPossible(request.getClearPanPossible());
            response.setFeature(request.getFeature());
            response.setFeatureData(request.getFeatureData());
            response.setStandardisedCode(request.getStandardisedCode());
            response.setDescription(request.getDescription());
            response.setUserGuidanceMessage(request.getUserGuidanceMessage());
            
            // In production, this would:
            // 1. Validate GSM rule exists
            // 2. Update GSM rule in database
            // 3. Update cache if applicable
            // 4. Return updated GSM rule
            
            return Result.<GsmResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error updating GSM rule: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("GSM_RULE_UPDATE_FAILED",
                "Failed to update GSM rule: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<GsmDeleteResponse, PaymentError>> deleteGsmRule(GsmDeleteRequest request) {
        log.info("Deleting GSM rule: connector={}, flow={}, code={}", 
                request.getConnector(), request.getFlow(), request.getCode());
        
        return Mono.fromCallable(() -> {
            GsmDeleteResponse response = new GsmDeleteResponse();
            response.setGsmRuleDelete(true);
            response.setConnector(request.getConnector());
            response.setFlow(request.getFlow());
            response.setSubFlow(request.getSubFlow());
            response.setCode(request.getCode());
            
            // In production, this would:
            // 1. Validate GSM rule exists
            // 2. Delete GSM rule from database
            // 3. Update cache if applicable
            // 4. Return deletion confirmation
            
            return Result.<GsmDeleteResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error deleting GSM rule: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("GSM_RULE_DELETE_FAILED",
                "Failed to delete GSM rule: " + error.getMessage())));
        });
    }
}

