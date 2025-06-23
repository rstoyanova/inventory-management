package com.inventory.model;

public enum Unit {
    KG("KG"),
    LB("LB");

    private final String value;

    Unit(String value) {
        this.value = value;
    }

    public static Unit fromValue(String value) {
        for (Unit unit : Unit.values()) {
            if (unit.value.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown unit: " + value);
    }
}
