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

    public static final Point LIGHT_THRONE_TILE = new Point(20, 200);
    public static final Point DARK_THRONE_TILE = new Point(200, 20);

    private static final Point[] TOP_LANE = {
            new Point(20, 200), new Point(20, 156), new Point(22, 112),
            new Point(24, 68), new Point(24, 20), new Point(76, 20),
            new Point(136, 20), new Point(200, 20)
    };

    private static final Point[] MID_LANE = {
            new Point(20, 200), new Point(56, 164), new Point(92, 128),
            new Point(110, 110), new Point(128, 92), new Point(164, 56),
            new Point(200, 20)
    };

    private static final Point[] BOT_LANE = {
            new Point(20, 200), new Point(68, 200), new Point(116, 198),
            new Point(164, 198), new Point(200, 198), new Point(200, 150),
            new Point(198, 104), new Point(198, 56), new Point(200, 20)
    };

    private static final Point[] NEUTRAL_CAMPS = {
            new Point(58, 150), new Point(82, 118), new Point(118, 82), new Point(150, 58),
            new Point(62, 182), new Point(182, 62), new Point(116, 150), new Point(150, 116)
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
            map.put(LaneType.TOP, List.of(new Point(20, 164), new Point(24, 86)));
            map.put(LaneType.MID, List.of(new Point(48, 172), new Point(94, 126)));
            map.put(LaneType.BOT, List.of(new Point(72, 200), new Point(144, 198)));
        } else {
            map.put(LaneType.TOP, List.of(new Point(150, 20), new Point(76, 20)));
            map.put(LaneType.MID, List.of(new Point(172, 48), new Point(126, 94)));
            map.put(LaneType.BOT, List.of(new Point(198, 150), new Point(198, 76)));
        }

        return map;
    }

    public static List<Point> neutralCampTiles() {
        return new ArrayList<>(Arrays.asList(NEUTRAL_CAMPS));
    }
}
