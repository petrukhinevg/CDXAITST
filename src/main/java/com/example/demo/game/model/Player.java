package com.example.demo.game.model;

public class Player implements CombatUnit {
    public Team team = Team.LIGHT;
    public double x;
    public double y;
    public double spawnX;
    public double spawnY;
    public double radius = 12.0;

    public int hp;
    public int maxHp = 100;
    public int defense = 2;

    public int level = 1;
    public int xp = 0;
    public int xpToNextLevel = 40;

    public boolean moving;
    public double animPhase;
    public double aimAngle;
    public double hitCooldown;
    public double respawnTimer;
    public double attackTimer;
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
        return 0;
    }

    @Override
    public int getDefense() {
        return defense;
    }

    @Override
    public double getAttackRange() {
        return 0.0;
    }

    @Override
    public double getAttackCooldown() {
        return 0.0;
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
        return aimAngle;
    }

    @Override
    public void setLookAngle(double lookAngle) {
        this.aimAngle = lookAngle;
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
