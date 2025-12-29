package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request DTO for creating GSM rule
 */
public class GsmCreateRequest {
    
    @JsonProperty("connector")
    private String connector;
    
    @JsonProperty("flow")
    private String flow;
    
    @JsonProperty("sub_flow")
    private String subFlow;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("router_error")
    private String routerError;
    
    @JsonProperty("decision")
    private String decision;
    
    @JsonProperty("step_up_possible")
    private Boolean stepUpPossible;
    
    @JsonProperty("unified_code")
    private String unifiedCode;
    
    @JsonProperty("unified_message")
    private String unifiedMessage;
    
    @JsonProperty("error_category")
    private String errorCategory;
    
    @JsonProperty("clear_pan_possible")
    private Boolean clearPanPossible;
    
    @JsonProperty("feature")
    private String feature;
    
    @JsonProperty("feature_data")
    private Map<String, Object> featureData;
    
    @JsonProperty("standardised_code")
    private String standardisedCode;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("user_guidance_message")
    private String userGuidanceMessage;
    
    // Getters and Setters
    public String getConnector() {
        return connector;
    }
    
    public void setConnector(String connector) {
        this.connector = connector;
    }
    
    public String getFlow() {
        return flow;
    }
    
    public void setFlow(String flow) {
        this.flow = flow;
    }
    
    public String getSubFlow() {
        return subFlow;
    }
    
    public void setSubFlow(String subFlow) {
        this.subFlow = subFlow;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRouterError() {
        return routerError;
    }
    
    public void setRouterError(String routerError) {
        this.routerError = routerError;
    }
    
    public String getDecision() {
        return decision;
    }
    
    public void setDecision(String decision) {
        this.decision = decision;
    }
    
    public Boolean getStepUpPossible() {
        return stepUpPossible;
    }
    
    public void setStepUpPossible(Boolean stepUpPossible) {
        this.stepUpPossible = stepUpPossible;
    }
    
    public String getUnifiedCode() {
        return unifiedCode;
    }
    
    public void setUnifiedCode(String unifiedCode) {
        this.unifiedCode = unifiedCode;
    }
    
    public String getUnifiedMessage() {
        return unifiedMessage;
    }
    
    public void setUnifiedMessage(String unifiedMessage) {
        this.unifiedMessage = unifiedMessage;
    }
    
    public String getErrorCategory() {
        return errorCategory;
    }
    
    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }
    
    public Boolean getClearPanPossible() {
        return clearPanPossible;
    }
    
    public void setClearPanPossible(Boolean clearPanPossible) {
        this.clearPanPossible = clearPanPossible;
    }
    
    public String getFeature() {
        return feature;
    }
    
    public void setFeature(String feature) {
        this.feature = feature;
    }
    
    public Map<String, Object> getFeatureData() {
        return featureData;
    }
    
    public void setFeatureData(Map<String, Object> featureData) {
        this.featureData = featureData;
    }
    
    public String getStandardisedCode() {
        return standardisedCode;
    }
    
    public void setStandardisedCode(String standardisedCode) {
        this.standardisedCode = standardisedCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUserGuidanceMessage() {
        return userGuidanceMessage;
    }
    
    public void setUserGuidanceMessage(String userGuidanceMessage) {
        this.userGuidanceMessage = userGuidanceMessage;
    }
}

