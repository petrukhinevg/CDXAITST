package com.example.demo.game.world;

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
    private MapLayout() {
    }

    public static final Point LIGHT_THRONE_TILE = new Point(15, 145);
    public static final Point DARK_THRONE_TILE = new Point(145, 15);

    private static final Point[] TOP_LANE = {
            new Point(15, 145), new Point(15, 113), new Point(16, 81),
            new Point(17, 49), new Point(17, 15), new Point(55, 15),
            new Point(99, 15), new Point(145, 15)
    };

    private static final Point[] MID_LANE = {
            new Point(15, 145), new Point(41, 119), new Point(67, 93),
            new Point(80, 80), new Point(93, 67), new Point(119, 41),
            new Point(145, 15)
    };

    private static final Point[] BOT_LANE = {
            new Point(15, 145), new Point(49, 145), new Point(84, 144),
            new Point(119, 144), new Point(145, 144), new Point(145, 109),
            new Point(144, 76), new Point(144, 41), new Point(145, 15)
    };

    private static final Point[] NEUTRAL_CAMPS = {
            new Point(42, 109), new Point(60, 86), new Point(86, 60), new Point(109, 42),
            new Point(45, 132), new Point(132, 45), new Point(84, 109), new Point(109, 84)
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
            map.put(LaneType.TOP, List.of(new Point(15, 119), new Point(17, 63)));
            map.put(LaneType.MID, List.of(new Point(35, 125), new Point(68, 92)));
            map.put(LaneType.BOT, List.of(new Point(52, 145), new Point(105, 144)));
        } else {
            map.put(LaneType.TOP, List.of(new Point(109, 15), new Point(55, 15)));
            map.put(LaneType.MID, List.of(new Point(125, 35), new Point(92, 68)));
            map.put(LaneType.BOT, List.of(new Point(144, 109), new Point(144, 55)));
        }

        return map;
    }

    public static List<Point> neutralCampTiles() {
        return new ArrayList<>(Arrays.asList(NEUTRAL_CAMPS));
    }
}
