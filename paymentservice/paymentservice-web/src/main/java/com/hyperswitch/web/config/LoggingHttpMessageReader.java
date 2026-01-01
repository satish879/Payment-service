package com.hyperswitch.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around HttpMessageReader that logs incoming DataBuffer(s), ResolvableType, hints,
 * and MediaType when readMono is called. This helps debug decoder selection and deserialization issues.
 */
public class LoggingHttpMessageReader<T> implements HttpMessageReader<T> {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingHttpMessageReader.class);
    
    private final HttpMessageReader<T> delegate;
    private final String delegateName;
    
    public LoggingHttpMessageReader(HttpMessageReader<T> delegate) {
        this.delegate = delegate;
        this.delegateName = delegate.getClass().getSimpleName();
        log.info("=== Created LoggingHttpMessageReader for delegate: {} ===", delegate.getClass().getName());
    }
    
    @Override
    public List<MediaType> getReadableMediaTypes() {
        return delegate.getReadableMediaTypes();
    }
    
    @Override
    public boolean canRead(ResolvableType elementType, MediaType mediaType) {
        boolean result = delegate.canRead(elementType, mediaType);
        log.info("=== LoggingHttpMessageReader.canRead: delegate={} elementType={} mediaType={} result={} ===",
            delegateName, elementType, mediaType, result);
        return result;
    }
    
    @Override
    public Mono<T> readMono(ResolvableType elementType, org.springframework.http.ReactiveHttpInputMessage message, Map<String, Object> hints) {
        log.info("=== LoggingHttpMessageReader.readMono CALLED ===");
        log.info("Delegate: {}", delegateName);
        log.info("ElementType: {}", elementType);
        log.info("ElementType.getRawClass(): {}", elementType.getRawClass());
        if (elementType.getGeneric(0) != null) {
            log.info("ElementType.getGeneric(0): {}", elementType.getGeneric(0));
        }
        log.info("Hints: {}", hints);
        
        // Capture headers
        HttpHeaders headers = message.getHeaders();
        log.info("Content-Type: {}", headers.getContentType());
        log.info("Content-Length: {}", headers.getContentLength());
        
        // Capture and log the DataBuffer payload, then replay it for the delegate
        return DataBufferUtils.join(message.getBody())
            .flatMap(dataBuffer -> {
                try {
                    int readableBytes = dataBuffer.readableByteCount();
                    log.info("DataBuffer readable bytes: {}", readableBytes);
                    
                    if (readableBytes > 0) {
                        // Read the buffer content for logging
                        byte[] bytes = new byte[readableBytes];
                        dataBuffer.read(bytes);
                        
                        String payload = new String(bytes, StandardCharsets.UTF_8);
                        log.info("=== DataBuffer payload (first 1000 chars) ===");
                        log.info("{}", payload.length() > 1000 ? payload.substring(0, 1000) + "..." : payload);
                        log.info("=== End DataBuffer payload ===");
                        
                        // Release the original buffer since we've consumed it
                        DataBufferUtils.release(dataBuffer);
                        
                        // Create a new buffer from the cached bytes for the delegate
                        final byte[] cachedBytes = bytes;
                        org.springframework.http.ReactiveHttpInputMessage replayMessage = new org.springframework.http.ReactiveHttpInputMessage() {
                            @Override
                            public HttpHeaders getHeaders() {
                                return message.getHeaders();
                            }
                            
                            @Override
                            public Flux<DataBuffer> getBody() {
                                // Create a new buffer each time getBody() is called
                                // This is important because WebFlux may call it multiple times
                                DataBuffer newBuffer = org.springframework.core.io.buffer.DefaultDataBufferFactory.sharedInstance.wrap(cachedBytes);
                                log.debug("Replaying cached DataBuffer for delegate - size: {}", cachedBytes.length);
                                return Flux.just(newBuffer);
                            }
                        };
                        
                        // Call delegate with replayed message
                        Mono<T> result = delegate.readMono(elementType, replayMessage, hints);
                        
                        return result
                            .doOnNext(value -> {
                                log.info("=== LoggingHttpMessageReader.readMono SUCCESS ===");
                                log.info("Decoded value type: {}", value != null ? value.getClass().getName() : "null");
                                if (value != null) {
                                    log.info("Decoded value toString: {}", value.toString());
                                }
                            })
                            .doOnError(error -> {
                                log.error("=== LoggingHttpMessageReader.readMono ERROR ===");
                                log.error("Error type: {}", error.getClass().getName());
                                log.error("Error message: {}", error.getMessage());
                                log.error("Stack trace:", error);
                            });
                    } else {
                        log.warn("DataBuffer has 0 readable bytes - calling delegate with original message");
                        DataBufferUtils.release(dataBuffer);
                        return delegate.readMono(elementType, message, hints);
                    }
                } catch (Exception e) {
                    log.error("Error reading DataBuffer for logging: {}", e.getMessage(), e);
                    DataBufferUtils.release(dataBuffer);
                    return delegate.readMono(elementType, message, hints);
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("=== LoggingHttpMessageReader.readMono: message body was empty ===");
                return delegate.readMono(elementType, message, hints);
            }));
    }
    
    @Override
    public Flux<T> read(ResolvableType elementType, org.springframework.http.ReactiveHttpInputMessage message, Map<String, Object> hints) {
        log.info("=== LoggingHttpMessageReader.read CALLED (Flux) ===");
        log.info("Delegate: {}", delegateName);
        log.info("ElementType: {}", elementType);
        log.info("Hints: {}", hints);
        
        return delegate.read(elementType, message, hints)
            .doOnNext(value -> {
                log.info("=== LoggingHttpMessageReader.read emitted value: {} ===", value);
            })
            .doOnError(error -> {
                log.error("=== LoggingHttpMessageReader.read ERROR: {} ===", error.getMessage(), error);
            });
    }
}

