package com.example.demo.game.model;

public class Enemy {
    public double x;
    public double y;
    public double radius = 12.0;
    public int hp = 1;

    public boolean moving;
    public double animPhase;
    public double lookAngle;
    public double attackTimer;
    public double deathTimer;
    public AnimationState state = AnimationState.IDLE;
}
