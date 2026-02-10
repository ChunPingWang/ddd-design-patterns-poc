package com.automfg.manufacturing.domain.model;

public record Duration(int minutes) {
    public Duration {
        if (minutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative: " + minutes);
        }
    }

    public boolean exceeds(Duration other) {
        return this.minutes > other.minutes;
    }

    public Duration multipliedBy(double factor) {
        return new Duration((int) (minutes * factor));
    }
}
