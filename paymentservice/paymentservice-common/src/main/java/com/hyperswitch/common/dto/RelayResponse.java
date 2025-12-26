package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for relay operations
 */
public class RelayResponse {
    
    @JsonProperty("relay_id")
    private String relayId;
    
    @JsonProperty("connector_resource_id")
    private String connectorResourceId;
    
    @JsonProperty("connector_id")
    private String connectorId;
    
    @JsonProperty("profile_id")
    private String profileId;
    
    @JsonProperty("merchant_id")
    private String merchantId;
    
    @JsonProperty("relay_type")
    private String relayType;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("connector_reference_id")
    private String connectorReferenceId;
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("response_data")
    private Map<String, Object> responseData;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("modified_at")
    private Instant modifiedAt;
    
    // Getters and Setters
    public String getRelayId() {
        return relayId;
    }
    
    public void setRelayId(String relayId) {
        this.relayId = relayId;
    }
    
    public String getConnectorResourceId() {
        return connectorResourceId;
    }
    
    public void setConnectorResourceId(String connectorResourceId) {
        this.connectorResourceId = connectorResourceId;
    }
    
    public String getConnectorId() {
        return connectorId;
    }
    
    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }
    
    public String getProfileId() {
        return profileId;
    }
    
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
    
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public String getRelayType() {
        return relayType;
    }
    
    public void setRelayType(String relayType) {
        this.relayType = relayType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getConnectorReferenceId() {
        return connectorReferenceId;
    }
    
    public void setConnectorReferenceId(String connectorReferenceId) {
        this.connectorReferenceId = connectorReferenceId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getResponseData() {
        return responseData;
    }
    
    public void setResponseData(Map<String, Object> responseData) {
        this.responseData = responseData;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}

