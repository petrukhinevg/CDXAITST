package com.example.demo.game.world.element;

public final class MapElements {
    public static final GroundElement FOREST = new TerrainElement("forest", GroundKind.FOREST);
    public static final GroundElement GRASS = new TerrainElement("grass", GroundKind.GRASS);
    public static final GroundElement GRASS_ALT = new TerrainElement("grass_alt", GroundKind.GRASS_ALT);
    public static final GroundElement DIRT = new TerrainElement("dirt", GroundKind.DIRT);
    public static final GroundElement LANE = new TerrainElement("lane", GroundKind.LANE);
    public static final GroundElement HIGH_GROUND = new TerrainElement("high_ground", GroundKind.HIGH_GROUND);
    public static final GroundElement BASE = new TerrainElement("base", GroundKind.BASE);

    public static final WaterElement RIVER = RiverElement.INSTANCE;

    public static final PropElement BOULDER = new StaticPropElement("boulder", PropKind.BOULDER);
    public static final PropElement ROCK = new StaticPropElement("rock", PropKind.ROCK);
    public static final PropElement BUSH = new StaticPropElement("bush", PropKind.BUSH);
    public static final PropElement STUMP = new StaticPropElement("stump", PropKind.STUMP);
    public static final PropElement PEBBLES = new StaticPropElement("pebbles", PropKind.PEBBLES);

    private MapElements() {
    }
}
