package com.example.demo.game.model;

public class Player {
    public double x;
    public double y;
    public double radius = 12.0;

    public int hp;
    public int maxHp = 100;

    public int level = 1;
    public int xp = 0;
    public int xpToNextLevel = 40;

    public boolean moving;
    public double animPhase;
    public double aimAngle;
    public double attackTimer;
    public AnimationState state = AnimationState.IDLE;
}
