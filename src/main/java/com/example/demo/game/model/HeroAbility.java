package com.example.demo.game.model;

public class HeroAbility {
    private final AbilitySlot slot;
    private final String displayName;
    private final double cooldownSeconds;
    private final boolean ultimate;
    private final int manaCost;
    private double remainingCooldown;

    public HeroAbility(AbilitySlot slot, String displayName, double cooldownSeconds, boolean ultimate, int manaCost) {
        this.slot = slot;
        this.displayName = displayName;
        this.cooldownSeconds = cooldownSeconds;
        this.ultimate = ultimate;
        this.manaCost = manaCost;
    }

    public AbilitySlot slot() {
        return slot;
    }

    public String displayName() {
        return displayName;
    }

    public double cooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean ultimate() {
        return ultimate;
    }

    public int manaCost() {
        return manaCost;
    }

    public double remainingCooldown() {
        return remainingCooldown;
    }

    public boolean isReady() {
        return remainingCooldown <= 0.0;
    }

    public void tick(double dt) {
        remainingCooldown = Math.max(0.0, remainingCooldown - dt);
    }

    public void triggerPlaceholder() {
        remainingCooldown = cooldownSeconds;
    }
}
