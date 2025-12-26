package com.hyperswitch.core.proxy;

import com.hyperswitch.common.dto.ProxyRequest;
import com.hyperswitch.common.dto.ProxyResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for proxy operations
 */
public interface ProxyService {
    
    /**
     * Proxy request
     */
    Mono<Result<ProxyResponse, PaymentError>> proxyRequest(
            String merchantId,
            ProxyRequest request);
}

