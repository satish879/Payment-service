package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperswitch.common.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.BindingContext;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatePaymentRequestArgumentResolverTest {

    // dummy method to construct an accurate MethodParameter for the resolver
    private void dummy(CreatePaymentRequest body) { }

    @Test
    public void resolver_should_select_reader_and_decode_createPaymentRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ServerCodecConfigurer codecConfigurer = new org.springframework.http.codec.support.DefaultServerCodecConfigurer();

        // Register standard Jackson2JsonDecoder built from our ObjectMapper
        Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder(mapper);
        codecConfigurer.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);

        CreatePaymentRequestArgumentResolver resolver = new CreatePaymentRequestArgumentResolver(codecConfigurer);

        String json = "{\"merchantId\":\"merchant_123\",\"amount\":{\"value\":1000,\"currencyCode\":\"USD\"}}";

        org.springframework.core.io.buffer.DefaultDataBufferFactory factory = new org.springframework.core.io.buffer.DefaultDataBufferFactory();
        org.springframework.core.io.buffer.DataBuffer dataBuffer = factory.wrap(json.getBytes(StandardCharsets.UTF_8));
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reactor.core.publisher.Mono.just(dataBuffer));

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        try {
            java.lang.reflect.Method method = getClass().getDeclaredMethod("dummy", CreatePaymentRequest.class);
            org.springframework.core.MethodParameter methodParameter = new org.springframework.core.MethodParameter(method, 0);
            org.springframework.web.reactive.BindingContext bindingContext = new org.springframework.web.reactive.BindingContext();
            Mono<Object> mono = resolver.resolveArgument(methodParameter, bindingContext, exchange);

            Object result = mono.block();
            assertThat(result).isInstanceOf(CreatePaymentRequest.class);
            CreatePaymentRequest req = (CreatePaymentRequest) result;
            assertThat(req.getMerchantId()).isEqualTo("merchant_123");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

