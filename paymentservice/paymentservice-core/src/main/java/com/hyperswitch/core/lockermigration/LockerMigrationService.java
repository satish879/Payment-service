package com.hyperswitch.core.lockermigration;

import com.hyperswitch.common.dto.LockerMigrationRequest;
import com.hyperswitch.common.dto.LockerMigrationResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for locker migration operations
 */
public interface LockerMigrationService {
    
    /**
     * Migrate locker data
     */
    Mono<Result<LockerMigrationResponse, PaymentError>> migrateLocker(
            String merchantId,
            LockerMigrationRequest request);
}

