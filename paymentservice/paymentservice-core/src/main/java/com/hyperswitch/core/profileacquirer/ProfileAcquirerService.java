package com.hyperswitch.core.profileacquirer;

import com.hyperswitch.common.dto.ProfileAcquirerRequest;
import com.hyperswitch.common.dto.ProfileAcquirerResponse;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import reactor.core.publisher.Mono;

/**
 * Service interface for profile acquirer operations
 */
public interface ProfileAcquirerService {
    
    /**
     * Create profile acquirer
     */
    Mono<Result<ProfileAcquirerResponse, PaymentError>> createProfileAcquirer(
            String merchantId,
            String profileId,
            ProfileAcquirerRequest request);
    
    /**
     * Update profile acquirer
     */
    Mono<Result<ProfileAcquirerResponse, PaymentError>> updateProfileAcquirer(
            String merchantId,
            String profileId,
            String profileAcquirerId,
            ProfileAcquirerRequest request);
}

