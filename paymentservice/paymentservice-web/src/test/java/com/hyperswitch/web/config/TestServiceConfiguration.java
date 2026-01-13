package com.hyperswitch.web.config;

import com.hyperswitch.core.paymentmethods.PaymentMethodService;
import com.hyperswitch.core.payments.PaymentService;
import com.hyperswitch.core.revenuerecovery.RevenueRecoveryService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Centralized test configuration providing mock services for integration tests.
 * This ensures all controllers have the required dependencies during test execution.
 */
@TestConfiguration
public class TestServiceConfiguration {

    @Bean
    @Primary
    public PaymentService paymentService() {
        return org.mockito.Mockito.mock(PaymentService.class);
    }

    @Bean
    @Primary
    public PaymentMethodService paymentMethodService() {
        return org.mockito.Mockito.mock(PaymentMethodService.class);
    }

    @Bean
    @Primary
    public RevenueRecoveryService revenueRecoveryService() {
        return org.mockito.Mockito.mock(RevenueRecoveryService.class);
    }
}