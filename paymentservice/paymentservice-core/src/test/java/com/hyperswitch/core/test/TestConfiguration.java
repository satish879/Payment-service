package com.hyperswitch.core.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Test configuration for integration tests in the core module
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.hyperswitch")
@EnableR2dbcRepositories(basePackages = "com.hyperswitch.storage.repository")
public class TestConfiguration {
    // Empty - used as Spring Boot test configuration
}

