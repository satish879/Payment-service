package com.hyperswitch.storage.repository;

import com.hyperswitch.storage.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for user entities
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<UserEntity, String> {
    
    /**
     * Find user by email
     */
    Mono<UserEntity> findByEmail(String email);
    
    /**
     * Check if user exists by email
     */
    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    Mono<Boolean> existsByEmail(String email);
}

