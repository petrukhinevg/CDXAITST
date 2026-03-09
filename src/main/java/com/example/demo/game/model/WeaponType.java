package com.example.demo.game.model;

public enum WeaponType {
    STONE("Камень", 12, 0.20, true, 430.0, 1.15, 4.5, 0xFFCDB27A, 0.0, 0.0, 0.14),
    BOW("Лук", 18, 0.30, true, 650.0, 1.40, 3.2, 0xFFE7D2A2, 0.0, 0.0, 0.18),
    SWORD("Меч", 27, 0.40, false, 0.0, 0.0, 0.0, 0x00000000, 62.0, 100.0, 0.20);

    private final String displayName;
    private final int damage;
    private final double cooldown;
    private final boolean projectile;
    private final double projectileSpeed;
    private final double projectileLife;
    private final double projectileRadius;
    private final int projectileColorArgb;
    private final double meleeRange;
    private final double meleeArcDegrees;
    private final double attackAnimationTime;

    WeaponType(String displayName,
               int damage,
               double cooldown,
               boolean projectile,
               double projectileSpeed,
               double projectileLife,
               double projectileRadius,
               int projectileColorArgb,
               double meleeRange,
               double meleeArcDegrees,
               double attackAnimationTime) {
        this.displayName = displayName;
        this.damage = damage;
        this.cooldown = cooldown;
        this.projectile = projectile;
        this.projectileSpeed = projectileSpeed;
        this.projectileLife = projectileLife;
        this.projectileRadius = projectileRadius;
        this.projectileColorArgb = projectileColorArgb;
        this.meleeRange = meleeRange;
        this.meleeArcDegrees = meleeArcDegrees;
        this.attackAnimationTime = attackAnimationTime;
    }

    public String displayName() {
        return displayName;
    }

    public int damage() {
        return damage;
    }

    public double cooldown() {
        return cooldown;
    }

    public boolean projectile() {
        return projectile;
    }

    public double projectileSpeed() {
        return projectileSpeed;
    }

    public double projectileLife() {
        return projectileLife;
    }

    public double projectileRadius() {
        return projectileRadius;
    }

    public int projectileColorArgb() {
        return projectileColorArgb;
    }

    public double meleeRange() {
        return meleeRange;
    }

    public double meleeArcDegrees() {
        return meleeArcDegrees;
    }

    public double attackAnimationTime() {
        return attackAnimationTime;
    }
}
