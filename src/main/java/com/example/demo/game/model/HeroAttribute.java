package com.example.demo.game.model;

public enum HeroAttribute {
    STRENGTH("СИЛ"),
    AGILITY("ЛОВ"),
    INTELLIGENCE("ИНТ");

    private final String shortLabel;

    HeroAttribute(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String shortLabel() {
        return shortLabel;
    }
}
