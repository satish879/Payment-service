package com.hyperswitch;

import com.hyperswitch.connectors.config.ConnectorConfig;
import com.hyperswitch.storage.config.DatabaseConfig;
import com.hyperswitch.web.config.DataSourceConfig;
import com.hyperswitch.web.config.FlywayConfig;
import com.hyperswitch.web.config.HealthCheckConfig;
import com.hyperswitch.web.config.JacksonConfig;
import com.hyperswitch.web.config.MetricsConfig;
import com.hyperswitch.web.config.RedisConfig;
import com.hyperswitch.web.config.SecurityConfig;
import com.hyperswitch.web.config.WebClientConfig;
import com.hyperswitch.web.controller.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for Hyperswitch Payment Service
 * 
 * Note: All controllers are automatically discovered via @ComponentScan(basePackages = {"com.hyperswitch"})
 * The @Import annotation below explicitly imports critical controllers and configurations
 * to ensure they are registered even if component scanning has issues.
 * 
 * OpenTelemetry is disabled via application.yml (management.tracing.enabled=false)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.hyperswitch"})
@Import({
    // Configuration classes
    SecurityConfig.class,
    DatabaseConfig.class,
    DataSourceConfig.class,
    WebClientConfig.class,
    HealthCheckConfig.class,
    JacksonConfig.class,
    RedisConfig.class,
    MetricsConfig.class,
    FlywayConfig.class,
    ConnectorConfig.class,
    
    // Core Controllers
    HealthController.class,
    PaymentController.class,
    PaymentIntentController.class,
    PaymentSessionController.class,
    PaymentLinkController.class,
    
    // Customer & Payment Methods
    CustomerController.class,
    PaymentMethodController.class,
    PaymentMethodSessionController.class,
    
    // Refunds & Disputes
    RefundController.class,
    DisputeController.class,
    
    // Payouts & Subscriptions
    PayoutController.class,
    SubscriptionController.class,
    
    // Connectors & Webhooks
    ConnectorApiController.class,
    ConnectorWebhookController.class,
    WebhookController.class,
    WebhookEventController.class,
    WebhookEventAdvancedController.class,
    WebhookRelayController.class,
    WebhookRelayV2Controller.class,
    NetworkTokenWebhookController.class,
    RecoveryWebhookController.class,
    RecoveryWebhookV2Controller.class,
    
    // Routing
    RoutingController.class,
    RoutingV2Controller.class,
    DynamicRoutingController.class,
    
    // Analytics
    AnalyticsController.class,
    AnalyticsMetricsController.class,
    AnalyticsReportsController.class,
    AnalyticsEventLogsController.class,
    AnalyticsFiltersController.class,
    AnalyticsDomainInfoController.class,
    OlapController.class,
    
    // Admin & Platform
    AdminController.class,
    MerchantAccountController.class,
    MerchantAccountV2Controller.class,
    MerchantConnectorAccountController.class,
    ConnectorAccountV2Controller.class,
    OrganizationController.class,
    OrganizationV2Controller.class,
    ProfileController.class,
    ProfileV2Controller.class,
    ProfileNewController.class,
    ProfileAcquirerController.class,
    ApiKeyController.class,
    ApiKeyV2Controller.class,
    UserController.class,
    UserV2Controller.class,
    
    // Revenue Recovery & Reconciliation
    RevenueRecoveryController.class,
    RevenueRecoveryAdvancedController.class,
    RevenueRecoveryRedisController.class,
    ReconciliationController.class,
    
    // Fraud & Security
    FraudCheckController.class,
    AuthenticationController.class,
    EphemeralKeyController.class,
    
    // Advanced Features
    TokenizationV2Controller.class,
    ThreeDsDecisionRuleController.class,
    VerificationController.class,
    PollController.class,
    ConfigController.class,
    ConfigV2Controller.class,
    FileController.class,
    CacheController.class,
    CardInfoController.class,
    BlocklistController.class,
    
    // Infrastructure
    RelayController.class,
    ProxyController.class,
    HypersenseController.class,
    OidcController.class,
    ForexController.class,
    PayoutLinkController.class,
    FeatureMatrixController.class,
    ConnectorOnboardingController.class,
    LockerMigrationController.class,
    ProcessTrackerV2Controller.class,
    ProcessTrackerDeprecatedV2Controller.class,
    RecoveryDataBackfillV2Controller.class,
    ChatAIController.class,
    GsmController.class,
    GsmV2Controller.class,
    ApplePayCertificatesMigrationController.class,
    DummyConnectorController.class
})
@EnableR2dbcRepositories(basePackages = "com.hyperswitch.storage.repository")
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

