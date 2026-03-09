package com.example.demo.game.world;

import com.example.demo.game.model.LaneType;
import com.example.demo.game.world.element.MapElements;

import java.awt.Point;
import java.util.List;
import java.util.Random;

public class MapGenerator {
    private static final double BASE_MAP_SIZE = 160.0;

    public void generate(GameMap map, Random random) {
        map.reset();

        paintBaseGround(map, random);

        int laneHalfWidth = scaledRadius(map, 2);
        carveLane(map, MapLayout.laneTiles(LaneType.TOP), laneHalfWidth);
        carveLane(map, MapLayout.laneTiles(LaneType.MID), laneHalfWidth);
        carveLane(map, MapLayout.laneTiles(LaneType.BOT), laneHalfWidth);

        carveBaseArea(map, MapLayout.LIGHT_THRONE_TILE, scaledRadius(map, 9));
        carveBaseArea(map, MapLayout.DARK_THRONE_TILE, scaledRadius(map, 9));

        paintRiver(map);
        paintHighGround(map);
        placeProps(map, random);
    }

    private void paintBaseGround(GameMap map, Random random) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.setGround(x, y, random.nextDouble() < 0.56 ? MapElements.GRASS : MapElements.GRASS_ALT);
                if (random.nextDouble() < 0.07) {
                    map.setGround(x, y, MapElements.DIRT);
                }
            }
        }
    }

    private void carveLane(GameMap map, List<Point> path, int halfWidth) {
        for (int i = 1; i < path.size(); i++) {
            carveSegment(map, path.get(i - 1), path.get(i), halfWidth, MapElements.LANE, true);
            carveSegment(map, path.get(i - 1), path.get(i), halfWidth + 1, MapElements.DIRT, false);
        }
    }

    private void carveBaseArea(GameMap map, Point center, int radius) {
        for (int y = center.y - radius; y <= center.y + radius; y++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                if (!map.inBounds(x, y)) {
                    continue;
                }
                if (distance(x, y, center.x, center.y) <= radius + 0.2) {
                    map.setBlocked(x, y, false);
                    map.setLane(x, y, false);
                    map.setWater(x, y, null);
                    map.setElevation(x, y, 1);
                    map.setGround(x, y, MapElements.BASE);
                    map.setProp(x, y, null);
                }
            }
        }
    }

    private void paintRiver(GameMap map) {
        int steps = Math.max(map.getWidth(), map.getHeight()) * 3;
        int riverRadius = scaledRadius(map, 4);
        double startX = -scaled(map, 10.0);
        double startY = scaled(map, 30.0);
        double c1X = scaled(map, 18.0);
        double c1Y = scaled(map, 25.0);
        double c2X = scaled(map, 31.0);
        double c2Y = scaled(map, 57.0);
        double endX = map.getWidth() - 1 + scaled(map, 12.0);
        double endY = scaled(map, 58.0);

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = cubic(startX, c1X, c2X, endX, t);
            double y = cubic(startY, c1Y, c2Y, endY, t);
            fillRiverDisc(map, (int) Math.round(x), (int) Math.round(y), riverRadius);
        }
    }

    private void fillRiverDisc(GameMap map, int cx, int cy, int radius) {
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (!map.inBounds(x, y)) {
                    continue;
                }
                if (distance(x, y, cx, cy) > radius + 0.35) {
                    continue;
                }
                if (map.isLane(x, y) && !isMidRiverCrossing(map, x, y)) {
                    continue;
                }

                map.setWater(x, y, MapElements.RIVER);
                map.setElevation(x, y, -1);
                if (!map.isLane(x, y)) {
                    map.setGround(x, y, map.getGround(x, y));
                }
                map.setBlocked(x, y, false);
                map.setProp(x, y, null);
            }
        }
    }

    private boolean isMidRiverCrossing(GameMap map, int x, int y) {
        int centerX = map.getWidth() / 2;
        int centerY = map.getHeight() / 2;
        int allowance = scaledRadius(map, 8);
        return Math.abs(x - centerX) <= allowance && Math.abs(y - centerY) <= allowance;
    }

    private void paintHighGround(GameMap map) {
        raiseRegion(map, sx(map, 20), sy(map, 20), sx(map, 70), sy(map, 70));
        raiseRegion(map, sx(map, 90), sy(map, 90), sx(map, 140), sy(map, 140));
        raiseRegion(map, sx(map, 20), sy(map, 90), sx(map, 70), sy(map, 140));
        raiseRegion(map, sx(map, 90), sy(map, 20), sx(map, 140), sy(map, 70));
        raiseRegion(map, sx(map, 33), sy(map, 119), sx(map, 67), sy(map, 151));
        raiseRegion(map, sx(map, 93), sy(map, 9), sx(map, 127), sy(map, 41));
    }

    private void raiseRegion(GameMap map, int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (!map.inBounds(x, y) || map.isLane(x, y) || map.isRiver(x, y)) {
                    continue;
                }
                map.setElevation(x, y, 1);
                if (map.getGround(x, y) != MapElements.BASE) {
                    map.setGround(x, y, MapElements.HIGH_GROUND);
                }
            }
        }
    }

    private void carveJungleRoadsAndCamps(GameMap map) {
        int trailHalfWidth = scaledRadius(map, 2);
        carveTrail(map, List.of(p(map, 23, 129), p(map, 38, 112), p(map, 54, 96), p(map, 71, 80)), trailHalfWidth);
        carveTrail(map, List.of(p(map, 42, 144), p(map, 58, 127), p(map, 74, 109), p(map, 90, 92)), trailHalfWidth);
        carveTrail(map, List.of(p(map, 17, 99), p(map, 33, 87), p(map, 52, 76), p(map, 70, 67)), trailHalfWidth);

        carveTrail(map, List.of(p(map, 137, 31), p(map, 121, 48), p(map, 106, 64), p(map, 90, 80)), trailHalfWidth);
        carveTrail(map, List.of(p(map, 119, 16), p(map, 102, 33), p(map, 86, 51), p(map, 70, 68)), trailHalfWidth);
        carveTrail(map, List.of(p(map, 144, 61), p(map, 128, 73), p(map, 109, 84), p(map, 90, 93)), trailHalfWidth);

        for (Point camp : MapLayout.neutralCampTiles()) {
            carveCamp(map, camp, scaledRadius(map, 4));
        }
    }

    private void carveTrail(GameMap map, List<Point> points, int halfWidth) {
        for (int i = 1; i < points.size(); i++) {
            carveSegment(map, points.get(i - 1), points.get(i), halfWidth, MapElements.DIRT, false);
        }
    }

    private void carveCamp(GameMap map, Point center, int radius) {
        for (int y = center.y - radius; y <= center.y + radius; y++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                if (!map.inBounds(x, y)) {
                    continue;
                }
                if (distance(x, y, center.x, center.y) <= radius + 0.2) {
                    map.setBlocked(x, y, false);
                    map.setGround(x, y, MapElements.GRASS_ALT);
                    map.setProp(x, y, null);
                }
            }
        }
    }

    private void carveSegment(GameMap map,
                              Point from,
                              Point to,
                              int radius,
                              com.example.demo.game.world.element.GroundElement ground,
                              boolean markLane) {
        int x = from.x;
        int y = from.y;

        while (x != to.x || y != to.y) {
            fillDisc(map, x, y, radius, ground, markLane);
            if (x != to.x) {
                x += Integer.compare(to.x, x);
            }
            if (y != to.y) {
                y += Integer.compare(to.y, y);
            }
        }

        fillDisc(map, to.x, to.y, radius, ground, markLane);
    }

    private void fillDisc(GameMap map, int cx, int cy, int radius, com.example.demo.game.world.element.GroundElement ground, boolean markLane) {
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (!map.inBounds(x, y)) {
                    continue;
                }
                if (distance(x, y, cx, cy) > radius + 0.3) {
                    continue;
                }

                map.setBlocked(x, y, false);
                if (!map.isRiver(x, y)) {
                    map.setGround(x, y, ground);
                }
                if (markLane) {
                    map.setLane(x, y, true);
                    map.setElevation(x, y, 0);
                }
                map.setProp(x, y, null);
            }
        }
    }

    private void plantForest(GameMap map, Random random) {
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (mustStayOpen(map, x, y)) {
                    continue;
                }

                double chance = map.getElevation(x, y) > 0 ? 0.58 : 0.48;
                if (random.nextDouble() < chance) {
                    map.setBlocked(x, y, true);
                    map.setGround(x, y, MapElements.FOREST);
                    map.setProp(x, y, null);
                }
            }
        }

        for (Point camp : MapLayout.neutralCampTiles()) {
            carveCamp(map, camp, scaledRadius(map, 4));
        }
    }

    private void placeProps(GameMap map, Random random) {
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (map.isBlocked(x, y) || map.isLane(x, y) || map.isRiver(x, y) || map.getGround(x, y) == MapElements.BASE) {
                    continue;
                }

                double roll = random.nextDouble();
                if (roll < 0.020) {
                    map.setProp(x, y, MapElements.ROCK);
                } else if (roll < 0.032) {
                    map.setProp(x, y, MapElements.PEBBLES);
                }
            }
        }
    }

    private boolean mustStayOpen(GameMap map, int x, int y) {
        if (map.isLane(x, y) || map.isRiver(x, y) || map.getGround(x, y) == MapElements.BASE) {
            return true;
        }

        if (map.getGround(x, y) == MapElements.DIRT) {
            return true;
        }

        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int nx = x + ox;
                int ny = y + oy;
                if (!map.inBounds(nx, ny)) {
                    continue;
                }
                if (map.isLane(nx, ny)) {
                    return true;
                }
            }
        }

        return false;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private double cubic(double p0, double p1, double p2, double p3, double t) {
        double inv = 1.0 - t;
        return inv * inv * inv * p0
                + 3.0 * inv * inv * t * p1
                + 3.0 * inv * t * t * p2
                + t * t * t * p3;
    }

    private Point p(GameMap map, int x, int y) {
        return new Point(sx(map, x), sy(map, y));
    }

    private int sx(GameMap map, int value) {
        return clamp((int) Math.round(value * map.getWidth() / BASE_MAP_SIZE), 0, map.getWidth() - 1);
    }

    private int sy(GameMap map, int value) {
        return clamp((int) Math.round(value * map.getHeight() / BASE_MAP_SIZE), 0, map.getHeight() - 1);
    }

    private int scaledRadius(GameMap map, int value) {
        return Math.max(1, (int) Math.round(value * Math.min(map.getWidth(), map.getHeight()) / BASE_MAP_SIZE));
    }

    private double scaled(GameMap map, double value) {
        return value * Math.min(map.getWidth(), map.getHeight()) / BASE_MAP_SIZE;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
