package com.hyperswitch.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.core.codec.Decoder;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.util.List;
import java.util.Map;

public class LoggingDecoderWrapper implements Decoder<Object> {
    private static final Logger log = LoggerFactory.getLogger(LoggingDecoderWrapper.class);

    private final Decoder<Object> delegate;

    @SuppressWarnings("unchecked")
    public LoggingDecoderWrapper(Decoder<?> delegate) {
        this.delegate = (Decoder<Object>) delegate;
        log.info("=== Created LoggingDecoderWrapper for delegate: {} ===", delegate.getClass().getName());
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        boolean result = delegate.canDecode(elementType, mimeType);
        log.info("=== LoggingDecoderWrapper.canDecode: elementType={} mimeType={} result={} delegate={} ===", elementType.toString(), mimeType, result, delegate.getClass().getName());
        return result;
    }

    @Override
    public Flux<Object> decode(org.reactivestreams.Publisher<DataBuffer> input, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        // Peek payload for logging, then delegate
        reactor.core.publisher.Mono<DataBuffer> joined = Flux.from(input).collectList().flatMap(list -> {
            if (list.isEmpty()) return Mono.empty();
            DataBuffer combined = list.get(0);
            if (list.size() > 1) {
                DefaultDataBufferFactory defaultFactory = new DefaultDataBufferFactory();
                DataBuffer merged = defaultFactory.allocateBuffer();
                for (DataBuffer db : list) {
                    merged.write(db);
                }
                combined = merged;
            }
            return Mono.just(combined);
        });

        return joined.flatMapMany(db -> {
            try {
                byte[] bytes = new byte[db.readableByteCount()];
                db.read(bytes);
                String payload = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                log.info("=== LoggingDecoderWrapper: delegate={} decoding elementType={} payload={} ===", delegate.getClass().getName(), elementType.toString(), payload);
                // recreate DataBuffer
                DataBuffer newBuffer = new DefaultDataBufferFactory().wrap(bytes);
                return delegate.decode(Mono.just(newBuffer), elementType, mimeType, hints);
            } catch (Exception e) {
                log.error("LoggingDecoderWrapper error: {}", e.getMessage(), e);
                return Flux.error(e);
            }
        });
    }

    @Override
    public Mono<Object> decodeToMono(org.reactivestreams.Publisher<DataBuffer> input, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(decode(input, elementType, mimeType, hints)).next();
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return delegate.getDecodableMimeTypes();
    }
}
