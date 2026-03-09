package com.example.demo.game.render;

import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.element.GroundKind;
import com.example.demo.game.world.element.PropElement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

public class MapRenderer {

    public BufferedImage buildLayer(GameMap map) {
        BufferedImage layer = new BufferedImage(map.getPixelWidth(), map.getPixelHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = layer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int sx = x * map.getTileSize();
                int sy = y * map.getTileSize();
                drawGroundTile(g2, map, x, y, sx, sy);
                drawWaterTile(g2, map, x, y, sx, sy);
            }
        }

        drawElevationEdges(g2, map);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int sx = x * map.getTileSize();
                int sy = y * map.getTileSize();

                if (map.isBlocked(x, y)) {
                    drawTree(g2, map.getTileSize(), x, y, sx, sy, map.getTreeVariant(x, y), map.getTreeTint(x, y));
                } else if (map.getProp(x, y) != null) {
                    drawProp(g2, sx, sy, map.getProp(x, y));
                }
            }
        }

        drawBoundary(g2, map);

        g2.dispose();
        return layer;
    }

    private void drawGroundTile(Graphics2D g2, GameMap map, int tileX, int tileY, int sx, int sy) {
        GroundKind type = map.getGround(tileX, tileY).kind();
        int tile = map.getTileSize();

        Color base = switch (type) {
            case FOREST -> new Color(86, 128, 76);
            case GRASS -> new Color(102, 146, 86);
            case GRASS_ALT -> new Color(88, 131, 74);
            case DIRT -> new Color(125, 103, 74);
            case LANE -> new Color(96, 87, 78);
            case HIGH_GROUND -> new Color(126, 160, 102);
            case BASE -> new Color(126, 118, 108);
        };

        int noise = tileNoise(tileX, tileY);
        int variation = (noise & 7) - 3;

        int elevation = map.getElevation(tileX, tileY);
        if (elevation > 0) {
            variation += 2;
        } else if (elevation < 0) {
            variation -= 3;
        }

        if (type == GroundKind.FOREST && ((noise >> 4) & 31) < 6) {
            variation -= 5;
        }

        g2.setColor(shift(base, variation * 2));
        g2.fillRect(sx, sy, tile, tile);

        if (map.isLane(tileX, tileY)) {
            drawLaneMarkings(g2, sx, sy, tile, noise, false);
        } else if (type == GroundKind.GRASS || type == GroundKind.GRASS_ALT || type == GroundKind.HIGH_GROUND || type == GroundKind.FOREST) {
            g2.setColor(new Color(54, 95, 45, 70));
            int bladeCount = 2 + (noise & 1);
            for (int i = 0; i < bladeCount; i++) {
                int bx = sx + 4 + ((noise >> (i * 3)) & 15);
                int by = sy + 6 + ((noise >> (i * 5)) & 11);
                g2.drawLine(bx, by, bx, by + 3);
            }
        }
    }

    private void drawWaterTile(Graphics2D g2, GameMap map, int tileX, int tileY, int sx, int sy) {
        if (map.getWater(tileX, tileY) == null) {
            return;
        }

        int tile = map.getTileSize();
        g2.setColor(new Color(74, 120, 160, 210));
        g2.fillRect(sx, sy, tile, tile);
        g2.setColor(new Color(146, 202, 226, 110));
        g2.drawLine(sx + 2, sy + 6, sx + tile - 3, sy + 6);
        g2.drawLine(sx + 4, sy + 14, sx + tile - 5, sy + 14);

        if (map.isLane(tileX, tileY)) {
            drawLaneMarkings(g2, sx, sy, tile, tileNoise(tileX, tileY), true);
        }
    }

    private void drawLaneMarkings(Graphics2D g2, int sx, int sy, int tile, int noise, boolean overWater) {
        int baseShift = ((noise >> 3) & 3) - 1;
        int accentShift = ((noise >> 5) & 3) - 1;

        Color laneBase = shift(new Color(82, 75, 68), baseShift * 4);
        Color laneAccent = shift(new Color(118, 108, 96), accentShift * 4);
        Color laneCenter = shift(new Color(62, 56, 50), baseShift * 3);

        int baseAlpha = overWater ? 155 : 120;
        int accentAlpha = overWater ? 220 : 210;
        int centerAlpha = overWater ? 170 : 155;

        g2.setColor(withAlpha(laneBase, baseAlpha));
        g2.fillRect(sx + 1, sy + 5, tile - 2, tile - 10);

        g2.setColor(withAlpha(laneAccent, accentAlpha));
        g2.drawLine(sx + 2, sy + 8, sx + tile - 3, sy + 8);
        g2.drawLine(sx + 2, sy + 16, sx + tile - 3, sy + 16);

        g2.setColor(withAlpha(laneCenter, centerAlpha));
        g2.drawLine(sx + 1, sy + 12, sx + tile - 2, sy + 12);
    }

    private void drawElevationEdges(Graphics2D g2, GameMap map) {
        int tile = map.getTileSize();
        g2.setColor(new Color(48, 70, 44, 110));

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int elev = map.getElevation(x, y);
                if (x < map.getWidth() - 1 && elev > map.getElevation(x + 1, y)) {
                    int sx = x * tile + tile - 1;
                    int sy = y * tile;
                    g2.drawLine(sx, sy, sx, sy + tile - 1);
                }
                if (y < map.getHeight() - 1 && elev > map.getElevation(x, y + 1)) {
                    int sx = x * tile;
                    int sy = y * tile + tile - 1;
                    g2.drawLine(sx, sy, sx + tile - 1, sy);
                }
            }
        }
    }

    private void drawTree(Graphics2D g2, int tile, int tileX, int tileY, int sx, int sy, int variant, int tintLevel) {
        Color leafMain = shift(new Color(46, 118, 56), tintLevel * 7);
        Color leafDark = shift(new Color(32, 89, 42), tintLevel * 6);
        Color leafLight = shift(new Color(72, 140, 74), tintLevel * 8);

        g2.setColor(new Color(58, 93, 52, 52));
        g2.fillOval(sx + 1, sy + tile - 15, tile - 2, 11);
        g2.setColor(new Color(48, 80, 44, 34));
        g2.fillOval(sx - 1, sy + tile - 17, tile + 2, 13);

        int underNoise = tileNoise(tileX, tileY);
        if ((underNoise & 31) < 6) {
            g2.setColor(new Color(74, 114, 66, 150));
        } else {
            g2.setColor(new Color(96, 141, 84, 125));
        }
        g2.fillOval(sx + 3, sy + tile - 7, tile - 6, 7);

        g2.setColor(new Color(98, 70, 46));
        g2.fillRoundRect(sx + tile / 2 - 3, sy + tile - 11, 6, 10, 3, 3);

        switch (variant % 4) {
            case 0 -> {
                g2.setColor(leafDark);
                g2.fillOval(sx + 1, sy - 4, 15, 15);
                g2.fillOval(sx + 9, sy - 6, 14, 14);
                g2.fillOval(sx + 5, sy + 3, 16, 16);
                g2.setColor(leafLight);
                g2.fillOval(sx + 7, sy - 3, 9, 9);
            }
            case 1 -> {
                g2.setColor(leafDark);
                Polygon p1 = new Polygon(new int[]{sx + tile / 2, sx + 3, sx + tile - 3}, new int[]{sy - 10, sy + 8, sy + 8}, 3);
                Polygon p2 = new Polygon(new int[]{sx + tile / 2, sx + 4, sx + tile - 4}, new int[]{sy - 2, sy + 12, sy + 12}, 3);
                g2.fillPolygon(p1);
                g2.setColor(leafMain);
                g2.fillPolygon(p2);
                g2.setColor(leafLight);
                g2.fillOval(sx + 10, sy + 1, 6, 6);
            }
            case 2 -> {
                g2.setColor(leafDark);
                g2.fillOval(sx + 2, sy - 8, 20, 14);
                g2.setColor(leafMain);
                g2.fillOval(sx, sy, 14, 14);
                g2.fillOval(sx + 10, sy, 14, 14);
                g2.setColor(leafLight);
                g2.fillOval(sx + 8, sy - 2, 8, 8);
            }
            default -> {
                g2.setColor(leafDark);
                g2.fillOval(sx + 3, sy - 7, 18, 18);
                g2.setColor(leafMain);
                g2.fillOval(sx + 1, sy + 1, 12, 12);
                g2.fillOval(sx + 11, sy + 1, 12, 12);
                g2.setColor(leafLight);
                g2.fillOval(sx + 9, sy - 3, 7, 7);
            }
        }
    }

    private void drawProp(Graphics2D g2, int sx, int sy, PropElement type) {
        switch (type.kind()) {
            case ROCK -> {
                g2.setColor(new Color(85, 88, 91));
                g2.fillOval(sx + 6, sy + 11, 12, 8);
                g2.setColor(new Color(120, 124, 128));
                g2.fillOval(sx + 8, sy + 10, 6, 4);
            }
            case BUSH -> {
                g2.setColor(new Color(41, 102, 48));
                g2.fillOval(sx + 4, sy + 10, 8, 8);
                g2.fillOval(sx + 10, sy + 8, 9, 9);
                g2.fillOval(sx + 7, sy + 13, 10, 8);
                g2.setColor(new Color(61, 126, 67));
                g2.fillOval(sx + 10, sy + 9, 4, 4);
            }
            case STUMP -> {
                g2.setColor(new Color(122, 92, 57));
                g2.fillOval(sx + 8, sy + 11, 8, 8);
                g2.setColor(new Color(156, 124, 82));
                g2.drawOval(sx + 9, sy + 12, 6, 6);
                g2.drawOval(sx + 10, sy + 13, 4, 4);
            }
            case PEBBLES -> {
                g2.setColor(new Color(122, 113, 99));
                g2.fillOval(sx + 8, sy + 13, 3, 3);
                g2.fillOval(sx + 12, sy + 10, 2, 2);
                g2.fillOval(sx + 15, sy + 14, 2, 2);
            }
        }
    }

    private void drawBoundary(Graphics2D g2, GameMap map) {
        int pad = map.getTileSize();
        Stroke old = g2.getStroke();
        g2.setColor(new Color(241, 232, 143, 180));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{10f, 8f}, 0f));
        g2.drawRect(pad, pad, map.getPixelWidth() - pad * 2, map.getPixelHeight() - pad * 2);
        g2.setStroke(old);
    }

    private int tileNoise(int x, int y) {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ (n >> 13)) * 1274126177;
        return n ^ (n >> 16);
    }

    private Color shift(Color base, int delta) {
        int r = clampColor(base.getRed() + delta);
        int g = clampColor(base.getGreen() + delta);
        int b = clampColor(base.getBlue() + delta);
        return new Color(r, g, b);
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
