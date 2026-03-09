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

    public static final Point LIGHT_THRONE_TILE = new Point(22, 120);
    public static final Point DARK_THRONE_TILE = new Point(198, 18);

    private static final Point[] TOP_LANE = {
            new Point(22, 120), new Point(22, 86), new Point(36, 46),
            new Point(76, 22), new Point(136, 16), new Point(198, 18)
    };

    private static final Point[] MID_LANE = {
            new Point(22, 120), new Point(62, 96), new Point(108, 70),
            new Point(154, 44), new Point(198, 18)
    };

    private static final Point[] BOT_LANE = {
            new Point(22, 120), new Point(58, 126), new Point(106, 124),
            new Point(150, 102), new Point(184, 62), new Point(198, 18)
    };

    private static final Point[] NEUTRAL_CAMPS = {
            new Point(70, 86), new Point(88, 54), new Point(104, 104),
            new Point(148, 46), new Point(130, 80), new Point(116, 32)
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
            map.put(LaneType.TOP, List.of(new Point(22, 96), new Point(42, 38)));
            map.put(LaneType.MID, List.of(new Point(40, 110), new Point(86, 84)));
            map.put(LaneType.BOT, List.of(new Point(44, 124), new Point(98, 124)));
        } else {
            map.put(LaneType.TOP, List.of(new Point(186, 18), new Point(136, 16)));
            map.put(LaneType.MID, List.of(new Point(184, 26), new Point(154, 44)));
            map.put(LaneType.BOT, List.of(new Point(194, 30), new Point(168, 80)));
        }

        return map;
    }

    public static List<Point> neutralCampTiles() {
        return new ArrayList<>(Arrays.asList(NEUTRAL_CAMPS));
    }
}
