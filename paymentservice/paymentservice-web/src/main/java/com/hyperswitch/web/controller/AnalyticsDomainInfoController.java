package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.DomainInfoResponse;
import com.hyperswitch.common.types.AnalyticsDomain;
import com.hyperswitch.core.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * REST controller for analytics domain info operations
 */
@RestController
@RequestMapping("/api/analytics/v1")
@Tag(name = "Analytics Domain Info", description = "Analytics domain information operations")
public class AnalyticsDomainInfoController {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsDomainInfoController.class);
    
    private AnalyticsService analyticsService;
    
    // Default constructor to allow bean creation even if dependencies are missing
    public AnalyticsDomainInfoController() {
        log.warn("AnalyticsDomainInfoController created without dependencies - services will be null");
    }

    @Autowired(required = false)
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private Mono<ResponseEntity<?>> checkServiceAvailable() {
        if (analyticsService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Analytics service is not available")));
        }
        return null;
    }
    
    /**
     * Get domain info
     * GET /api/analytics/v1/{domain}/info
     */
    @GetMapping("/{domain}/info")
    @Operation(
        summary = "Get domain info",
        description = "Retrieves information about an analytics domain (metrics, dimensions)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Domain info retrieved successfully",
            content = @Content(schema = @Schema(implementation = DomainInfoResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid domain")
    })
    public Mono<ResponseEntity<?>> getDomainInfo(
            @PathVariable("domain") String domain) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        try {
            AnalyticsDomain analyticsDomain = AnalyticsDomain.fromString(domain);
            return analyticsService.getDomainInfo(analyticsDomain)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    /**
     * Get merchant domain info
     * GET /api/analytics/v1/merchant/{domain}/info
     */
    @GetMapping("/merchant/{domain}/info")
    @Operation(
        summary = "Get merchant domain info",
        description = "Retrieves merchant-specific information about an analytics domain"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant domain info retrieved successfully",
            content = @Content(schema = @Schema(implementation = DomainInfoResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid domain")
    })
    public Mono<ResponseEntity<?>> getMerchantDomainInfo(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("domain") String domain) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        try {
            AnalyticsDomain analyticsDomain = AnalyticsDomain.fromString(domain);
            return analyticsService.getMerchantDomainInfo(merchantId, analyticsDomain)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    /**
     * Get org domain info
     * GET /api/analytics/v1/org/{domain}/info
     */
    @GetMapping("/org/{domain}/info")
    @Operation(
        summary = "Get org domain info",
        description = "Retrieves organization-specific information about an analytics domain"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Org domain info retrieved successfully",
            content = @Content(schema = @Schema(implementation = DomainInfoResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid domain")
    })
    public Mono<ResponseEntity<?>> getOrgDomainInfo(
            @RequestHeader("org_id") String orgId,
            @PathVariable("domain") String domain) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        try {
            AnalyticsDomain analyticsDomain = AnalyticsDomain.fromString(domain);
            return analyticsService.getOrgDomainInfo(orgId, analyticsDomain)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    /**
     * Get profile domain info
     * GET /api/analytics/v1/profile/{domain}/info
     */
    @GetMapping("/profile/{domain}/info")
    @Operation(
        summary = "Get profile domain info",
        description = "Retrieves profile-specific information about an analytics domain"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile domain info retrieved successfully",
            content = @Content(schema = @Schema(implementation = DomainInfoResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid domain")
    })
    public Mono<ResponseEntity<?>> getProfileDomainInfo(
            @RequestHeader("profile_id") String profileId,
            @PathVariable("domain") String domain) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        try {
            AnalyticsDomain analyticsDomain = AnalyticsDomain.fromString(domain);
            return analyticsService.getProfileDomainInfo(profileId, analyticsDomain)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    /**
     * Global search
     * POST /api/analytics/v1/search
     */
    @PostMapping("/search")
    @Operation(
        summary = "Global search",
        description = "Performs a global search across all analytics domains"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.SearchResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<ResponseEntity<?>> globalSearch(
            @RequestHeader("merchant_id") String merchantId,
            @RequestBody com.hyperswitch.common.dto.SearchRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        return analyticsService.globalSearch(merchantId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Domain-specific search
     * POST /api/analytics/v1/search/{domain}
     */
    @PostMapping("/search/{domain}")
    @Operation(
        summary = "Domain-specific search",
        description = "Performs a search within a specific analytics domain"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = com.hyperswitch.common.dto.SearchResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid domain or request")
    })
    public Mono<ResponseEntity<?>> domainSearch(
            @RequestHeader("merchant_id") String merchantId,
            @PathVariable("domain") String domain,
            @RequestBody com.hyperswitch.common.dto.SearchRequest request) {
        Mono<ResponseEntity<?>> unavailable = checkServiceAvailable();
        if (unavailable != null) return unavailable;
        try {
            AnalyticsDomain analyticsDomain = AnalyticsDomain.fromString(domain);
            return analyticsService.domainSearch(merchantId, analyticsDomain, request)
                .map(result -> {
                    if (result.isOk()) {
                        return ResponseEntity.ok(result.unwrap());
                    } else {
                        throw new PaymentException(result.unwrapErr());
                    }
                });
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
}

