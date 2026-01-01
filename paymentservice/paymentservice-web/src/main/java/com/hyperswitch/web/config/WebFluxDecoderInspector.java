package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Debug-only component that logs the set of registered HttpMessageReaders at application ready
 * time and prints details about any Jackson2JsonDecoder instances (including ObjectMapper modules).
 */
@Component
public class WebFluxDecoderInspector {
    private static final Logger log = LoggerFactory.getLogger(WebFluxDecoderInspector.class);
    private final ServerCodecConfigurer codecConfigurer;

    private final LoggingJackson2JsonDecoder loggingDecoder;
    private final ObjectMapper objectMapper;

    public WebFluxDecoderInspector(ServerCodecConfigurer codecConfigurer, LoggingJackson2JsonDecoder loggingDecoder, ObjectMapper objectMapper) {
        this.codecConfigurer = codecConfigurer;
        this.loggingDecoder = loggingDecoder;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== WebFluxDecoderInspector: enumerating HttpMessageReaders ===");
        try {
            List<HttpMessageReader<?>> readers = codecConfigurer.getReaders();
            log.info("Total HttpMessageReaders: {}", readers.size());
            int i = 0;
            for (HttpMessageReader<?> reader : readers) {
                log.info("Reader[{}] class={}", i++, reader.getClass().getName());

                // Log info about any Jackson-based decoders we find
                if (reader instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                    Object inner = dhr.getDecoder();
                    log.info(" -> Decoder field class: {}", inner == null ? "null" : inner.getClass().getName());
                    if (inner instanceof Jackson2JsonDecoder jacksonDecoder) {
                        try {
                            ObjectMapper mapper = jacksonDecoder.getObjectMapper();
                            log.info("  -> Jackson2JsonDecoder detected; ObjectMapper: {}; modules: {}",
                                    mapper, mapper.getRegisteredModuleIds());
                        } catch (Exception e) {
                            log.warn("  -> Could not access ObjectMapper on decoder {}: {}", reader.getClass().getName(), e.getMessage(), e);
                        }
                    } else if (inner != null && inner.getClass().getName().contains("JacksonJsonDecoder")) {
                        // For Spring versions that use JacksonJsonDecoder (class name contains JacksonJsonDecoder)
                        try {
                            log.info("  -> JacksonJsonDecoder detected (class: {}); will attempt to wrap for logging", inner.getClass().getName());
                        } catch (Exception e) {
                            log.warn("  -> Error inspecting JacksonJsonDecoder: {}", e.getMessage());
                        }
                    }
                }
            }

            // Replace any Jackson-based decoder instances (that are not our Logging wrappers)
            // AND wrap all DecoderHttpMessageReader instances with LoggingHttpMessageReader
            int replaced = 0;
            int wrapped = 0;
            for (int idx = 0; idx < readers.size(); idx++) {
                HttpMessageReader<?> r = readers.get(idx);
                if (r instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                    Object inner = dhr.getDecoder();
                    HttpMessageReader<?> newReader = null;
                    
                    if (inner instanceof org.springframework.http.codec.json.Jackson2JsonDecoder && !(inner instanceof LoggingJackson2JsonDecoder)) {
                        newReader = new org.springframework.http.codec.DecoderHttpMessageReader<>(loggingDecoder);
                        replaced++;
                        log.info("Replaced Jackson2JsonDecoder at index {} with LoggingJackson2JsonDecoder-backed reader", idx);
                    } else if (inner != null && inner.getClass().getName().contains("JacksonJsonDecoder") && !(inner instanceof LoggingDecoderWrapper)) {
                        // Wrap the existing JacksonJsonDecoder with our LoggingDecoderWrapper to intercept canDecode/decode
                        org.springframework.core.codec.Decoder<?> original = (org.springframework.core.codec.Decoder<?>) inner;
                        org.springframework.core.codec.Decoder<Object> wrapper = new LoggingDecoderWrapper(original);
                        newReader = new org.springframework.http.codec.DecoderHttpMessageReader<>(wrapper);
                        replaced++;
                        log.info("Replaced JacksonJsonDecoder at index {} with LoggingDecoderWrapper-backed reader", idx);
                    } else {
                        // Keep the existing reader but wrap it with LoggingHttpMessageReader for instrumentation
                        newReader = dhr;
                    }
                    
                    // Wrap ALL DecoderHttpMessageReader instances with LoggingHttpMessageReader for instrumentation
                    // But avoid double-wrapping if already wrapped
                    if (newReader != null && !(r instanceof LoggingHttpMessageReader) && !(newReader instanceof LoggingHttpMessageReader)) {
                        @SuppressWarnings("unchecked")
                        LoggingHttpMessageReader<?> loggingReader = new LoggingHttpMessageReader<>((HttpMessageReader<Object>) newReader);
                        readers.set(idx, loggingReader);
                        wrapped++;
                        log.info("Wrapped HttpMessageReader at index {} with LoggingHttpMessageReader", idx);
                    } else if (newReader == null) {
                        // If we didn't replace the decoder, still wrap the original reader if not already wrapped
                        if (!(r instanceof LoggingHttpMessageReader)) {
                            @SuppressWarnings("unchecked")
                            LoggingHttpMessageReader<?> loggingReader = new LoggingHttpMessageReader<>((HttpMessageReader<Object>) r);
                            readers.set(idx, loggingReader);
                            wrapped++;
                            log.info("Wrapped existing HttpMessageReader at index {} with LoggingHttpMessageReader", idx);
                        }
                    }
                }
            }

            if (replaced > 0) {
                log.info("Replaced {} Jackson-based reader(s) with our LoggingJackson2JsonDecoder", replaced);
            } else {
                log.info("No Jackson-based readers needed replacement (they may already be our LoggingJackson2JsonDecoder)");
            }
            
            if (wrapped > 0) {
                log.info("Wrapped {} HttpMessageReader(s) with LoggingHttpMessageReader for instrumentation", wrapped);
            }

            // Ensure our LoggingJackson2JsonDecoder-backed reader is at index 0 for highest precedence
            boolean atZero = false;
            if (!readers.isEmpty()) {
                HttpMessageReader<?> first = readers.get(0);
                // Check if first reader is LoggingHttpMessageReader wrapping our decoder
                if (first instanceof LoggingHttpMessageReader) {
                    // We can't easily check the delegate, so we'll check by looking for our decoder in the first few readers
                    // For now, assume it's at zero if we wrapped readers
                    atZero = wrapped > 0;
                } else if (first instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr0) {
                    Object inner0 = dhr0.getDecoder();
                    atZero = inner0 instanceof LoggingJackson2JsonDecoder;
                }
            }

            if (!atZero) {
                // Remove any existing reader that already wraps our LoggingJackson2JsonDecoder to avoid duplicates
                // But be careful not to remove LoggingHttpMessageReader wrappers
                readers.removeIf(r -> {
                    if (r instanceof LoggingHttpMessageReader) {
                        // Can't easily check delegate, so don't remove LoggingHttpMessageReader wrappers
                        return false;
                    }
                    if (r instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                        Object inner = dhr.getDecoder();
                        return inner instanceof LoggingJackson2JsonDecoder;
                    }
                    return false;
                });

                org.springframework.http.codec.DecoderHttpMessageReader<?> ourReader = new org.springframework.http.codec.DecoderHttpMessageReader<>(loggingDecoder);
                // Wrap with LoggingHttpMessageReader for instrumentation
                LoggingHttpMessageReader<?> wrappedReader = new LoggingHttpMessageReader<>(ourReader);
                readers.add(0, wrappedReader);
                log.info("Inserted our LoggingJackson2JsonDecoder-backed reader (wrapped with LoggingHttpMessageReader) at index 0");
            }

            // Also set default codecs to our decoder to be extra-safe
            try {
                codecConfigurer.defaultCodecs().jackson2JsonDecoder(loggingDecoder);
                log.info("Applied LoggingJackson2JsonDecoder to defaultCodecs().jackson2JsonDecoder(...)");
            } catch (Exception e) {
                log.warn("Failed to set default codecs jackson2JsonDecoder to our logging decoder: {}", e.getMessage());
            }

            // Log final readers for verification
            log.info("=== WebFluxDecoderInspector: final readers list (post-replacement) ===");
            i = 0;
            for (HttpMessageReader<?> reader : readers) {
                log.info("Final Reader[{}] class={}", i++, reader.getClass().getName());
                if (reader instanceof org.springframework.http.codec.DecoderHttpMessageReader<?> dhr) {
                    log.info("   -> Decoder class: {}", dhr.getDecoder() == null ? "null" : dhr.getDecoder().getClass().getName());
                }
            }

        } catch (Exception e) {
            log.error("Error enumerating or replacing HttpMessageReaders: {}", e.getMessage(), e);
        }
        log.info("=== WebFluxDecoderInspector: enumeration complete ===");
    }
}
