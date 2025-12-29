package com.hyperswitch.core.analytics;

import com.hyperswitch.common.dto.DomainInfoResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.AnalyticsDomain;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for analytics operations
 */
public interface AnalyticsService {
    
    /**
     * Get domain info
     */
    Mono<Result<DomainInfoResponse, PaymentError>> getDomainInfo(AnalyticsDomain domain);
    
    /**
     * Get merchant domain info
     */
    Mono<Result<DomainInfoResponse, PaymentError>> getMerchantDomainInfo(String merchantId, AnalyticsDomain domain);
    
    /**
     * Get org domain info
     */
    Mono<Result<DomainInfoResponse, PaymentError>> getOrgDomainInfo(String orgId, AnalyticsDomain domain);
    
    /**
     * Get profile domain info
     */
    Mono<Result<DomainInfoResponse, PaymentError>> getProfileDomainInfo(String profileId, AnalyticsDomain domain);
    
    /**
     * Global search
     */
    Mono<Result<com.hyperswitch.common.dto.SearchResponse, PaymentError>> globalSearch(
            String merchantId,
            com.hyperswitch.common.dto.SearchRequest request);
    
    /**
     * Domain-specific search
     */
    Mono<Result<com.hyperswitch.common.dto.SearchResponse, PaymentError>> domainSearch(
            String merchantId,
            AnalyticsDomain domain,
            com.hyperswitch.common.dto.SearchRequest request);
    
    /**
     * Get payment metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentMetricsResponse, PaymentError>> getPaymentMetrics(
            String merchantId,
            com.hyperswitch.common.dto.PaymentMetricsRequest request);
    
    /**
     * Get merchant payment metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentMetricsResponse, PaymentError>> getMerchantPaymentMetrics(
            String merchantId,
            com.hyperswitch.common.dto.PaymentMetricsRequest request);
    
    /**
     * Get org payment metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentMetricsResponse, PaymentError>> getOrgPaymentMetrics(
            String orgId,
            com.hyperswitch.common.dto.PaymentMetricsRequest request);
    
    /**
     * Get profile payment metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentMetricsResponse, PaymentError>> getProfilePaymentMetrics(
            String profileId,
            com.hyperswitch.common.dto.PaymentMetricsRequest request);
    
    /**
     * Get payment intent metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentIntentMetricsResponse, PaymentError>> getPaymentIntentMetrics(
            String merchantId,
            com.hyperswitch.common.dto.PaymentIntentMetricsRequest request);
    
    /**
     * Get merchant payment intent metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentIntentMetricsResponse, PaymentError>> getMerchantPaymentIntentMetrics(
            String merchantId,
            com.hyperswitch.common.dto.PaymentIntentMetricsRequest request);
    
    /**
     * Get org payment intent metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentIntentMetricsResponse, PaymentError>> getOrgPaymentIntentMetrics(
            String orgId,
            com.hyperswitch.common.dto.PaymentIntentMetricsRequest request);
    
    /**
     * Get profile payment intent metrics
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentIntentMetricsResponse, PaymentError>> getProfilePaymentIntentMetrics(
            String profileId,
            com.hyperswitch.common.dto.PaymentIntentMetricsRequest request);
    
    /**
     * Get refund metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RefundMetricsResponse, PaymentError>> getRefundMetrics(
            String merchantId,
            com.hyperswitch.common.dto.RefundMetricsRequest request);
    
    /**
     * Get merchant refund metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RefundMetricsResponse, PaymentError>> getMerchantRefundMetrics(
            String merchantId,
            com.hyperswitch.common.dto.RefundMetricsRequest request);
    
    /**
     * Get org refund metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RefundMetricsResponse, PaymentError>> getOrgRefundMetrics(
            String orgId,
            com.hyperswitch.common.dto.RefundMetricsRequest request);
    
    /**
     * Get profile refund metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RefundMetricsResponse, PaymentError>> getProfileRefundMetrics(
            String profileId,
            com.hyperswitch.common.dto.RefundMetricsRequest request);
    
    /**
     * Get routing metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingMetricsResponse, PaymentError>> getRoutingMetrics(
            String merchantId,
            com.hyperswitch.common.dto.RoutingMetricsRequest request);
    
    /**
     * Get merchant routing metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingMetricsResponse, PaymentError>> getMerchantRoutingMetrics(
            String merchantId,
            com.hyperswitch.common.dto.RoutingMetricsRequest request);
    
    /**
     * Get org routing metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingMetricsResponse, PaymentError>> getOrgRoutingMetrics(
            String orgId,
            com.hyperswitch.common.dto.RoutingMetricsRequest request);
    
    /**
     * Get profile routing metrics
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingMetricsResponse, PaymentError>> getProfileRoutingMetrics(
            String profileId,
            com.hyperswitch.common.dto.RoutingMetricsRequest request);
    
    /**
     * Get auth event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventMetricsResponse, PaymentError>> getAuthEventMetrics(
            String merchantId,
            com.hyperswitch.common.dto.AuthEventMetricsRequest request);
    
    /**
     * Get merchant auth event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventMetricsResponse, PaymentError>> getMerchantAuthEventMetrics(
            String merchantId,
            com.hyperswitch.common.dto.AuthEventMetricsRequest request);
    
    /**
     * Get org auth event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventMetricsResponse, PaymentError>> getOrgAuthEventMetrics(
            String orgId,
            com.hyperswitch.common.dto.AuthEventMetricsRequest request);
    
    /**
     * Get profile auth event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventMetricsResponse, PaymentError>> getProfileAuthEventMetrics(
            String profileId,
            com.hyperswitch.common.dto.AuthEventMetricsRequest request);
    
    /**
     * Get SDK event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.SdkEventMetricsResponse, PaymentError>> getSdkEventMetrics(
            String merchantId,
            com.hyperswitch.common.dto.SdkEventMetricsRequest request);
    
    /**
     * Get active payments metrics
     */
    Mono<Result<com.hyperswitch.common.dto.ActivePaymentsMetricsResponse, PaymentError>> getActivePaymentsMetrics(
            String merchantId,
            com.hyperswitch.common.dto.ActivePaymentsMetricsRequest request);
    
    /**
     * Get FRM metrics
     */
    Mono<Result<com.hyperswitch.common.dto.FrmMetricsResponse, PaymentError>> getFrmMetrics(
            String merchantId,
            com.hyperswitch.common.dto.FrmMetricsRequest request);
    
    /**
     * Get dispute metrics
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeMetricsResponse, PaymentError>> getDisputeMetrics(
            String merchantId,
            com.hyperswitch.common.dto.DisputeMetricsRequest request);
    
    /**
     * Get merchant dispute metrics
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeMetricsResponse, PaymentError>> getMerchantDisputeMetrics(
            String merchantId,
            com.hyperswitch.common.dto.DisputeMetricsRequest request);
    
    /**
     * Get org dispute metrics
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeMetricsResponse, PaymentError>> getOrgDisputeMetrics(
            String orgId,
            com.hyperswitch.common.dto.DisputeMetricsRequest request);
    
    /**
     * Get profile dispute metrics
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeMetricsResponse, PaymentError>> getProfileDisputeMetrics(
            String profileId,
            com.hyperswitch.common.dto.DisputeMetricsRequest request);
    
    /**
     * Get API event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventMetricsResponse, PaymentError>> getApiEventMetrics(
            String merchantId,
            com.hyperswitch.common.dto.ApiEventMetricsRequest request);
    
    /**
     * Get merchant API event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventMetricsResponse, PaymentError>> getMerchantApiEventMetrics(
            String merchantId,
            com.hyperswitch.common.dto.ApiEventMetricsRequest request);
    
    /**
     * Get org API event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventMetricsResponse, PaymentError>> getOrgApiEventMetrics(
            String orgId,
            com.hyperswitch.common.dto.ApiEventMetricsRequest request);
    
    /**
     * Get profile API event metrics
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventMetricsResponse, PaymentError>> getProfileApiEventMetrics(
            String profileId,
            com.hyperswitch.common.dto.ApiEventMetricsRequest request);
    
    /**
     * Get payment filters
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentFiltersResponse, PaymentError>> getPaymentFilters(
            String merchantId,
            com.hyperswitch.common.dto.PaymentFiltersRequest request);
    
    /**
     * Get merchant payment filters
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentFiltersResponse, PaymentError>> getMerchantPaymentFilters(
            String merchantId,
            com.hyperswitch.common.dto.PaymentFiltersRequest request);
    
    /**
     * Get org payment filters
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentFiltersResponse, PaymentError>> getOrgPaymentFilters(
            String orgId,
            com.hyperswitch.common.dto.PaymentFiltersRequest request);
    
    /**
     * Get profile payment filters
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentFiltersResponse, PaymentError>> getProfilePaymentFilters(
            String profileId,
            com.hyperswitch.common.dto.PaymentFiltersRequest request);
    
    /**
     * Get payment intent filters
     */
    Mono<Result<com.hyperswitch.common.dto.PaymentIntentFiltersResponse, PaymentError>> getPaymentIntentFilters(
            String merchantId,
            com.hyperswitch.common.dto.PaymentIntentFiltersRequest request);
    
    /**
     * Get refund filters
     */
    Mono<Result<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>> getRefundFilters(
            String merchantId,
            com.hyperswitch.common.dto.RefundFiltersRequest request);
    
    /**
     * Get merchant refund filters
     */
    Mono<Result<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>> getMerchantRefundFilters(
            String merchantId,
            com.hyperswitch.common.dto.RefundFiltersRequest request);
    
    /**
     * Get org refund filters
     */
    Mono<Result<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>> getOrgRefundFilters(
            String orgId,
            com.hyperswitch.common.dto.RefundFiltersRequest request);
    
    /**
     * Get profile refund filters
     */
    Mono<Result<com.hyperswitch.common.dto.RefundFiltersResponse, PaymentError>> getProfileRefundFilters(
            String profileId,
            com.hyperswitch.common.dto.RefundFiltersRequest request);
    
    /**
     * Get routing filters
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingFiltersResponse, PaymentError>> getRoutingFilters(
            String merchantId,
            com.hyperswitch.common.dto.RoutingFiltersRequest request);
    
    /**
     * Get merchant routing filters
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingFiltersResponse, PaymentError>> getMerchantRoutingFilters(
            String merchantId,
            com.hyperswitch.common.dto.RoutingFiltersRequest request);
    
    /**
     * Get org routing filters
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingFiltersResponse, PaymentError>> getOrgRoutingFilters(
            String orgId,
            com.hyperswitch.common.dto.RoutingFiltersRequest request);
    
    /**
     * Get profile routing filters
     */
    Mono<Result<com.hyperswitch.common.dto.RoutingFiltersResponse, PaymentError>> getProfileRoutingFilters(
            String profileId,
            com.hyperswitch.common.dto.RoutingFiltersRequest request);
    
    /**
     * Get auth event filters
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventFiltersResponse, PaymentError>> getAuthEventFilters(
            String merchantId,
            com.hyperswitch.common.dto.AuthEventFiltersRequest request);
    
    /**
     * Get merchant auth event filters
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventFiltersResponse, PaymentError>> getMerchantAuthEventFilters(
            String merchantId,
            com.hyperswitch.common.dto.AuthEventFiltersRequest request);
    
    /**
     * Get org auth event filters
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventFiltersResponse, PaymentError>> getOrgAuthEventFilters(
            String orgId,
            com.hyperswitch.common.dto.AuthEventFiltersRequest request);
    
    /**
     * Get profile auth event filters
     */
    Mono<Result<com.hyperswitch.common.dto.AuthEventFiltersResponse, PaymentError>> getProfileAuthEventFilters(
            String profileId,
            com.hyperswitch.common.dto.AuthEventFiltersRequest request);
    
    /**
     * Get SDK event filters
     */
    Mono<Result<com.hyperswitch.common.dto.SdkEventFiltersResponse, PaymentError>> getSdkEventFilters(
            String merchantId,
            com.hyperswitch.common.dto.SdkEventFiltersRequest request);
    
    /**
     * Get FRM filters
     */
    Mono<Result<com.hyperswitch.common.dto.FrmFiltersResponse, PaymentError>> getFrmFilters(
            String merchantId,
            com.hyperswitch.common.dto.FrmFiltersRequest request);
    
    /**
     * Get dispute filters
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeFiltersResponse, PaymentError>> getDisputeFilters(
            String merchantId,
            com.hyperswitch.common.dto.DisputeFiltersRequest request);
    
    /**
     * Get merchant dispute filters
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeFiltersResponse, PaymentError>> getMerchantDisputeFilters(
            String merchantId,
            com.hyperswitch.common.dto.DisputeFiltersRequest request);
    
    /**
     * Get org dispute filters
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeFiltersResponse, PaymentError>> getOrgDisputeFilters(
            String orgId,
            com.hyperswitch.common.dto.DisputeFiltersRequest request);
    
    /**
     * Get profile dispute filters
     */
    Mono<Result<com.hyperswitch.common.dto.DisputeFiltersResponse, PaymentError>> getProfileDisputeFilters(
            String profileId,
            com.hyperswitch.common.dto.DisputeFiltersRequest request);
    
    /**
     * Get API event filters
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventFiltersResponse, PaymentError>> getApiEventFilters(
            String merchantId,
            com.hyperswitch.common.dto.ApiEventFiltersRequest request);
    
    /**
     * Get merchant API event filters
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventFiltersResponse, PaymentError>> getMerchantApiEventFilters(
            String merchantId,
            com.hyperswitch.common.dto.ApiEventFiltersRequest request);
    
    /**
     * Get org API event filters
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventFiltersResponse, PaymentError>> getOrgApiEventFilters(
            String orgId,
            com.hyperswitch.common.dto.ApiEventFiltersRequest request);
    
    /**
     * Get profile API event filters
     */
    Mono<Result<com.hyperswitch.common.dto.ApiEventFiltersResponse, PaymentError>> getProfileApiEventFilters(
            String profileId,
            com.hyperswitch.common.dto.ApiEventFiltersRequest request);
}

