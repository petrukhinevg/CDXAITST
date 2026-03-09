package com.example.demo.game.world.element;

public final class RiverElement implements WaterElement {
    public static final RiverElement INSTANCE = new RiverElement();

    private RiverElement() {
    }

    @Override
    public String id() {
        return "river";
    }
}
