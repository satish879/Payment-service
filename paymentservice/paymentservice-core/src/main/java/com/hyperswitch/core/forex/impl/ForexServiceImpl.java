package com.hyperswitch.core.forex.impl;

import com.hyperswitch.common.dto.ForexConvertResponse;
import com.hyperswitch.common.dto.ForexRatesResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.core.forex.ForexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ForexService
 */
@Service
public class ForexServiceImpl implements ForexService {
    
    private static final Logger log = LoggerFactory.getLogger(ForexServiceImpl.class);
    
    // In production, this would fetch from a forex API or database
    private static final Map<String, Double> DEFAULT_RATES = Map.of(
        "USD", 1.0,
        "EUR", 0.92,
        "GBP", 0.79,
        "INR", 83.0,
        "JPY", 150.0
    );
    
    @Override
    public Mono<Result<ForexRatesResponse, PaymentError>> getForexRates(
            String baseCurrency) {
        
        log.info("Getting forex rates for base currency: {}", baseCurrency);
        
        return Mono.fromCallable(() -> {
            ForexRatesResponse response = new ForexRatesResponse();
            response.setBaseCurrency(baseCurrency != null ? baseCurrency : "USD");
            
            Map<String, Double> rates = new HashMap<>(DEFAULT_RATES);
            // In production, this would fetch real-time rates from a forex API
            
            response.setRates(rates);
            response.setTimestamp(System.currentTimeMillis() / 1000);
            
            return Result.<ForexRatesResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error getting forex rates: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("FOREX_RATES_RETRIEVAL_FAILED",
                "Failed to get forex rates: " + error.getMessage())));
        });
    }
    
    @Override
    public Mono<Result<ForexConvertResponse, PaymentError>> convertFromMinor(
            Long amount,
            String fromCurrency,
            String toCurrency) {
        
        log.info("Converting {} {} to {}", amount, fromCurrency, toCurrency);
        
        return Mono.fromCallable(() -> {
            if (amount == null || amount < 0) {
                return Result.<ForexConvertResponse, PaymentError>err(
                    PaymentError.of("INVALID_AMOUNT", "Amount must be positive"));
            }
            
            Double fromRate = DEFAULT_RATES.getOrDefault(fromCurrency, 1.0);
            Double toRate = DEFAULT_RATES.getOrDefault(toCurrency, 1.0);
            
            if (fromRate == null || toRate == null) {
                return Result.<ForexConvertResponse, PaymentError>err(
                    PaymentError.of("UNSUPPORTED_CURRENCY", 
                        "Currency not supported: " + fromCurrency + " or " + toCurrency));
            }
            
            Double exchangeRate = toRate / fromRate;
            Long convertedAmount = Math.round(amount * exchangeRate);
            
            ForexConvertResponse response = new ForexConvertResponse();
            response.setAmount(amount);
            response.setCurrency(fromCurrency);
            response.setConvertedAmount(convertedAmount);
            response.setConvertedCurrency(toCurrency);
            response.setExchangeRate(exchangeRate);
            
            return Result.<ForexConvertResponse, PaymentError>ok(response);
        })
        .onErrorResume(error -> {
            log.error("Error converting currency: {}", error.getMessage(), error);
            return Mono.just(Result.err(PaymentError.of("FOREX_CONVERSION_FAILED",
                "Failed to convert currency: " + error.getMessage())));
        });
    }
}

