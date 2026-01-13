package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response for refund list
 * Matches hyperswitch RefundListResponse structure
 */
public class RefundListResponse {
    @JsonProperty("refunds")
    private List<Map<String, Object>> refunds;
    
    @JsonProperty("total")
    private Long total;
    
    private Integer limit;
    private Integer offset;
    
    public RefundListResponse() {
    }
    
    @JsonProperty("refunds")
    public List<Map<String, Object>> getRefunds() {
        return refunds;
    }
    
    @JsonProperty("refunds")
    public void setRefunds(List<Map<String, Object>> refunds) {
        this.refunds = refunds;
    }
    
    // Backward compatibility - also support "data" field
    @JsonProperty("data")
    public List<Map<String, Object>> getData() {
        return refunds;
    }
    
    @JsonProperty("data")
    public void setData(List<Map<String, Object>> data) {
        this.refunds = data;
    }
    
    @JsonProperty("total")
    public Long getTotal() {
        return total;
    }
    
    @JsonProperty("total")
    public void setTotal(Long total) {
        this.total = total;
    }
    
    // Backward compatibility - also support "totalCount" field
    @JsonProperty("totalCount")
    public Long getTotalCount() {
        return total;
    }
    
    @JsonProperty("totalCount")
    public void setTotalCount(Long totalCount) {
        this.total = totalCount;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Integer getOffset() {
        return offset;
    }
    
    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}

