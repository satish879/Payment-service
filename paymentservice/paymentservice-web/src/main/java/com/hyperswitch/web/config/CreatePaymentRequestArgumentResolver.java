package com.hyperswitch.web.config;

import com.hyperswitch.common.dto.CreatePaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.core.MethodParameter;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CreatePaymentRequestArgumentResolver implements HandlerMethodArgumentResolver {
    private static final Logger log = LoggerFactory.getLogger(CreatePaymentRequestArgumentResolver.class);

    private final ServerCodecConfigurer codecConfigurer;

    public CreatePaymentRequestArgumentResolver(ServerCodecConfigurer codecConfigurer) {
        this.codecConfigurer = codecConfigurer;
        log.info("=== CreatePaymentRequestArgumentResolver created ===");
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isTargetType = CreatePaymentRequest.class.isAssignableFrom(parameter.getParameterType());
        // Previously we required a @RequestBody annotation; that was too strict and meant
        // our resolver could be bypassed (e.g., when the parameter lacked the annotation).
        // Accept any parameter whose type is CreatePaymentRequest so we can inspect and
        // decode the body consistently.
        boolean supports = isTargetType;
        log.info("CreatePaymentRequestArgumentResolver.supportsParameter: paramType={} -> {}",
                parameter.getParameterType().getName(), supports);
        return supports;
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, org.springframework.web.reactive.BindingContext bindingContext, ServerWebExchange exchange) {
        log.info("=== CreatePaymentRequestArgumentResolver.resolveArgument: parameter={} ===", parameter.getParameterType().getName());

        ResolvableType elementType = ResolvableType.forType(parameter.getGenericParameterType());
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        if (mediaType == null) mediaType = MediaType.APPLICATION_JSON;

        log.info("Looking for HttpMessageReader for elementType={} mediaType={}", elementType, mediaType);

        List<HttpMessageReader<?>> readers = this.codecConfigurer.getReaders();
        for (HttpMessageReader<?> reader : readers) {
            try {
                if (reader.canRead(elementType, mediaType)) {
                    log.info("Selected reader: {} for elementType={} mediaType={}", reader.getClass().getName(), elementType, mediaType);
                    // Read using the selected reader
                    Mono<?> mono = reader.readMono(elementType, exchange.getRequest(), Collections.emptyMap());
                    return mono.doOnNext(obj -> log.info("Reader {} produced object: {}", reader.getClass().getName(), obj)).cast(Object.class);
                } else {
                    log.info("Reader {} cannot read elementType={} mediaType={}", reader.getClass().getName(), elementType, mediaType);
                }
            } catch (Exception e) {
                log.error("Error while probing reader {}: {}", reader.getClass().getName(), e.getMessage(), e);
            }
        }

        log.warn("No HttpMessageReader found for CreatePaymentRequest - falling back to empty CreatePaymentRequest instance");
        return Mono.just(new CreatePaymentRequest());
    }
}
