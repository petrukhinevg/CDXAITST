package com.example.demo.game.model;

public interface CombatEntity {
    Team getTeam();

    double getX();

    double getY();

    void setX(double x);

    void setY(double y);

    double getRadius();

    int getHp();

    void setHp(int hp);

    int getMaxHp();

    int getDamage();

    int getDefense();

    double getAttackRange();

    double getAttackCooldown();

    double getAttackTimer();

    void setAttackTimer(double attackTimer);

    default boolean isAlive() {
        return getHp() > 0;
    }

    default int mitigatedDamage(int rawDamage) {
        return Math.max(1, rawDamage - Math.max(0, getDefense()));
    }

    default void applyDamage(int rawDamage) {
        setHp(getHp() - mitigatedDamage(rawDamage));
    }
}
