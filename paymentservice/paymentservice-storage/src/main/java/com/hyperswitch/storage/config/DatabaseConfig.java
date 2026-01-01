package com.hyperswitch.storage.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Database configuration for R2DBC
 * Note: @EnableR2dbcRepositories is now in PaymentServiceApplication to ensure proper scanning
 */
@Configuration
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Override
    @Bean
    @SuppressWarnings("null")
    public ConnectionFactory connectionFactory() {
        // Parse URL: r2dbc:postgresql://localhost:5432/hyperswitch_db
        String[] parts = url.replace("r2dbc:postgresql://", "").split("/");
        String[] hostPort = parts[0].split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
        String database = parts.length > 1 ? parts[1] : "hyperswitch_db";

        // Create the underlying connection factory
        PostgresqlConnectionFactory postgresqlFactory = new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .build()
        );

        // Wrap with connection pool for proper transaction handling
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(postgresqlFactory)
            .initialSize(5)
            .maxSize(20)
            .maxIdleTime(Duration.ofMinutes(30))
            .maxAcquireTime(Duration.ofSeconds(30))
            .maxCreateConnectionTime(Duration.ofSeconds(30))
            .build();

        log.info("Creating R2DBC ConnectionPool: initialSize=5, maxSize=20");
        return new ConnectionPool(poolConfig);
    }

    /**
     * Configure custom R2DBC conversions to handle Map<String, Object> to JSONB conversion.
     * This is required because R2DBC cannot automatically convert Map to JSONB.
     */
    @Override
    @Bean
    @SuppressWarnings("null")
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        
        // Use ObjectMapper if available, otherwise create a default one
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        
        // Converter: Map<String, Object> -> Json (for writing to database)
        converters.add(new MapToJsonConverter(mapper));
        
        // Converter: Json -> Map<String, Object> (for reading from database)
        converters.add(new JsonToMapConverter(mapper));
        
        log.info("Registered {} R2DBC custom converters for Map<String, Object> <-> JSONB conversion", converters.size());
        
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }

    /**
     * Writing converter: Map<String, Object> -> Json
     * Converts a Map to PostgreSQL Json type for storing in JSONB columns
     */
    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        private final ObjectMapper objectMapper;

        public MapToJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        @SuppressWarnings("null")
        public Json convert(Map<String, Object> source) {
            try {
                if (source == null || source.isEmpty()) {
                    // Return empty JSON object for null or empty maps
                    return Json.of("{}");
                }
                String json = objectMapper.writeValueAsString(source);
                return Json.of(json);
            } catch (Exception e) {
                log.error("Error converting Map to Json: {}", e.getMessage(), e);
                // Return empty JSON object on error
                return Json.of("{}");
            }
        }
    }

    /**
     * Reading converter: Json -> Map<String, Object>
     * Converts PostgreSQL Json type to Map for reading from JSONB columns
     */
    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        private final ObjectMapper objectMapper;
        private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};

        public JsonToMapConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        @SuppressWarnings("null")
        public Map<String, Object> convert(Json source) {
            try {
                if (source == null) {
                    return null;
                }
                String jsonString = source.asString();
                if (jsonString == null || jsonString.trim().isEmpty() || jsonString.equals("null")) {
                    return null;
                }
                return objectMapper.readValue(jsonString, MAP_TYPE_REF);
            } catch (Exception e) {
                log.error("Error converting Json to Map: {}", e.getMessage(), e);
                return null;
            }
        }
    }
}

