package tn.sia.b2b.shared.domain;

import java.util.Objects;

public final class Sku {

    private final String value;

    private Sku(String value) {
        this.value = value;
    }

    public static Sku of(String value) {
        Objects.requireNonNull(value, "SKU value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SKU value must not be blank");
        }
        return new Sku(value.trim());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sku s)) return false;
        return value.equals(s.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
