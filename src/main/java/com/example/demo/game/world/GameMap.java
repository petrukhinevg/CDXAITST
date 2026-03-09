package com.example.demo.game.world;

import com.example.demo.game.config.GameConfig;

import java.util.Random;

public class GameMap {
    private final int width;
    private final int height;
    private final int tileSize;

    private final boolean[][] blocked;
    private final boolean[][] lane;
    private final boolean[][] river;
    private final int[][] elevation;

    private final GroundType[][] ground;
    private final int[][] treeVariant;
    private final int[][] treeTint;
    private final PropType[][] props;

    public GameMap() {
        this(GameConfig.MAP_W, GameConfig.MAP_H, GameConfig.TILE);
    }

    public GameMap(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;

        blocked = new boolean[height][width];
        lane = new boolean[height][width];
        river = new boolean[height][width];
        elevation = new int[height][width];

        ground = new GroundType[height][width];
        treeVariant = new int[height][width];
        treeTint = new int[height][width];
        props = new PropType[height][width];
    }

    public void reset(Random random) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blocked[y][x] = false;
                lane[y][x] = false;
                river[y][x] = false;
                elevation[y][x] = 0;
                ground[y][x] = GroundType.GRASS;
                treeVariant[y][x] = random.nextInt(4);
                treeTint[y][x] = random.nextInt(5) - 2;
                props[y][x] = PropType.NONE;
            }
        }
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isBlocked(int x, int y) {
        return blocked[y][x];
    }

    public void setBlocked(int x, int y, boolean value) {
        blocked[y][x] = value;
    }

    public boolean isLane(int x, int y) {
        return lane[y][x];
    }

    public void setLane(int x, int y, boolean value) {
        lane[y][x] = value;
    }

    public boolean isRiver(int x, int y) {
        return river[y][x];
    }

    public void setRiver(int x, int y, boolean value) {
        river[y][x] = value;
    }

    public int getElevation(int x, int y) {
        return elevation[y][x];
    }

    public void setElevation(int x, int y, int value) {
        elevation[y][x] = value;
    }

    public GroundType getGround(int x, int y) {
        return ground[y][x];
    }

    public void setGround(int x, int y, GroundType type) {
        ground[y][x] = type;
    }

    public int getTreeVariant(int x, int y) {
        return treeVariant[y][x];
    }

    public void setTreeVariant(int x, int y, int variant) {
        treeVariant[y][x] = variant;
    }

    public int getTreeTint(int x, int y) {
        return treeTint[y][x];
    }

    public void setTreeTint(int x, int y, int tint) {
        treeTint[y][x] = tint;
    }

    public PropType getProp(int x, int y) {
        return props[y][x];
    }

    public void setProp(int x, int y, PropType type) {
        props[y][x] = type;
    }

    public boolean isBlockedPixel(double px, double py) {
        int tx = (int) (px / tileSize);
        int ty = (int) (py / tileSize);
        if (!inBounds(tx, ty)) {
            return true;
        }
        return blocked[ty][tx];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getPixelWidth() {
        return width * tileSize;
    }

    public int getPixelHeight() {
        return height * tileSize;
    }

    public double tileCenter(int tileIndex) {
        return tileIndex * tileSize + tileSize / 2.0;
    }
}
