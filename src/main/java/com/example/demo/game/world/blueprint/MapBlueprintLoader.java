package com.example.demo.game.world.blueprint;

import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Team;
import com.example.demo.game.world.element.GroundElement;
import com.example.demo.game.world.element.MapElements;
import com.example.demo.game.world.element.PropElement;
import com.example.demo.game.world.element.WaterElement;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapBlueprintLoader {
    private static final String GRID_MARKER = "grid";

    public MapBlueprint load(String resourcePath) {
        try (InputStream in = MapBlueprintLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Map resource not found: " + resourcePath);
            }

            List<String> lines = readLines(in);
            return parse(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load map blueprint: " + resourcePath, e);
        }
    }

    private MapBlueprint parse(List<String> lines) {
        int width = -1;
        int height = -1;
        Point playerStart = null;
        Point lightThrone = null;
        Point darkThrone = null;
        EnumMap<LaneType, List<Point>> lanePaths = initLaneMap();
        EnumMap<Team, EnumMap<LaneType, List<Point>>> towerTiles = initTowerMap();
        List<String> gridLines = new ArrayList<>();
        boolean inGrid = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (inGrid) {
                gridLines.add(line);
                continue;
            }
            if (line.equalsIgnoreCase(GRID_MARKER)) {
                inGrid = true;
                continue;
            }

            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "size" -> {
                    width = Integer.parseInt(parts[1]);
                    height = Integer.parseInt(parts[2]);
                }
                case "player_start" -> playerStart = new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "light_throne" -> lightThrone = new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "dark_throne" -> darkThrone = new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "lane_path" -> lanePaths.put(parseLane(parts[1]), parsePointList(parts, 2));
                case "light_top_towers" -> towerTiles.get(Team.LIGHT).put(LaneType.TOP, parsePointList(parts, 1));
                case "light_mid_towers" -> towerTiles.get(Team.LIGHT).put(LaneType.MID, parsePointList(parts, 1));
                case "light_bot_towers" -> towerTiles.get(Team.LIGHT).put(LaneType.BOT, parsePointList(parts, 1));
                case "dark_top_towers" -> towerTiles.get(Team.DARK).put(LaneType.TOP, parsePointList(parts, 1));
                case "dark_mid_towers" -> towerTiles.get(Team.DARK).put(LaneType.MID, parsePointList(parts, 1));
                case "dark_bot_towers" -> towerTiles.get(Team.DARK).put(LaneType.BOT, parsePointList(parts, 1));
                default -> throw new IllegalArgumentException("Unknown map directive: " + parts[0]);
            }
        }

        if (width <= 0 || height <= 0 || playerStart == null || lightThrone == null || darkThrone == null) {
            throw new IllegalArgumentException("Map blueprint header is incomplete");
        }
        if (gridLines.size() != height) {
            throw new IllegalArgumentException("Expected " + height + " grid rows, got " + gridLines.size());
        }

        GroundElement[][] ground = new GroundElement[height][width];
        WaterElement[][] water = new WaterElement[height][width];
        PropElement[][] props = new PropElement[height][width];
        boolean[][] blocked = new boolean[height][width];
        boolean[][] lane = new boolean[height][width];
        int[][] laneMask = new int[height][width];

        for (int y = 0; y < height; y++) {
            String[] cells = gridLines.get(y).split("\\s+");
            if (cells.length != width) {
                throw new IllegalArgumentException("Expected " + width + " cells in row " + y + ", got " + cells.length);
            }
            for (int x = 0; x < width; x++) {
                TileLayers tile = parseTile(cells[x]);
                ground[y][x] = tile.ground();
                water[y][x] = tile.water();
                props[y][x] = tile.prop();
                blocked[y][x] = tile.blocked();
                lane[y][x] = tile.lane();
                laneMask[y][x] = tile.laneMask();
            }
        }

        return new MapBlueprint(width, height, ground, water, props, blocked, lane, laneMask,
                playerStart, lightThrone, darkThrone, lanePaths, towerTiles);
    }

    private TileLayers parseTile(String token) {
        String[] layers = token.split("\\+");
        GroundElement ground = null;
        WaterElement water = null;
        PropElement prop = null;
        boolean blocked = false;
        int laneMask = 0;

        for (String layer : layers) {
            switch (layer) {
                case "gr1" -> ground = MapElements.GRASS;
                case "gr2" -> ground = MapElements.GRASS_ALT;
                case "dt1" -> ground = MapElements.DIRT;
                case "hg1" -> ground = MapElements.HIGH_GROUND;
                case "bs1" -> ground = MapElements.BASE;
                case "wd1" -> {
                    ground = MapElements.FOREST;
                    blocked = true;
                }
                case "rv1" -> water = MapElements.RIVER;
                case "l1" -> laneMask |= laneBit(LaneType.TOP);
                case "l2" -> laneMask |= laneBit(LaneType.MID);
                case "l3" -> laneMask |= laneBit(LaneType.BOT);
                case "rk1" -> prop = MapElements.ROCK;
                case "bh1" -> prop = MapElements.BUSH;
                case "st1" -> prop = MapElements.STUMP;
                case "pb1" -> prop = MapElements.PEBBLES;
                default -> throw new IllegalArgumentException("Unknown tile code: " + layer);
            }
        }

        if (ground == null) {
            throw new IllegalArgumentException("Tile must define a ground layer: " + token);
        }

        return new TileLayers(ground, water, prop, blocked, laneMask != 0, laneMask);
    }

    private EnumMap<LaneType, List<Point>> initLaneMap() {
        EnumMap<LaneType, List<Point>> map = new EnumMap<>(LaneType.class);
        for (LaneType laneType : LaneType.values()) {
            map.put(laneType, new ArrayList<>());
        }
        return map;
    }

    private EnumMap<Team, EnumMap<LaneType, List<Point>>> initTowerMap() {
        EnumMap<Team, EnumMap<LaneType, List<Point>>> towerTiles = new EnumMap<>(Team.class);
        for (Team team : List.of(Team.LIGHT, Team.DARK)) {
            EnumMap<LaneType, List<Point>> map = new EnumMap<>(LaneType.class);
            for (LaneType laneType : LaneType.values()) {
                map.put(laneType, new ArrayList<>());
            }
            towerTiles.put(team, map);
        }
        return towerTiles;
    }

    private LaneType parseLane(String token) {
        return switch (token) {
            case "l1" -> LaneType.TOP;
            case "l2" -> LaneType.MID;
            case "l3" -> LaneType.BOT;
            default -> throw new IllegalArgumentException("Unknown lane code: " + token);
        };
    }

    private List<Point> parsePointList(String[] parts, int fromIndex) {
        List<Point> points = new ArrayList<>();
        for (int i = fromIndex; i < parts.length; i++) {
            String[] xy = parts[i].split(",");
            points.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
        }
        return points;
    }

    private List<String> readLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private record TileLayers(GroundElement ground,
                              WaterElement water,
                              PropElement prop,
                              boolean blocked,
                              boolean lane,
                              int laneMask) {
    }

    private int laneBit(LaneType laneType) {
        return switch (laneType) {
            case TOP -> 1;
            case MID -> 1 << 1;
            case BOT -> 1 << 2;
        };
    }
}
