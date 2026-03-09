package com.example.demo.game.world.blueprint;

import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Team;
import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.element.GroundElement;
import com.example.demo.game.world.element.PropElement;
import com.example.demo.game.world.element.WaterElement;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
public final class MapBlueprint {
    static final int NO_TREE_VARIANT = -1;
    static final int NO_TREE_TINT = Integer.MIN_VALUE;

    private final int width;
    private final int height;
    private final GroundElement[][] ground;
    private final WaterElement[][] water;
    private final PropElement[][] props;
    private final int[][] treeVariant;
    private final int[][] treeTint;
    private final boolean[][] blocked;
    private final boolean[][] lane;
    private final int[][] laneMask;
    private final Point playerStart;
    private final Point lightThrone;
    private final Point darkThrone;
    private final EnumMap<LaneType, List<Point>> lanePaths;
    private final EnumMap<Team, EnumMap<LaneType, List<Point>>> towerTiles;

    public MapBlueprint(int width,
                        int height,
                        GroundElement[][] ground,
                        WaterElement[][] water,
                        PropElement[][] props,
                        int[][] treeVariant,
                        int[][] treeTint,
                        boolean[][] blocked,
                        boolean[][] lane,
                        int[][] laneMask,
                        Point playerStart,
                        Point lightThrone,
                        Point darkThrone,
                        EnumMap<LaneType, List<Point>> lanePaths,
                        EnumMap<Team, EnumMap<LaneType, List<Point>>> towerTiles) {
        this.width = width;
        this.height = height;
        this.ground = ground;
        this.water = water;
        this.props = props;
        this.treeVariant = treeVariant;
        this.treeTint = treeTint;
        this.blocked = blocked;
        this.lane = lane;
        this.laneMask = laneMask;
        this.playerStart = playerStart;
        this.lightThrone = lightThrone;
        this.darkThrone = darkThrone;
        this.lanePaths = lanePaths;
        this.towerTiles = towerTiles;
    }

    public void applyTo(GameMap map) {
        if (map.getWidth() != width || map.getHeight() != height) {
            throw new IllegalArgumentException("Blueprint size " + width + "x" + height
                    + " does not match map size " + map.getWidth() + "x" + map.getHeight());
        }

        map.reset();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map.setGround(x, y, ground[y][x]);
                map.setWater(x, y, water[y][x]);
                map.setProp(x, y, props[y][x]);
                if (treeVariant[y][x] != NO_TREE_VARIANT) {
                    map.setTreeVariant(x, y, treeVariant[y][x]);
                }
                if (treeTint[y][x] != NO_TREE_TINT) {
                    map.setTreeTint(x, y, treeTint[y][x]);
                }
                map.setBlocked(x, y, blocked[y][x]);
                map.setLane(x, y, lane[y][x]);
                map.setLaneMask(x, y, laneMask[y][x]);
            }
        }
    }

    public Point playerStart() {
        return new Point(playerStart);
    }

    public Point throneTile(Team team) {
        return team == Team.LIGHT ? new Point(lightThrone) : new Point(darkThrone);
    }

    public List<Point> laneTilesForTeam(LaneType laneType, Team team) {
        List<Point> base = new ArrayList<>(lanePaths.get(laneType));
        if (team == Team.DARK) {
            Collections.reverse(base);
        }
        return base;
    }

    public List<Point> towerTiles(Team team, LaneType laneType) {
        return new ArrayList<>(towerTiles.get(team).get(laneType));
    }
}
