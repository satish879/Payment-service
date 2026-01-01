package com.hyperswitch.web.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.hyperswitch.common.errors.PaymentError;
import com.hyperswitch.web.controller.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Global exception handler
 */
@RestControllerAdvice
public class ErrorHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(PaymentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handlePaymentException(PaymentException ex, org.springframework.web.server.ServerWebExchange exchange) {
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed; cannot write PaymentException response: {}", ex.getMessage());
            return Mono.empty();
        }
        PaymentError error = ex.getError();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", Map.of(
                    "code", error.getCode(),
                    "message", error.getMessage()
                )
            )));
    }

    @ExceptionHandler(DecodingException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDecodingException(Exception ex, org.springframework.web.server.ServerWebExchange exchange) {
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed; cannot write decoding error response: {}", ex.getMessage());
            return Mono.empty();
        }
        log.error("JSON deserialization error: {}", ex.getMessage(), ex);
        
        String message = "Invalid request body";
        if (ex.getCause() != null) {
            Throwable cause = ex.getCause();
            if (cause instanceof InvalidFormatException) {
                InvalidFormatException ife = (InvalidFormatException) cause;
                message = "Invalid format for field: " + ife.getPathReference();
            } else if (cause instanceof MismatchedInputException) {
                MismatchedInputException mie = (MismatchedInputException) cause;
                message = "Mismatched input for field: " + mie.getPathReference();
            } else {
                message = "Deserialization error: " + cause.getMessage();
            }
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", Map.of(
                    "code", "INVALID_REQUEST",
                    "message", message,
                    "details", ex.getMessage()
                )
            )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, String>>> handleGenericException(Exception ex, org.springframework.web.server.ServerWebExchange exchange) {
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed; cannot write generic error response: {}", ex.getMessage());
            return Mono.empty();
        }
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal server error: " + ex.getMessage())));
    }
}

