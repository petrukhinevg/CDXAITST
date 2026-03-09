package com.example.demo.game.model;

public class Structure implements CombatEntity {
    public Team team;
    public StructureType type;
    public LaneType lane;

    public double x;
    public double y;
    public double radius;

    public int hp;
    public int maxHp;

    public int damage;
    public int defense;
    public double attackRange;
    public double attackCooldown;
    public double attackTimer;
    public CombatEntity attackTarget;

    public int laneOrder;

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
}
