package com.example.demo.game.model;

public class Bullet {
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double radius;
    public double life;
    public int damage;
    public int colorArgb;
    public int glowColorArgb;
    public CombatEntity target;
    public Team ownerTeam;
    public boolean structureProjectile;
    public boolean terrainCollision = true;
}
