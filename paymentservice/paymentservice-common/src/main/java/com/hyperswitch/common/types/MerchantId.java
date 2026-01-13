package com.hyperswitch.common.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Strongly typed Merchant ID
 */
public final class MerchantId {
    private final String value;

    private MerchantId(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MerchantId of(String value) {
        return new MerchantId(value);
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
        MerchantId that = (MerchantId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

