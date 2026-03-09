package com.example.demo.game.world.blueprint;

import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Structure;
import com.example.demo.game.model.StructureType;
import com.example.demo.game.model.Team;
import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.element.GroundKind;

import java.awt.Point;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public final class MapBlueprintWriter {
    public void save(Path path,
                     GameMap map,
                     Point playerStart,
                     EnumMap<LaneType, List<Point>> lanePaths,
                     List<Structure> structures) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# Codes\n");
        out.append("# gr1 grass\n");
        out.append("# gr2 grass_alt\n");
        out.append("# dt1 dirt\n");
        out.append("# hg1 high_ground\n");
        out.append("# bs1 base\n");
        out.append("# wd1 forest\n");
        out.append("# bl1 blocked\n");
        out.append("# rv1 river_water\n");
        out.append("# l1 top_lane\n");
        out.append("# l2 mid_lane\n");
        out.append("# l3 bot_lane\n");
        out.append("# bd1 boulder\n");
        out.append("# rk1 rock\n");
        out.append("# bh1 bush\n");
        out.append("# st1 stump\n");
        out.append("# pb1 pebbles\n\n");

        out.append("size ").append(map.getWidth()).append(' ').append(map.getHeight()).append('\n');
        out.append("player_start ").append(playerStart.x).append(' ').append(playerStart.y).append('\n');
        appendThrone(out, map, "light_throne", Team.LIGHT, structures);
        appendThrone(out, map, "dark_throne", Team.DARK, structures);
        appendTowers(out, map, "light_top_towers", Team.LIGHT, LaneType.TOP, structures);
        appendTowers(out, map, "light_mid_towers", Team.LIGHT, LaneType.MID, structures);
        appendTowers(out, map, "light_bot_towers", Team.LIGHT, LaneType.BOT, structures);
        appendTowers(out, map, "dark_top_towers", Team.DARK, LaneType.TOP, structures);
        appendTowers(out, map, "dark_mid_towers", Team.DARK, LaneType.MID, structures);
        appendTowers(out, map, "dark_bot_towers", Team.DARK, LaneType.BOT, structures);
        appendLanePath(out, "lane_path l1", lanePaths.get(LaneType.TOP));
        appendLanePath(out, "lane_path l2", lanePaths.get(LaneType.MID));
        appendLanePath(out, "lane_path l3", lanePaths.get(LaneType.BOT));
        out.append("grid\n");

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (x > 0) {
                    out.append(' ');
                }
                out.append(encodeTile(map, x, y));
            }
            out.append('\n');
        }

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }

    private void appendThrone(StringBuilder out, GameMap map, String label, Team team, List<Structure> structures) {
        Structure throne = structures.stream()
                .filter(structure -> structure.hp > 0 || structure.type == StructureType.THRONE)
                .filter(structure -> structure.team == team && structure.type == StructureType.THRONE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing throne for team " + team));
        out.append(label)
                .append(' ')
                .append(tileIndex(map, throne.x))
                .append(' ')
                .append(tileIndex(map, throne.y))
                .append('\n');
    }

    private void appendTowers(StringBuilder out, GameMap map, String label, Team team, LaneType lane, List<Structure> structures) {
        List<Structure> towers = structures.stream()
                .filter(structure -> structure.team == team)
                .filter(structure -> structure.type == StructureType.TOWER)
                .filter(structure -> structure.lane == lane)
                .sorted(Comparator.comparingInt(structure -> structure.laneOrder))
                .toList();
        out.append(label);
        for (Structure tower : towers) {
            out.append(' ')
                    .append(tileIndex(map, tower.x))
                    .append(',')
                    .append(tileIndex(map, tower.y));
        }
        out.append('\n');
    }

    private void appendLanePath(StringBuilder out, String label, List<Point> points) {
        out.append(label);
        for (Point point : points == null ? List.<Point>of() : points) {
            out.append(' ').append(point.x).append(',').append(point.y);
        }
        out.append('\n');
    }

    private String encodeTile(GameMap map, int x, int y) {
        List<String> layers = new ArrayList<>();
        GroundKind groundKind = map.getGround(x, y).kind();
        layers.add(switch (groundKind) {
            case FOREST -> "wd1";
            case GRASS -> "gr1";
            case GRASS_ALT -> "gr2";
            case DIRT -> "dt1";
            case LANE -> "dt1";
            case HIGH_GROUND -> "hg1";
            case BASE -> "bs1";
        });

        if (map.isBlocked(x, y) && groundKind != GroundKind.FOREST) {
            layers.add("bl1");
        }
        if (map.getWater(x, y) != null) {
            layers.add("rv1");
        }
        if (map.hasLaneType(x, y, LaneType.TOP)) {
            layers.add("l1");
        }
        if (map.hasLaneType(x, y, LaneType.MID)) {
            layers.add("l2");
        }
        if (map.hasLaneType(x, y, LaneType.BOT)) {
            layers.add("l3");
        }
        if (map.getProp(x, y) != null) {
            layers.add(switch (map.getProp(x, y).kind()) {
                case BOULDER -> "bd1";
                case ROCK -> "rk1";
                case BUSH -> "bh1";
                case STUMP -> "st1";
                case PEBBLES -> "pb1";
            });
        }
        return String.join("+", layers);
    }

    private int tileIndex(GameMap map, double worldCoord) {
        return Math.max(0, (int) Math.floor(worldCoord / map.getTileSize()));
    }
}
