package com.example.demo.game.model;

public class Creep {
    public Team team;
    public CreepRole role;
    public LaneType lane;

    public double x;
    public double y;
    public double radius;

    public int hp;
    public int maxHp;
    public int damage;

    public double moveSpeed;
    public double attackRange;
    public double attackCooldown;
    public double attackTimer;

    public int waypointIndex;
    public double animPhase;
}
