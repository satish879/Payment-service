package com.hyperswitch.routing.impl;

import com.hyperswitch.common.enums.Connector;
import com.hyperswitch.common.dto.CreatePaymentRequest;
import com.hyperswitch.common.dto.*;
import com.hyperswitch.common.analytics.AnalyticsService;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.common.types.Result;
import com.hyperswitch.routing.RoutingAlgorithm;
import com.hyperswitch.routing.RoutingService;
import com.hyperswitch.storage.entity.ConnectorSuccessRateEntity;
import com.hyperswitch.storage.entity.DecisionManagerConfigEntity;
import com.hyperswitch.storage.entity.RoutingAlgorithmEntity;
import com.hyperswitch.storage.entity.RoutingConfigEntity;
import com.hyperswitch.storage.repository.ConnectorSuccessRateRepository;
import com.hyperswitch.storage.repository.DecisionManagerConfigRepository;
import com.hyperswitch.storage.repository.RoutingAlgorithmRepository;
import com.hyperswitch.storage.repository.RoutingConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Implementation of RoutingService
 * Provides intelligent routing to select the best payment processor
 */
@Service
public class RoutingServiceImpl implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingServiceImpl.class);
    private static final Random random = new Random();

    @Value("${hyperswitch.routing.algorithm:SUCCESS_RATE_BASED}")
    private RoutingAlgorithm algorithm;
    
    @Value("${hyperswitch.routing.window.duration.minutes:60}")
    private int windowDurationMinutes;

    private final RoutingConfigRepository routingConfigRepository;
    private final ConnectorSuccessRateRepository successRateRepository;
    private AnalyticsService analyticsService;
    private final RoutingAlgorithmRepository routingAlgorithmRepository;
    private final DecisionManagerConfigRepository decisionManagerConfigRepository;

    public RoutingServiceImpl(
            RoutingConfigRepository routingConfigRepository,
            ConnectorSuccessRateRepository successRateRepository,
            RoutingAlgorithmRepository routingAlgorithmRepository,
            DecisionManagerConfigRepository decisionManagerConfigRepository) {
        this.routingConfigRepository = routingConfigRepository;
        this.successRateRepository = successRateRepository;
        this.routingAlgorithmRepository = routingAlgorithmRepository;
        this.decisionManagerConfigRepository = decisionManagerConfigRepository;
    }
    
    @Autowired(required = false)
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        if (analyticsService != null) {
            log.info("AnalyticsService injected into RoutingServiceImpl");
        } else {
            log.warn("AnalyticsService not available - RoutingServiceImpl will use fallback success rate calculation");
        }
    }

    @Override
    public Mono<List<Connector>> selectConnectors(CreatePaymentRequest request, String merchantId) {
        log.info("Selecting connector for payment: merchant={}, amount={}", 
            merchantId, request.getAmount());
        
        return Mono.fromCallable(() -> {
            List<Connector> connectors = new ArrayList<>();
            
            switch (algorithm) {
                case PRIORITY_BASED:
                    connectors = priorityBasedRouting(request, merchantId);
                    break;
                case SUCCESS_RATE_BASED:
                    connectors = successRateBasedRouting(request, merchantId);
                    break;
                case VOLUME_BASED:
                    connectors = volumeBasedRouting(request, merchantId);
                    break;
                case RULE_BASED:
                    connectors = ruleBasedRouting(request, merchantId);
                    break;
                default:
                    connectors = getDefaultConnectors();
            }
            
            return connectors;
        });
    }

    @Override
    public RoutingAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Priority-based routing - returns connectors in priority order
     */
    private List<Connector> priorityBasedRouting(CreatePaymentRequest request, String merchantId) {
        return routingConfigRepository
            .findByMerchantIdAndEnabledTrue(merchantId)
            .filter(config -> matchesAmount(config, request.getAmount().getValue()))
            .filter(config -> matchesCurrency(config, request.getAmount().getCurrencyCode()))
            .filter(config -> matchesPaymentMethod(config, request.getPaymentMethod()))
            .sort(Comparator.comparing(RoutingConfigEntity::getPriority).reversed())
            .map(RoutingConfigEntity::getConnector)
            .map(Connector::valueOf)
            .collectList()
            .blockOptional()
            .orElseGet(this::getDefaultConnectors);
    }

    /**
     * Success-rate-based routing - selects connector with highest success rate
     * Uses time-window based metrics for real-time success rate tracking
     */
    private List<Connector> successRateBasedRouting(CreatePaymentRequest request, String merchantId) {
        String profileId = request.getMetadata() != null 
            ? (String) request.getMetadata().getOrDefault("profile_id", null) 
            : null;
        String paymentMethod = request.getPaymentMethod() != null 
            ? request.getPaymentMethod().name() 
            : null;
        String currency = request.getAmount().getCurrencyCode();
        
        return routingConfigRepository
            .findByMerchantIdAndEnabledTrue(merchantId)
            .filter(config -> matchesAmount(config, request.getAmount().getValue()))
            .filter(config -> matchesCurrency(config, currency))
            .filter(config -> matchesPaymentMethod(config, request.getPaymentMethod()))
            .flatMap(config -> {
                Connector connector = Connector.valueOf(config.getConnector());
                
                // Try to get windowed success rate first (real-time metrics)
                // Only if AnalyticsService is available
                Mono<BigDecimal> windowedRate = (profileId != null && analyticsService != null)
                    ? analyticsService.getWindowedSuccessRate(
                        profileId, config.getConnector(), paymentMethod, currency, windowDurationMinutes)
                    : Mono.just(java.math.BigDecimal.ZERO);
                
                // Fallback to overall success rate if windowed rate is not available
                Mono<BigDecimal> overallRate = successRateRepository
                    .findByMerchantIdAndConnectorAndProfileIdAndPaymentMethodAndCurrency(
                        merchantId, config.getConnector(), profileId, paymentMethod, currency
                    )
                    .map(ConnectorSuccessRateEntity::getSuccessRate)
                    .defaultIfEmpty(java.math.BigDecimal.ZERO);
                
                // Use windowed rate if available and > 0, otherwise use overall rate
                return windowedRate
                    .flatMap(wr -> {
                        if (wr.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            return Mono.just(wr);
                        }
                        return overallRate;
                    })
                    .defaultIfEmpty(java.math.BigDecimal.ZERO)
                    .map(rate -> new ConnectorWithRate(connector, rate, config.getPriority()));
            })
            .sort(Comparator
                .comparing((ConnectorWithRate c) -> c.successRate)
                .thenComparing(c -> c.priority)
                .reversed())
            .map(c -> c.connector)
            .collectList()
            .blockOptional()
            .orElseGet(this::getDefaultConnectors);
    }

    /**
     * Volume-based routing - splits traffic based on volume percentages
     */
    private List<Connector> volumeBasedRouting(CreatePaymentRequest request, String merchantId) {
        return routingConfigRepository
            .findByMerchantIdAndEnabledTrue(merchantId)
            .filter(config -> matchesAmount(config, request.getAmount().getValue()))
            .filter(config -> matchesCurrency(config, request.getAmount().getCurrencyCode()))
            .filter(config -> matchesPaymentMethod(config, request.getPaymentMethod()))
            .filter(config -> config.getVolumePercentage() != null && config.getVolumePercentage().compareTo(BigDecimal.ZERO) > 0)
            .collectList()
            .map(configs -> {
                if (configs.isEmpty()) {
                    return getDefaultConnectors();
                }
                
                // Calculate cumulative percentages
                BigDecimal total = configs.stream()
                    .map(RoutingConfigEntity::getVolumePercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (total.compareTo(BigDecimal.ZERO) == 0) {
                    return getDefaultConnectors();
                }
                
                // Select connector based on random value and volume percentages
                BigDecimal randomValue = BigDecimal.valueOf(random.nextDouble() * 100);
                BigDecimal cumulative = BigDecimal.ZERO;
                
                for (RoutingConfigEntity config : configs) {
                    cumulative = cumulative.add(config.getVolumePercentage());
                    if (randomValue.compareTo(cumulative) <= 0) {
                        return List.of(Connector.valueOf(config.getConnector()));
                    }
                }
                
                // Fallback to first connector
                return List.of(Connector.valueOf(configs.get(0).getConnector()));
            })
            .blockOptional()
            .orElseGet(this::getDefaultConnectors);
    }

    /**
     * Rule-based routing - uses DSL rules to select connector
     */
    private List<Connector> ruleBasedRouting(CreatePaymentRequest request, String merchantId) {
        // Rule-based routing with priority and filtering
        return routingConfigRepository
            .findByMerchantIdAndEnabledTrue(merchantId)
            .filter(config -> matchesAmount(config, request.getAmount().getValue())
                && matchesCurrency(config, request.getAmount().getCurrencyCode())
                && matchesPaymentMethod(config, request.getPaymentMethod()))
            .sort(Comparator.comparing(RoutingConfigEntity::getPriority).reversed())
            .map(RoutingConfigEntity::getConnector)
            .map(Connector::valueOf)
            .collectList()
            .blockOptional()
            .orElseGet(this::getDefaultConnectors);
    }

    /**
     * Default connector list
     */
    private List<Connector> getDefaultConnectors() {
        return List.of(Connector.STRIPE);
    }

    private boolean matchesAmount(RoutingConfigEntity config, java.math.BigDecimal amount) {
        return (config.getMinAmount() == null || amount.compareTo(java.math.BigDecimal.valueOf(config.getMinAmount())) >= 0)
            && (config.getMaxAmount() == null || amount.compareTo(java.math.BigDecimal.valueOf(config.getMaxAmount())) <= 0);
    }

    private boolean matchesCurrency(RoutingConfigEntity config, String currency) {
        return config.getCurrency() == null || config.getCurrency().equals(currency);
    }

    private boolean matchesPaymentMethod(RoutingConfigEntity config, com.hyperswitch.common.enums.PaymentMethod paymentMethod) {
        if (config.getPaymentMethod() == null) {
            return true;
        }
        return paymentMethod != null && config.getPaymentMethod().equals(paymentMethod.name());
    }

    /**
     * Helper class for sorting connectors by success rate
     */
    private static class ConnectorWithRate {
        final Connector connector;
        final BigDecimal successRate;
        final Integer priority;

        ConnectorWithRate(Connector connector, BigDecimal successRate, Integer priority) {
            this.connector = connector;
            this.successRate = successRate;
            this.priority = priority;
        }
    }
    
    // ========== Routing Configuration Methods ==========
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> createRoutingConfig(
            String merchantId, RoutingConfigRequest request) {
        log.info("Creating routing config for merchant: {}", merchantId);
        
        String algorithmId = "routing_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        
        RoutingAlgorithmEntity entity = new RoutingAlgorithmEntity();
        entity.setAlgorithmId(algorithmId);
        entity.setMerchantId(merchantId);
        entity.setProfileId(request.getProfileId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAlgorithmData(request.getAlgorithm());
        entity.setKind(determineAlgorithmKind(request.getAlgorithm()));
        entity.setAlgorithmFor(request.getTransactionType() != null ? request.getTransactionType() : "payment");
        entity.setIsActive(false);
        entity.setIsDefault(false);
        entity.setCreatedAt(now);
        entity.setModifiedAt(now);
        
        return routingAlgorithmRepository.save(entity)
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error creating routing config", error);
                return Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                    PaymentError.of("ROUTING_CONFIG_CREATE_FAILED", error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<Flux<RoutingConfigResponse>, PaymentError>> listRoutingConfigs(
            String merchantId, Integer limit, Integer offset, String transactionType) {
        log.info("Listing routing configs for merchant: {}", merchantId);
        
        Flux<RoutingAlgorithmEntity> query = routingAlgorithmRepository.findByMerchantId(merchantId);
        
        if (transactionType != null && !transactionType.isEmpty()) {
            query = query.filter(e -> transactionType.equals(e.getAlgorithmFor()));
        }
        
        Flux<RoutingConfigResponse> response = query
            .skip(offset != null ? offset : 0)
            .take(limit != null ? limit : 100)
            .map(this::toRoutingConfigResponse);
        
        return Mono.just(Result.<Flux<RoutingConfigResponse>, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> getRoutingConfig(
            String merchantId, String algorithmId) {
        log.info("Getting routing config: {} for merchant: {}", algorithmId, merchantId);
        
        return routingAlgorithmRepository.findById(algorithmId)
            .filter(e -> merchantId.equals(e.getMerchantId()))
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("ROUTING_CONFIG_NOT_FOUND", "Routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> activateRoutingAlgorithm(
            String merchantId, String algorithmId, RoutingActivateRequest request) {
        log.info("Activating routing algorithm: {} for merchant: {}", algorithmId, merchantId);
        
        return routingAlgorithmRepository.findById(algorithmId)
            .filter(e -> merchantId.equals(e.getMerchantId()))
            .flatMap(entity -> 
                // Deactivate all other active configs for this transaction type
                routingAlgorithmRepository
                    .findByMerchantId(merchantId)
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive()) 
                        && (request.getTransactionType() == null 
                            || request.getTransactionType().equals(e.getAlgorithmFor())))
                    .flatMap(e -> {
                        e.setIsActive(false);
                        e.setModifiedAt(Instant.now());
                        return routingAlgorithmRepository.save(e);
                    })
                    .then(Mono.just(entity))
            )
            .flatMap(entity -> {
                entity.setIsActive(true);
                entity.setModifiedAt(Instant.now());
                return routingAlgorithmRepository.save(entity);
            })
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("ROUTING_CONFIG_NOT_FOUND", "Routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> deactivateRoutingConfig(
            String merchantId, RoutingActivateRequest request) {
        log.info("Deactivating routing configs for merchant: {}", merchantId);
        
        return routingAlgorithmRepository
            .findByMerchantId(merchantId)
            .filter(e -> Boolean.TRUE.equals(e.getIsActive())
                && (request.getTransactionType() == null 
                    || request.getTransactionType().equals(e.getAlgorithmFor())))
            .flatMap(entity -> {
                entity.setIsActive(false);
                entity.setModifiedAt(Instant.now());
                return routingAlgorithmRepository.save(entity);
            })
            .then(Mono.just(Result.<Void, PaymentError>ok(null)));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> setDefaultRoutingConfig(
            String merchantId, RoutingDefaultRequest request) {
        log.info("Setting default routing config for merchant: {}", merchantId);
        
        return routingAlgorithmRepository.findById(request.getAlgorithmId())
            .filter(e -> merchantId.equals(e.getMerchantId()))
            .flatMap(entity -> 
                // Unset other defaults for this transaction type
                routingAlgorithmRepository
                    .findByMerchantId(merchantId)
                    .filter(e -> Boolean.TRUE.equals(e.getIsDefault())
                        && (request.getTransactionType() == null 
                            || request.getTransactionType().equals(e.getAlgorithmFor())))
                    .flatMap(e -> {
                        e.setIsDefault(false);
                        e.setModifiedAt(Instant.now());
                        return routingAlgorithmRepository.save(e);
                    })
                    .then(Mono.just(entity))
            )
            .flatMap(entity -> {
                entity.setIsDefault(true);
                entity.setModifiedAt(Instant.now());
                return routingAlgorithmRepository.save(entity);
            })
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("ROUTING_CONFIG_NOT_FOUND", "Routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> getDefaultRoutingConfig(String merchantId) {
        log.info("Getting default routing config for merchant: {}", merchantId);
        
        return routingAlgorithmRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("DEFAULT_ROUTING_CONFIG_NOT_FOUND", "Default routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> getActiveRoutingConfig(String merchantId) {
        log.info("Getting active routing config for merchant: {}", merchantId);
        
        return routingAlgorithmRepository.findByMerchantIdAndIsActiveTrue(merchantId)
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("ACTIVE_ROUTING_CONFIG_NOT_FOUND", "Active routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<Flux<RoutingConfigResponse>, PaymentError>> listRoutingConfigsForProfile(
            String merchantId, Integer limit, Integer offset) {
        log.info("Listing routing configs for profile, merchant: {}", merchantId);
        
        Flux<RoutingConfigResponse> response = routingAlgorithmRepository
            .findByMerchantId(merchantId)
            .skip(offset != null ? offset : 0)
            .take(limit != null ? limit : 100)
            .map(this::toRoutingConfigResponse);
        
        return Mono.just(Result.<Flux<RoutingConfigResponse>, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> setDefaultRoutingForProfile(
            String merchantId, String profileId, RoutingDefaultRequest request) {
        log.info("Setting default routing for profile: {} merchant: {}", profileId, merchantId);
        
        return routingAlgorithmRepository.findById(request.getAlgorithmId())
            .filter(e -> merchantId.equals(e.getMerchantId()) 
                && (profileId == null || profileId.equals(e.getProfileId())))
            .flatMap(entity -> 
                // Unset other defaults for this profile
                routingAlgorithmRepository
                    .findByMerchantIdAndProfileId(merchantId, profileId)
                    .filter(e -> Boolean.TRUE.equals(e.getIsDefault()))
                    .flatMap(e -> {
                        e.setIsDefault(false);
                        e.setModifiedAt(Instant.now());
                        return routingAlgorithmRepository.save(e);
                    })
                    .then(Mono.just(entity))
            )
            .flatMap(entity -> {
                entity.setIsDefault(true);
                entity.setModifiedAt(Instant.now());
                return routingAlgorithmRepository.save(entity);
            })
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("ROUTING_CONFIG_NOT_FOUND", "Routing config not found")
            )));
    }
    
    @Override
    public Mono<Result<RoutingConfigResponse, PaymentError>> getDefaultRoutingForProfile(String merchantId) {
        log.info("Getting default routing for profile, merchant: {}", merchantId);
        
        return routingAlgorithmRepository
            .findByMerchantId(merchantId)
            .filter(e -> Boolean.TRUE.equals(e.getIsDefault()))
            .next()
            .map(this::toRoutingConfigResponse)
            .map(Result::<RoutingConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<RoutingConfigResponse, PaymentError>err(
                PaymentError.of("DEFAULT_ROUTING_NOT_FOUND", "Default routing not found")
            )));
    }
    
    // ========== Decision Manager Config Methods ==========
    
    @Override
    public Mono<Result<DecisionManagerConfigResponse, PaymentError>> upsertDecisionManagerConfig(
            String merchantId, DecisionManagerConfigRequest request) {
        log.info("Upserting decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository
            .findByMerchantIdAndConfigType(merchantId, "standard")
            .switchIfEmpty(Mono.defer(() -> {
                DecisionManagerConfigEntity newEntity = new DecisionManagerConfigEntity();
                newEntity.setId(UUID.randomUUID().toString());
                newEntity.setMerchantId(merchantId);
                newEntity.setConfigType("standard");
                newEntity.setConfigData(request.getConfig());
                newEntity.setCreatedAt(Instant.now());
                newEntity.setModifiedAt(Instant.now());
                return Mono.just(newEntity);
            }))
            .flatMap(entity -> {
                entity.setConfigData(request.getConfig());
                entity.setModifiedAt(Instant.now());
                return decisionManagerConfigRepository.save(entity);
            })
            .map(this::toDecisionManagerConfigResponse)
            .map(Result::<DecisionManagerConfigResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error upserting decision manager config", error);
                return Mono.just(Result.<DecisionManagerConfigResponse, PaymentError>err(
                    PaymentError.of("DECISION_MANAGER_CONFIG_UPSERT_FAILED", error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<DecisionManagerConfigResponse, PaymentError>> getDecisionManagerConfig(String merchantId) {
        log.info("Getting decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository.findByMerchantIdAndConfigType(merchantId, "standard")
            .map(this::toDecisionManagerConfigResponse)
            .map(Result::<DecisionManagerConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<DecisionManagerConfigResponse, PaymentError>err(
                PaymentError.of("DECISION_MANAGER_CONFIG_NOT_FOUND", "Decision manager config not found")
            )));
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> deleteDecisionManagerConfig(String merchantId) {
        log.info("Deleting decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository
            .findByMerchantIdAndConfigType(merchantId, "standard")
            .flatMap(decisionManagerConfigRepository::delete)
            .then(Mono.just(Result.<Void, PaymentError>ok(null)))
            .switchIfEmpty(Mono.just(Result.<Void, PaymentError>err(
                PaymentError.of("DECISION_MANAGER_CONFIG_NOT_FOUND", "Decision manager config not found")
            )));
    }
    
    @Override
    public Mono<Result<DecisionManagerConfigResponse, PaymentError>> upsertSurchargeDecisionManagerConfig(
            String merchantId, DecisionManagerConfigRequest request) {
        log.info("Upserting surcharge decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository
            .findByMerchantIdAndConfigType(merchantId, "surcharge")
            .switchIfEmpty(Mono.defer(() -> {
                DecisionManagerConfigEntity newEntity = new DecisionManagerConfigEntity();
                newEntity.setId(UUID.randomUUID().toString());
                newEntity.setMerchantId(merchantId);
                newEntity.setConfigType("surcharge");
                newEntity.setConfigData(request.getConfig());
                newEntity.setCreatedAt(Instant.now());
                newEntity.setModifiedAt(Instant.now());
                return Mono.just(newEntity);
            }))
            .flatMap(entity -> {
                entity.setConfigData(request.getConfig());
                entity.setModifiedAt(Instant.now());
                return decisionManagerConfigRepository.save(entity);
            })
            .map(this::toDecisionManagerConfigResponse)
            .map(Result::<DecisionManagerConfigResponse, PaymentError>ok)
            .onErrorResume(error -> {
                log.error("Error upserting surcharge decision manager config", error);
                return Mono.just(Result.<DecisionManagerConfigResponse, PaymentError>err(
                    PaymentError.of("SURCHARGE_DECISION_MANAGER_CONFIG_UPSERT_FAILED", error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<DecisionManagerConfigResponse, PaymentError>> getSurchargeDecisionManagerConfig(String merchantId) {
        log.info("Getting surcharge decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository.findByMerchantIdAndConfigType(merchantId, "surcharge")
            .map(this::toDecisionManagerConfigResponse)
            .map(Result::<DecisionManagerConfigResponse, PaymentError>ok)
            .switchIfEmpty(Mono.just(Result.<DecisionManagerConfigResponse, PaymentError>err(
                PaymentError.of("SURCHARGE_DECISION_MANAGER_CONFIG_NOT_FOUND", 
                    "Surcharge decision manager config not found")
            )));
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> deleteSurchargeDecisionManagerConfig(String merchantId) {
        log.info("Deleting surcharge decision manager config for merchant: {}", merchantId);
        
        return decisionManagerConfigRepository
            .findByMerchantIdAndConfigType(merchantId, "surcharge")
            .flatMap(decisionManagerConfigRepository::delete)
            .then(Mono.just(Result.<Void, PaymentError>ok(null)))
            .switchIfEmpty(Mono.just(Result.<Void, PaymentError>err(
                PaymentError.of("SURCHARGE_DECISION_MANAGER_CONFIG_NOT_FOUND", 
                    "Surcharge decision manager config not found")
            )));
    }
    
    // ========== Routing Evaluation and Gateway Score Methods ==========
    
    @Override
    public Mono<Result<RoutingEvaluationResponse, PaymentError>> evaluateRoutingRule(
            String merchantId, RoutingEvaluationRequest request) {
        log.info("Evaluating routing rule for merchant: {}", merchantId);
        
        // Simple rule evaluation - in production this would use a rule engine
        RoutingEvaluationResponse response = new RoutingEvaluationResponse();
        List<String> connectors = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        
        // Basic evaluation logic
        if (request.getRule() != null && request.getPaymentRequest() != null) {
            // Extract connector from rule if available
            Object connectorObj = request.getRule().get("connector");
            if (connectorObj instanceof String string) {
                connectors.add(string);
            } else if (connectorObj instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> stringList = (List<String>) list;
                connectors.addAll(stringList);
            }
            
            result.put("matched", true);
            result.put("rule", request.getRule());
        }
        
        response.setConnectors(connectors);
        response.setResult(result);
        
        return Mono.just(Result.<RoutingEvaluationResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> updateGatewayScore(
            String merchantId, GatewayScoreUpdateRequest request) {
        log.info("Updating gateway score for merchant: {}, connector: {}", merchantId, request.getConnector());
        
        // Update success rate based on gateway score feedback
        return successRateRepository
            .findByMerchantIdAndConnectorAndProfileIdAndPaymentMethodAndCurrency(
                merchantId, request.getConnector(), null, null, null)
            .switchIfEmpty(Mono.defer(() -> {
                ConnectorSuccessRateEntity newEntity = new ConnectorSuccessRateEntity();
                newEntity.setId(UUID.randomUUID().toString());
                newEntity.setMerchantId(merchantId);
                newEntity.setConnector(request.getConnector());
                newEntity.setSuccessRate(BigDecimal.valueOf(request.getScore()));
                return Mono.just(newEntity);
            }))
            .flatMap(entity -> {
                // Update success rate based on feedback
                BigDecimal currentRate = entity.getSuccessRate() != null 
                    ? entity.getSuccessRate() 
                    : BigDecimal.ZERO;
                BigDecimal newRate = request.getScore() != null 
                    ? BigDecimal.valueOf(request.getScore()) 
                    : currentRate;
                
                // Simple averaging - in production use more sophisticated algorithm
                if (Boolean.TRUE.equals(request.getSuccess())) {
                    newRate = currentRate.add(BigDecimal.valueOf(0.1))
                        .min(BigDecimal.ONE);
                } else {
                    newRate = currentRate.subtract(BigDecimal.valueOf(0.1))
                        .max(BigDecimal.ZERO);
                }
                
                entity.setSuccessRate(newRate);
                return successRateRepository.save(entity);
            })
            .then(Mono.just(Result.<Void, PaymentError>ok(null)))
            .onErrorResume(error -> {
                log.error("Error updating gateway score", error);
                return Mono.just(Result.<Void, PaymentError>err(
                    PaymentError.of("GATEWAY_SCORE_UPDATE_FAILED", error.getMessage())
                ));
            });
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> migrateRoutingRules(
            String merchantId, RoutingRuleMigrationRequest request) {
        log.info("Migrating routing rules for merchant: {}", merchantId);
        
        // In production, this would migrate rules from source to target algorithm
        // For now, just return success
        return Mono.just(Result.<Void, PaymentError>ok(null))
            .onErrorResume(error -> {
                log.error("Error migrating routing rules", error);
                return Mono.just(Result.<Void, PaymentError>err(
                    PaymentError.of("ROUTING_RULE_MIGRATION_FAILED", error.getMessage())
                ));
            });
    }
    
    // ========== Helper Methods ==========
    
    private RoutingConfigResponse toRoutingConfigResponse(RoutingAlgorithmEntity entity) {
        RoutingConfigResponse response = new RoutingConfigResponse();
        response.setAlgorithmId(entity.getAlgorithmId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setAlgorithm(entity.getAlgorithmData());
        response.setProfileId(entity.getProfileId());
        response.setTransactionType(entity.getAlgorithmFor());
        response.setIsActive(Boolean.TRUE.equals(entity.getIsActive()));
        response.setIsDefault(Boolean.TRUE.equals(entity.getIsDefault()));
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        return response;
    }
    
    private DecisionManagerConfigResponse toDecisionManagerConfigResponse(DecisionManagerConfigEntity entity) {
        DecisionManagerConfigResponse response = new DecisionManagerConfigResponse();
        response.setConfig(entity.getConfigData());
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        return response;
    }
    
    private String determineAlgorithmKind(Map<String, Object> algorithm) {
        if (algorithm == null) {
            return "single";
        }
        if (algorithm.containsKey("priority")) {
            return "priority";
        }
        if (algorithm.containsKey("volume_split")) {
            return "volume_split";
        }
        if (algorithm.containsKey("rules")) {
            return "advanced";
        }
        return "single";
    }
    
    @Override
    public Mono<Result<Flux<PayoutRoutingResponse>, PaymentError>> listPayoutRoutings(String merchantId) {
        log.info("Listing payout routing configurations for merchant: {}", merchantId);
        
        // In production, this would query payout routing configurations from database
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId("payout_alg_" + UUID.randomUUID().toString().substring(0, 8));
        response.setIsDefault(Boolean.FALSE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<Flux<PayoutRoutingResponse>, PaymentError>ok(Flux.just(response)));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> createPayoutRouting(
            String merchantId,
            PayoutRoutingRequest request) {
        log.info("Creating payout routing configuration for merchant: {}", merchantId);
        
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId() != null ? request.getAlgorithmId() : 
            "payout_alg_" + UUID.randomUUID().toString().substring(0, 8));
        response.setProfileId(request.getProfileId());
        response.setConfig(request.getConfig());
        response.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : Boolean.FALSE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> getActivePayoutRouting(String merchantId) {
        log.info("Getting active payout routing for merchant: {}", merchantId);
        
        // In production, this would query active payout routing from database
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId("payout_alg_active");
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> getDefaultPayoutRouting(String merchantId) {
        log.info("Getting default payout routing for merchant: {}", merchantId);
        
        // In production, this would query default payout routing from database
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId("payout_alg_default");
        response.setIsDefault(Boolean.TRUE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> setDefaultPayoutRouting(
            String merchantId,
            PayoutRoutingRequest request) {
        log.info("Setting default payout routing for merchant: {}", merchantId);
        
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId());
        response.setProfileId(request.getProfileId());
        response.setConfig(request.getConfig());
        response.setIsDefault(Boolean.TRUE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> activatePayoutRouting(
            String merchantId,
            String algorithmId) {
        log.info("Activating payout routing: {} for merchant: {}", algorithmId, merchantId);
        
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId(algorithmId);
        response.setIsActive(Boolean.TRUE);
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<Void, PaymentError>> deactivatePayoutRouting(String merchantId) {
        log.info("Deactivating payout routing for merchant: {}", merchantId);
        
        // In production, this would deactivate all payout routing configurations
        return Mono.just(Result.<Void, PaymentError>ok(null));
    }
    
    @Override
    public Mono<Result<PayoutRoutingResponse, PaymentError>> setDefaultPayoutRoutingForProfile(
            String merchantId,
            String profileId,
            PayoutRoutingRequest request) {
        log.info("Setting default payout routing for profile: {} in merchant: {}", profileId, merchantId);
        
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId());
        response.setProfileId(profileId);
        response.setConfig(request.getConfig());
        response.setIsDefault(Boolean.TRUE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<PayoutRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<Flux<PayoutRoutingResponse>, PaymentError>> getDefaultPayoutRoutingForProfiles(String merchantId) {
        log.info("Getting default payout routing for profiles in merchant: {}", merchantId);
        
        // In production, this would query default payout routing for all profiles
        PayoutRoutingResponse response = new PayoutRoutingResponse();
        response.setAlgorithmId("payout_alg_profile_default");
        response.setIsDefault(Boolean.TRUE);
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<Flux<PayoutRoutingResponse>, PaymentError>ok(Flux.just(response)));
    }
    
    @Override
    public Mono<Result<RoutingAlgorithmV2Response, PaymentError>> createRoutingAlgorithmV2(
            String merchantId,
            RoutingAlgorithmV2Request request) {
        log.info("Creating routing algorithm (v2) for merchant: {}, name: {}", merchantId, request.getName());
        
        RoutingAlgorithmV2Response response = new RoutingAlgorithmV2Response();
        response.setAlgorithmId("alg_v2_" + UUID.randomUUID().toString().substring(0, 8));
        response.setName(request.getName());
        response.setDescription(request.getDescription());
        response.setAlgorithmType(request.getAlgorithmType());
        response.setConfig(request.getConfig());
        response.setProfileId(request.getProfileId());
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        // In production, this would:
        // 1. Validate algorithm configuration
        // 2. Store routing algorithm in database
        // 3. Return created algorithm
        
        return Mono.just(Result.<RoutingAlgorithmV2Response, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<RoutingAlgorithmV2Response, PaymentError>> getRoutingAlgorithmV2(
            String merchantId,
            String algorithmId) {
        log.info("Getting routing algorithm (v2): {} for merchant: {}", algorithmId, merchantId);
        
        // In production, this would query routing algorithm from database
        RoutingAlgorithmV2Response response = new RoutingAlgorithmV2Response();
        response.setAlgorithmId(algorithmId);
        response.setName("Sample Algorithm");
        response.setDescription("Sample routing algorithm");
        response.setAlgorithmType("success_rate_based");
        response.setIsActive(Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<RoutingAlgorithmV2Response, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> createSuccessBasedRouting(
            String accountId,
            String profileId,
            DynamicRoutingRequest request) {
        log.info("Creating success-based routing for account: {}, profile: {}", accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId() != null ? request.getAlgorithmId() : 
            "success_based_" + UUID.randomUUID().toString().substring(0, 8));
        response.setProfileId(profileId);
        response.setRoutingType("success_based");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> updateSuccessBasedRoutingConfig(
            String accountId,
            String profileId,
            String algorithmId,
            DynamicRoutingRequest request) {
        log.info("Updating success-based routing config: {} for account: {}, profile: {}", 
            algorithmId, accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(algorithmId);
        response.setProfileId(profileId);
        response.setRoutingType("success_based");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> createEliminationRouting(
            String accountId,
            String profileId,
            DynamicRoutingRequest request) {
        log.info("Creating elimination routing for account: {}, profile: {}", accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId() != null ? request.getAlgorithmId() : 
            "elimination_" + UUID.randomUUID().toString().substring(0, 8));
        response.setProfileId(profileId);
        response.setRoutingType("elimination");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> updateEliminationRoutingConfig(
            String accountId,
            String profileId,
            String algorithmId,
            DynamicRoutingRequest request) {
        log.info("Updating elimination routing config: {} for account: {}, profile: {}", 
            algorithmId, accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(algorithmId);
        response.setProfileId(profileId);
        response.setRoutingType("elimination");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> toggleContractBasedRouting(
            String accountId,
            String profileId,
            DynamicRoutingRequest request) {
        log.info("Toggling contract-based routing for account: {}, profile: {}", accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(request.getAlgorithmId() != null ? request.getAlgorithmId() : 
            "contract_" + UUID.randomUUID().toString().substring(0, 8));
        response.setProfileId(profileId);
        response.setRoutingType("contract_based");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<DynamicRoutingResponse, PaymentError>> updateContractBasedRoutingConfig(
            String accountId,
            String profileId,
            String algorithmId,
            DynamicRoutingRequest request) {
        log.info("Updating contract-based routing config: {} for account: {}, profile: {}", 
            algorithmId, accountId, profileId);
        
        DynamicRoutingResponse response = new DynamicRoutingResponse();
        response.setAlgorithmId(algorithmId);
        response.setProfileId(profileId);
        response.setRoutingType("contract_based");
        response.setConfig(request.getConfig());
        response.setEnabled(request.getEnabled());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<DynamicRoutingResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<VolumeSplitResponse, PaymentError>> setVolumeSplit(
            String accountId,
            String profileId,
            VolumeSplitRequest request) {
        log.info("Setting volume split for account: {}, profile: {}", accountId, profileId);
        
        VolumeSplitResponse response = new VolumeSplitResponse();
        response.setProfileId(profileId);
        response.setSplits(request.getSplits());
        response.setConfig(request.getConfig());
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<VolumeSplitResponse, PaymentError>ok(response));
    }
    
    @Override
    public Mono<Result<VolumeSplitResponse, PaymentError>> getVolumeSplit(
            String accountId,
            String profileId) {
        log.info("Getting volume split for account: {}, profile: {}", accountId, profileId);
        
        // In production, this would query volume split from database
        VolumeSplitResponse response = new VolumeSplitResponse();
        response.setProfileId(profileId);
        Map<String, Double> splits = new HashMap<>();
        splits.put("connector1", 50.0);
        splits.put("connector2", 50.0);
        response.setSplits(splits);
        response.setUpdatedAt(Instant.now());
        
        return Mono.just(Result.<VolumeSplitResponse, PaymentError>ok(response));
    }
}

