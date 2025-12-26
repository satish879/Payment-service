package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for locker migration
 */
public class LockerMigrationRequest {
    
    @JsonProperty("migration_type")
    private String migrationType;
    
    @JsonProperty("force")
    private Boolean force;
    
    // Getters and Setters
    public String getMigrationType() {
        return migrationType;
    }
    
    public void setMigrationType(String migrationType) {
        this.migrationType = migrationType;
    }
    
    public Boolean getForce() {
        return force;
    }
    
    public void setForce(Boolean force) {
        this.force = force;
    }
}

