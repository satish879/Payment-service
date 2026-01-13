package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for resuming payment after 3DS authentication
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreeDSResumeRequest {
    @JsonProperty("authenticationId")
    private String authenticationId;

    /**
     * Default constructor for Jackson deserialization
     */
    public ThreeDSResumeRequest() {
        // Empty constructor for Jackson deserialization
    }

    @JsonCreator
    public ThreeDSResumeRequest(@JsonProperty("authenticationId") String authenticationId) {
        this.authenticationId = authenticationId;
    }

    @Override
    public String toString() {
        return "ThreeDSResumeRequest{authenticationId='" + authenticationId + "'}";
    }

    @JsonProperty("authenticationId")
    public String getAuthenticationId() {
        return authenticationId;
    }

    @JsonProperty("authenticationId")
    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }
}

