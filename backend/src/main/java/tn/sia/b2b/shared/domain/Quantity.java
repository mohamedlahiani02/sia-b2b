package tn.sia.b2b.shared.domain;

import java.util.Objects;

public final class Quantity {

    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public int getValue() {
        return value;
    }

    public boolean isZero() {
        return value == 0;
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(value - other.value);
    }

    public boolean isGreaterThan(Quantity other) {
        return value > other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity q)) return false;
        return value == q.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
