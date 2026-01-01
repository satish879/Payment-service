package com.hyperswitch.web.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Flyway configuration to ensure migrations run on startup
 * This ensures that database tables are created automatically
 * 
 * Note: When R2DBC is present, Spring Boot might not auto-configure Flyway.
 * This configuration explicitly creates a Flyway bean and runs migrations.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;

    /**
     * Create Flyway bean and run migrations explicitly
     * This ensures migrations run even when R2DBC is present
     */
    @Bean(initMethod = "migrate")
    @DependsOn("dataSource")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource) {
        log.info("=== Creating Flyway Bean ===");
        log.info("Flyway locations: {}", String.join(", ", locations));
        log.info("Baseline on migrate: {}", baselineOnMigrate);
        
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(validateOnMigrate)
                .load();
        
        log.info("Flyway bean created successfully");
        return flyway;
    }

    /**
     * Application runner to verify Flyway migrations completed successfully
     */
    @Bean
    public ApplicationRunner flywayMigrationVerifier(@Autowired(required = false) Flyway flyway) {
        return args -> {
            if (flyway == null) {
                log.warn("Flyway bean is not available - migrations may not have run");
                return;
            }
            
            try {
                log.info("=== Flyway Migration Verification ===");
                org.flywaydb.core.api.MigrationInfo[] migrations = flyway.info().all();
                log.info("Total migrations found: {}", migrations.length);
                
                org.flywaydb.core.api.MigrationInfo current = flyway.info().current();
                if (current != null) {
                    log.info("Current schema version: {} - {}", 
                        current.getVersion(), 
                        current.getDescription());
                } else {
                    log.warn("No migrations have been applied. Check Flyway configuration.");
                }
                
                // Log pending migrations
                org.flywaydb.core.api.MigrationInfo[] pending = flyway.info().pending();
                if (pending.length > 0) {
                    log.warn("Pending migrations found: {}", pending.length);
                    for (org.flywaydb.core.api.MigrationInfo migration : pending) {
                        log.warn("  - {}: {}", migration.getVersion(), migration.getDescription());
                    }
                } else {
                    log.info("All migrations are up to date");
                }
            } catch (Exception e) {
                log.error("Error verifying Flyway migrations", e);
            }
        };
    }
}

