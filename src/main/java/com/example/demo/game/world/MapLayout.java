package com.example.demo.game.world;

import com.example.demo.game.config.GameConfig;
import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Team;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MapLayout {
    private static final double BASE_MAP_SIZE = 160.0;

    private MapLayout() {
    }

    public static final Point LIGHT_THRONE_TILE = p(15, 145);
    public static final Point DARK_THRONE_TILE = p(145, 15);

    private static final Point[] TOP_LANE = {
            p(15, 145), p(15, 113), p(16, 81),
            p(17, 49), p(17, 15), p(55, 15),
            p(99, 15), p(145, 15)
    };

    private static final Point[] MID_LANE = {
            p(15, 145), p(41, 119), p(67, 93),
            p(80, 80), p(93, 67), p(119, 41),
            p(145, 15)
    };

    private static final Point[] BOT_LANE = {
            p(15, 145), p(49, 145), p(84, 144),
            p(119, 144), p(145, 144), p(145, 109),
            p(144, 76), p(144, 41), p(145, 15)
    };

    private static final Point[] NEUTRAL_CAMPS = {
            p(42, 109), p(60, 86), p(86, 60), p(109, 42),
            p(45, 132), p(132, 45), p(84, 109), p(109, 84)
    };

    public static List<Point> laneTiles(LaneType lane) {
        return switch (lane) {
            case TOP -> new ArrayList<>(Arrays.asList(TOP_LANE));
            case MID -> new ArrayList<>(Arrays.asList(MID_LANE));
            case BOT -> new ArrayList<>(Arrays.asList(BOT_LANE));
        };
    }

    public static List<Point> laneTilesForTeam(LaneType lane, Team team) {
        List<Point> base = new ArrayList<>(laneTiles(lane));
        if (team == Team.DARK) {
            Collections.reverse(base);
        }
        return base;
    }

    public static Map<LaneType, List<Point>> towerTilesForTeam(Team team) {
        EnumMap<LaneType, List<Point>> map = new EnumMap<>(LaneType.class);

        if (team == Team.LIGHT) {
            map.put(LaneType.TOP, List.of(p(15, 119), p(17, 63)));
            map.put(LaneType.MID, List.of(p(35, 125), p(68, 92)));
            map.put(LaneType.BOT, List.of(p(52, 145), p(105, 144)));
        } else {
            map.put(LaneType.TOP, List.of(p(109, 15), p(55, 15)));
            map.put(LaneType.MID, List.of(p(125, 35), p(92, 68)));
            map.put(LaneType.BOT, List.of(p(144, 109), p(144, 55)));
        }

        return map;
    }

    public static List<Point> neutralCampTiles() {
        return new ArrayList<>(Arrays.asList(NEUTRAL_CAMPS));
    }

    private static Point p(int x, int y) {
        return new Point(scaleX(x), scaleY(y));
    }

    private static int scaleX(int value) {
        return clamp((int) Math.round(value * GameConfig.MAP_W / BASE_MAP_SIZE), 0, GameConfig.MAP_W - 1);
    }

    private static int scaleY(int value) {
        return clamp((int) Math.round(value * GameConfig.MAP_H / BASE_MAP_SIZE), 0, GameConfig.MAP_H - 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
