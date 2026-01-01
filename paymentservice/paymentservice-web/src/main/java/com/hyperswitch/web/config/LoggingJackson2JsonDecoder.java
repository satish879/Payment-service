package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;

/**
 * Wrapper around Jackson2JsonDecoder that adds logging to debug deserialization issues.
 * This helps us understand when and how the decoder is being used.
 * 
 * Note: Method signatures in Jackson2JsonDecoder may vary between Spring versions,
 * so we use a simple wrapper approach. The constructor logging confirms the decoder
 * is created with the correct ObjectMapper. Additional verification can be done
 * by checking if AmountDeserializer logs appear in the application logs.
 */
@SuppressWarnings("deprecation")
public class LoggingJackson2JsonDecoder extends Jackson2JsonDecoder {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingJackson2JsonDecoder.class);
    
    public LoggingJackson2JsonDecoder(ObjectMapper mapper) {
        super(mapper);
        log.info("=== LoggingJackson2JsonDecoder created ===");
        log.info("ObjectMapper instance: {}", mapper);
        log.info("ObjectMapper modules: {}", mapper.getRegisteredModuleIds());
        log.info("AmountDeserializer registered: {}", mapper.getRegisteredModuleIds().contains("AmountModule"));
        log.info("This decoder will be used by WebFlux for JSON deserialization");
    }

    @Override
    public boolean canDecode(org.springframework.core.ResolvableType elementType, org.springframework.util.MimeType mimeType) {
        boolean result = super.canDecode(elementType, mimeType);
        try {
            log.info("=== LoggingJackson2JsonDecoder.canDecode: elementType={} mimeType={} result={} ===", elementType.toString(), mimeType, result);
        } catch (Exception e) {
            log.warn("Error logging canDecode details: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public reactor.core.publisher.Flux<Object> decode(org.reactivestreams.Publisher<org.springframework.core.io.buffer.DataBuffer> input,
                                                   org.springframework.core.ResolvableType elementType,
                                                   org.springframework.util.MimeType mimeType,
                                                   java.util.Map<String, Object> hints) {
        // Join the data buffers to log the raw payload and then delegate to the superclass for actual decoding
        reactor.core.publisher.Mono<org.springframework.core.io.buffer.DataBuffer> joined =
            reactor.core.publisher.Flux.from(input)
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) return reactor.core.publisher.Mono.empty();
                    org.springframework.core.io.buffer.DataBuffer combined = list.get(0);
                    if (list.size() > 1) {
                        org.springframework.core.io.buffer.DataBufferFactory factory = combined.factory();
                        org.springframework.core.io.buffer.DefaultDataBufferFactory defaultFactory = new org.springframework.core.io.buffer.DefaultDataBufferFactory();
                        org.springframework.core.io.buffer.DataBuffer merged = defaultFactory.allocateBuffer();
                        for (org.springframework.core.io.buffer.DataBuffer db : list) {
                            merged.write(db);
                        }
                        combined = merged;
                    }
                    return reactor.core.publisher.Mono.just(combined);
                });

        return joined.flatMapMany(dataBuffer -> {
            try {
                int readable = dataBuffer.readableByteCount();
                byte[] bytes = new byte[readable];
                dataBuffer.read(bytes);
                String payload = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

                // Log overall decoding payload
                log.info("=== LoggingJackson2JsonDecoder: decoding elementType={} payload={} ===", elementType.toString(), payload);

                // If it's CreatePaymentRequest or Amount, log more specifically
                if (elementType.toString().contains("CreatePaymentRequest") || elementType.toString().contains("Amount")) {
                    log.info("Detailed payload for {}: {}", elementType.toString(), payload);
                }

                // Recreate DataBuffer publisher for superclass decode
                org.springframework.core.io.buffer.DataBufferFactory factory = new org.springframework.core.io.buffer.DefaultDataBufferFactory();
                org.springframework.core.io.buffer.DataBuffer newBuffer = factory.wrap(bytes);
                return reactor.core.publisher.Flux.from(super.decode(reactor.core.publisher.Flux.just(newBuffer), elementType, mimeType, hints));
            } catch (Exception e) {
                log.error("Error logging payload in LoggingJackson2JsonDecoder: {}", e.getMessage(), e);
                return reactor.core.publisher.Flux.error(e);
            }
        });
    }
}
