package com.hyperswitch.core.forex;

import com.hyperswitch.common.dto.ForexConvertResponse;
import com.hyperswitch.common.dto.ForexRatesResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for forex operations
 */
public interface ForexService {
    
    /**
     * Get forex rates
     */
    Mono<Result<ForexRatesResponse, PaymentError>> getForexRates(
            String baseCurrency);
    
    /**
     * Convert from minor currency units
     */
    Mono<Result<ForexConvertResponse, PaymentError>> convertFromMinor(
            Long amount,
            String fromCurrency,
            String toCurrency);
}

