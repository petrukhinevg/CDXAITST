package com.example.demo.game.model;

public class Bullet {
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double radius;
    public double life;
    public double maxLife;
    public int damage;
    public int colorArgb;
    public int glowColorArgb;
    public CombatEntity target;
    public Team ownerTeam;
    public ProjectileType projectileType = ProjectileType.STONE;
    public boolean structureProjectile;
    public boolean creepProjectile;
    public boolean terrainCollision = true;
}
