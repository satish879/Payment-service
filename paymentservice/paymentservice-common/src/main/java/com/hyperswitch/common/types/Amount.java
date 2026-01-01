package com.hyperswitch.common.types;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Monetary amount with currency
 */
@JsonDeserialize(using = AmountDeserializer.class)
public final class Amount {
    private final BigDecimal value;
    private final Currency currency;

    // Private constructor for internal use
    private Amount(BigDecimal value, Currency currency) {
        this.value = value;
        this.currency = currency;
    }

    // Factory method for programmatic creation (not used by Jackson)
    // Jackson uses AmountDeserializer instead (configured via @JsonDeserialize)
    public static Amount create(
            BigDecimal value,
            String currencyCode) {
        if (value == null) {
            throw new IllegalArgumentException("Amount value cannot be null");
        }
        if (currencyCode == null || currencyCode.isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        return new Amount(value, Currency.getInstance(currencyCode));
    }

    public static Amount of(BigDecimal value, Currency currency) {
        return new Amount(value, currency);
    }

    public static Amount of(BigDecimal value, String currencyCode) {
        return new Amount(value, Currency.getInstance(currencyCode));
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Amount amount = (Amount) o;
        return Objects.equals(value, amount.value) && Objects.equals(currency, amount.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    @Override
    public String toString() {
        return value + " " + currency.getCurrencyCode();
    }
}

