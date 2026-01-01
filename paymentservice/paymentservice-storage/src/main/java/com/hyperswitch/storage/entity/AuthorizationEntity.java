package com.hyperswitch.storage.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Authorization entity for tracking incremental authorizations
 * Note: "authorization" is a reserved keyword in PostgreSQL, but Spring Data R2DBC
 * should handle the quoted table name automatically when generating queries.
 */
@Table("authorization")
public class AuthorizationEntity {
    @Id
    @Column("id")
    private String id;
    
    @Column("authorization_id")
    private String authorizationId;
    
    @Column("merchant_id")
    private String merchantId;
    
    @Column("payment_id")
    private String paymentId;
    
    @Column("amount")
    private Long amount; // Total authorized amount in minor units
    
    @Column("status")
    private String status; // AuthorizationStatus enum
    
    @Column("error_code")
    private String errorCode;
    
    @Column("error_message")
    private String errorMessage;
    
    @Column("connector_authorization_id")
    private String connectorAuthorizationId;
    
    @Column("previously_authorized_amount")
    private Long previouslyAuthorizedAmount;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("modified_at")
    private Instant modifiedAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getConnectorAuthorizationId() {
        return connectorAuthorizationId;
    }

    public void setConnectorAuthorizationId(String connectorAuthorizationId) {
        this.connectorAuthorizationId = connectorAuthorizationId;
    }

    public Long getPreviouslyAuthorizedAmount() {
        return previouslyAuthorizedAmount;
    }

    public void setPreviouslyAuthorizedAmount(Long previouslyAuthorizedAmount) {
        this.previouslyAuthorizedAmount = previouslyAuthorizedAmount;
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

