package com.example.demo.game.model;

public class Creep implements CombatUnit {
    public Team team;
    public CreepRole role;
    public LaneType lane;

    public double x;
    public double y;
    public double radius;

    public int hp;
    public int maxHp;
    public int damage;
    public int defense;

    public double moveSpeed;
    public double attackRange;
    public double attackCooldown;
    public double attackTimer;

    public int waypointIndex;
    public boolean moving;
    public double animPhase;
    public double lookAngle;
    public double attackAnimationTimer;
    public AnimationState state = AnimationState.IDLE;

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public void setHp(int hp) {
        this.hp = hp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public int getDefense() {
        return defense;
    }

    @Override
    public double getAttackRange() {
        return attackRange;
    }

    @Override
    public double getAttackCooldown() {
        return attackCooldown;
    }

    @Override
    public double getAttackTimer() {
        return attackTimer;
    }

    @Override
    public void setAttackTimer(double attackTimer) {
        this.attackTimer = attackTimer;
    }

    @Override
    public boolean isMoving() {
        return moving;
    }

    @Override
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    @Override
    public double getAnimPhase() {
        return animPhase;
    }

    @Override
    public void setAnimPhase(double animPhase) {
        this.animPhase = animPhase;
    }

    @Override
    public double getLookAngle() {
        return lookAngle;
    }

    @Override
    public void setLookAngle(double lookAngle) {
        this.lookAngle = lookAngle;
    }

    @Override
    public double getAttackAnimationTimer() {
        return attackAnimationTimer;
    }

    @Override
    public void setAttackAnimationTimer(double attackAnimationTimer) {
        this.attackAnimationTimer = attackAnimationTimer;
    }

    @Override
    public AnimationState getAnimationState() {
        return state;
    }

    @Override
    public void setAnimationState(AnimationState animationState) {
        this.state = animationState;
    }
}
