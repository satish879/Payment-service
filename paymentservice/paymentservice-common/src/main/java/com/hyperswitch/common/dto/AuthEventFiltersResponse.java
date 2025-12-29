package com.hyperswitch.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for auth event filters
 */
public class AuthEventFiltersResponse {
    
    @JsonProperty("query_data")
    private List<FilterValue> queryData;
    
    // Getters and Setters
    public List<FilterValue> getQueryData() {
        return queryData;
    }
    
    public void setQueryData(List<FilterValue> queryData) {
        this.queryData = queryData;
    }
    
    /**
     * Filter value containing dimension and values
     */
    public static class FilterValue {
        @JsonProperty("dimension")
        private String dimension;
        
        @JsonProperty("values")
        private List<String> values;
        
        // Getters and Setters
        public String getDimension() {
            return dimension;
        }
        
        public void setDimension(String dimension) {
            this.dimension = dimension;
        }
        
        public List<String> getValues() {
            return values;
        }
        
        public void setValues(List<String> values) {
            this.values = values;
        }
    }
}

