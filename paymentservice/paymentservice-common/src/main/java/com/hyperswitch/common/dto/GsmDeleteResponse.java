package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for GSM rule deletion
 */
public class GsmDeleteResponse {
    
    @JsonProperty("gsm_rule_delete")
    private Boolean gsmRuleDelete;
    
    @JsonProperty("connector")
    private String connector;
    
    @JsonProperty("flow")
    private String flow;
    
    @JsonProperty("sub_flow")
    private String subFlow;
    
    @JsonProperty("code")
    private String code;
    
    // Getters and Setters
    public Boolean getGsmRuleDelete() {
        return gsmRuleDelete;
    }
    
    public void setGsmRuleDelete(Boolean gsmRuleDelete) {
        this.gsmRuleDelete = gsmRuleDelete;
    }
    
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
}

