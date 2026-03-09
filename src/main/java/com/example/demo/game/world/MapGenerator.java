package com.example.demo.game.world;

import com.example.demo.game.model.LaneType;

import java.awt.Point;
import java.util.List;
import java.util.Random;

public class MapGenerator {

    public void generate(GameMap map, Random random) {
        map.reset(random);

        paintBaseGround(map, random);

        carveLane(map, MapLayout.laneTiles(LaneType.TOP), 2);
        carveLane(map, MapLayout.laneTiles(LaneType.MID), 2);
        carveLane(map, MapLayout.laneTiles(LaneType.BOT), 2);

        carveBaseArea(map, MapLayout.LIGHT_THRONE_TILE, 12);
        carveBaseArea(map, MapLayout.DARK_THRONE_TILE, 12);

        paintRiver(map);
        paintHighGround(map);
        carveJungleRoadsAndCamps(map);

        plantForest(map, random);
        placeProps(map, random);
    }

    private void paintBaseGround(GameMap map, Random random) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.setGround(x, y, random.nextDouble() < 0.56 ? GroundType.GRASS : GroundType.GRASS_ALT);
                if (random.nextDouble() < 0.07) {
                    map.setGround(x, y, GroundType.DIRT);
                }
            }
        }
    }

    private void carveLane(GameMap map, List<Point> path, int halfWidth) {
        for (int i = 1; i < path.size(); i++) {
            carveSegment(map, path.get(i - 1), path.get(i), halfWidth, GroundType.LANE, true);
            carveSegment(map, path.get(i - 1), path.get(i), halfWidth + 1, GroundType.DIRT, false);
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
                    map.setRiver(x, y, false);
                    map.setElevation(x, y, 1);
                    map.setGround(x, y, GroundType.BASE);
                    map.setProp(x, y, PropType.NONE);
                }
            }
        }
    }

    private void paintRiver(GameMap map) {
        int baseY = map.getHeight() / 2;
        int centerX = map.getWidth() / 2;

        for (int x = 0; x < map.getWidth(); x++) {
            int yCenter = baseY + (int) Math.round(Math.sin(x * 0.07) * 2.0);
            for (int y = yCenter - 4; y <= yCenter + 4; y++) {
                if (!map.inBounds(x, y)) {
                    continue;
                }

                map.setRiver(x, y, true);
                map.setElevation(x, y, -1);

                if (Math.abs(x - centerX) <= 7 && map.isLane(x, y)) {
                    map.setGround(x, y, GroundType.DIRT);
                } else {
                    map.setGround(x, y, GroundType.RIVER);
                }

                map.setBlocked(x, y, false);
                map.setProp(x, y, PropType.NONE);
            }
        }
    }

    private void paintHighGround(GameMap map) {
        raiseRegion(map, 34, 22, 92, 66);
        raiseRegion(map, 126, 72, 186, 118);
        raiseRegion(map, 50, 88, 92, 124);
        raiseRegion(map, 122, 18, 164, 54);
    }

    private void raiseRegion(GameMap map, int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (!map.inBounds(x, y) || map.isLane(x, y) || map.isRiver(x, y)) {
                    continue;
                }
                map.setElevation(x, y, 1);
                if (map.getGround(x, y) != GroundType.BASE) {
                    map.setGround(x, y, GroundType.HIGH_GROUND);
                }
            }
        }
    }

    private void carveJungleRoadsAndCamps(GameMap map) {
        carveTrail(map, List.of(new Point(30, 110), new Point(58, 92), new Point(86, 74), new Point(110, 70)), 2);
        carveTrail(map, List.of(new Point(40, 124), new Point(64, 116), new Point(84, 102)), 2);

        carveTrail(map, List.of(new Point(188, 30), new Point(162, 48), new Point(136, 66), new Point(110, 70)), 2);
        carveTrail(map, List.of(new Point(180, 54), new Point(156, 70), new Point(136, 86)), 2);

        for (Point camp : MapLayout.neutralCampTiles()) {
            carveCamp(map, camp, 5);
        }
    }

    private void carveTrail(GameMap map, List<Point> points, int halfWidth) {
        for (int i = 1; i < points.size(); i++) {
            carveSegment(map, points.get(i - 1), points.get(i), halfWidth, GroundType.DIRT, false);
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
                    map.setGround(x, y, GroundType.GRASS_ALT);
                    map.setProp(x, y, PropType.NONE);
                }
            }
        }
    }

    private void carveSegment(GameMap map,
                              Point from,
                              Point to,
                              int radius,
                              GroundType ground,
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

    private void fillDisc(GameMap map, int cx, int cy, int radius, GroundType ground, boolean markLane) {
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
                map.setProp(x, y, PropType.NONE);
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
                    map.setGround(x, y, GroundType.FOREST);
                    map.setProp(x, y, PropType.NONE);
                }
            }
        }

        for (Point camp : MapLayout.neutralCampTiles()) {
            carveCamp(map, camp, 5);
        }
    }

    private void placeProps(GameMap map, Random random) {
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (map.isBlocked(x, y) || map.isLane(x, y) || map.isRiver(x, y) || map.getGround(x, y) == GroundType.BASE) {
                    continue;
                }

                double roll = random.nextDouble();
                if (roll < 0.018) {
                    map.setProp(x, y, PropType.ROCK);
                } else if (roll < 0.031) {
                    map.setProp(x, y, PropType.BUSH);
                } else if (roll < 0.037) {
                    map.setProp(x, y, PropType.STUMP);
                }
            }
        }
    }

    private boolean mustStayOpen(GameMap map, int x, int y) {
        if (map.isLane(x, y) || map.isRiver(x, y) || map.getGround(x, y) == GroundType.BASE) {
            return true;
        }

        if (map.getGround(x, y) == GroundType.DIRT || map.getGround(x, y) == GroundType.GRASS_ALT) {
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
}
