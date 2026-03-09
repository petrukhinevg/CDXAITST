package com.example.demo.game.model;

public interface CombatUnit extends CombatEntity {
    boolean isMoving();

    void setMoving(boolean moving);

    double getAnimPhase();

    void setAnimPhase(double animPhase);

    double getLookAngle();

    void setLookAngle(double lookAngle);

    double getAttackAnimationTimer();

    void setAttackAnimationTimer(double attackAnimationTimer);

    AnimationState getAnimationState();

    void setAnimationState(AnimationState animationState);
}
