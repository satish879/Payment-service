package com.hyperswitch.common.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed Payment Method ID
 */
public final class PaymentMethodId {
    private final String value;

    private PaymentMethodId(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PaymentMethodId of(String value) {
        return new PaymentMethodId(value);
    }

    public static PaymentMethodId generate() {
        return new PaymentMethodId(UUID.randomUUID().toString());
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethodId that = (PaymentMethodId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

