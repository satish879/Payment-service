package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Debug-only component that inspects registered HttpMessageReaders after the
 * application is ready and logs which decoders/readers are present and which
 * ObjectMapper modules they use. Useful to verify that our LoggingJackson2JsonDecoder
 * is actually used by WebFlux.
 */
@Component
public class CodecInspector {
    private static final Logger log = LoggerFactory.getLogger(CodecInspector.class);

    private final ServerCodecConfigurer codecConfigurer;

    @Autowired
    public CodecInspector(ServerCodecConfigurer codecConfigurer) {
        this.codecConfigurer = codecConfigurer;
    }

    @jakarta.annotation.PostConstruct
    public void postConstructInspect() {
        // Run the same inspection as in ApplicationReady but earlier during initialization
        try {
            List<HttpMessageReader<?>> readers = codecConfigurer.getReaders();
            log.info("=== CodecInspector@PostConstruct - total HttpMessageReaders: {} ===", readers.size());
            for (int i = 0; i < readers.size(); i++) {
                HttpMessageReader<?> reader = readers.get(i);
                log.info("PC Reader[{}]: class={} toString={}", i, reader.getClass().getName(), reader.toString());

                if (reader instanceof DecoderHttpMessageReader<?> decoderReader) {
                    Object decoder = decoderReader.getDecoder();
                    log.info("PC -> Decoder field class: {}", decoder != null ? decoder.getClass().getName() : "null");
                    if (decoder instanceof Jackson2JsonDecoder jacksonDecoder) {
                        ObjectMapper om = jacksonDecoder.getObjectMapper();
                        log.info("PC -> Found Jackson2JsonDecoder (wrapped). ObjectMapper: {}", om);
                        log.info("PC -> ObjectMapper modules: {}", om.getRegisteredModuleIds());
                        log.info("PC -> Is LoggingJackson2JsonDecoder: {}", jacksonDecoder instanceof LoggingJackson2JsonDecoder);
                    }
                } else if (reader instanceof Jackson2JsonDecoder jacksonDecoder) {
                    ObjectMapper om = jacksonDecoder.getObjectMapper();
                    log.info("PC -> Found Jackson2JsonDecoder (direct). ObjectMapper: {}", om);
                    log.info("PC -> ObjectMapper modules: {}", om.getRegisteredModuleIds());
                    log.info("PC -> Is LoggingJackson2JsonDecoder: {}", jacksonDecoder instanceof LoggingJackson2JsonDecoder);
                }
            }
            log.info("=== CodecInspector@PostConstruct: Listing complete ===");
        } catch (Exception e) {
            log.error("Error while enumerating HttpMessageReaders at PostConstruct: {}", e.getMessage(), e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent evt) {
        try {
            List<HttpMessageReader<?>> readers = codecConfigurer.getReaders();
            log.info("=== CodecInspector: ApplicationReady - total HttpMessageReaders: {} ===", readers.size());
            for (int i = 0; i < readers.size(); i++) {
                HttpMessageReader<?> reader = readers.get(i);
                log.info("Reader[{}]: class={} toString={}", i, reader.getClass().getName(), reader.toString());

                // If the reader wraps a decoder, inspect it
                if (reader instanceof DecoderHttpMessageReader<?> decoderReader) {
                    Object decoder = decoderReader.getDecoder();
                    log.info(" -> Decoder field class: {}", decoder != null ? decoder.getClass().getName() : "null");

                    if (decoder instanceof Jackson2JsonDecoder jacksonDecoder) {
                        ObjectMapper om = jacksonDecoder.getObjectMapper();
                        log.info(" -> Found Jackson2JsonDecoder (wrapped). ObjectMapper: {}", om);
                        log.info(" -> ObjectMapper modules: {}", om.getRegisteredModuleIds());
                        log.info(" -> Is LoggingJackson2JsonDecoder: {}", jacksonDecoder instanceof LoggingJackson2JsonDecoder);
                    }
                } else if (reader instanceof Jackson2JsonDecoder jacksonDecoder) {
                    ObjectMapper om = jacksonDecoder.getObjectMapper();
                    log.info(" -> Found Jackson2JsonDecoder (direct). ObjectMapper: {}", om);
                    log.info(" -> ObjectMapper modules: {}", om.getRegisteredModuleIds());
                    log.info(" -> Is LoggingJackson2JsonDecoder: {}", jacksonDecoder instanceof LoggingJackson2JsonDecoder);
                }
            }
            log.info("=== CodecInspector: Listing complete ===");
        } catch (Exception e) {
            log.error("Error while enumerating HttpMessageReaders: {}", e.getMessage(), e);
        }
    }
}
