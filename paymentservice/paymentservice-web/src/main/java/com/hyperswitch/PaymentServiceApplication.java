package com.hyperswitch;

import com.hyperswitch.storage.config.DatabaseConfig;
import com.hyperswitch.web.config.HealthCheckConfig;
import com.hyperswitch.web.config.SecurityConfig;
import com.hyperswitch.web.controller.HealthController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for Hyperswitch Payment Service
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.hyperswitch"})
@Import({SecurityConfig.class, HealthController.class, DatabaseConfig.class, HealthCheckConfig.class})
@EnableR2dbcRepositories
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

