package com.example.demo.game.model;

public enum AbilitySlot {
    PRIMARY("Q"),
    SECONDARY("E"),
    ULTIMATE("R");

    private final String keyLabel;

    AbilitySlot(String keyLabel) {
        this.keyLabel = keyLabel;
    }

    public String keyLabel() {
        return keyLabel;
    }
}
