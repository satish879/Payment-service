package com.hyperswitch.core.applepay;

import com.hyperswitch.common.dto.ApplePayCertificatesMigrationRequest;
import com.hyperswitch.common.dto.ApplePayCertificatesMigrationResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for Apple Pay certificates migration
 */
public interface ApplePayCertificatesMigrationService {
    
    /**
     * Migrate Apple Pay certificates for specified merchants
     */
    Mono<Result<ApplePayCertificatesMigrationResponse, PaymentError>> migrateApplePayCertificates(
            ApplePayCertificatesMigrationRequest request);
}

