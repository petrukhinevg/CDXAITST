package com.example.demo.game.model;

public enum Team {
    LIGHT,
    DARK,
    NEUTRAL;

    public Team opposite() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
