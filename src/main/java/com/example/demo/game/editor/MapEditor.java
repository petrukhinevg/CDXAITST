package com.example.demo.game.editor;

import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.GroundType;
import com.example.demo.game.world.PropType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

public final class MapEditor {
    private final Map<Long, TileOverride> overrides = new HashMap<>();
    private boolean editMode;

    public enum TilePaintAction {
        BLOCKED,
        CLEARED
    }

    private enum TileOverride {
        BLOCKED,
        CLEARED
    }

    public record HoveredTile(boolean inBounds, int x, int y, boolean editable) {
    }

    public void toggleEditMode() {
        editMode = !editMode;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void clearOverrides() {
        overrides.clear();
    }

    public int overrideCount() {
        return overrides.size();
    }

    public boolean hasOverrides() {
        return !overrides.isEmpty();
    }

    public void applyStoredOverrides(GameMap map) {
        for (Map.Entry<Long, TileOverride> entry : overrides.entrySet()) {
            int tileX = decodeTileX(entry.getKey());
            int tileY = decodeTileY(entry.getKey());
            applyTileOverride(map, tileX, tileY, entry.getValue());
        }
    }

    public boolean paintAtScreenPoint(GameMap map,
                                      int mouseX,
                                      int mouseY,
                                      double cameraX,
                                      double cameraY,
                                      double zoom,
                                      TilePaintAction action) {
        HoveredTile hoveredTile = hoveredTile(map, mouseX, mouseY, cameraX, cameraY, zoom);
        if (!hoveredTile.inBounds() || !hoveredTile.editable()) {
            return false;
        }

        long key = tileKey(hoveredTile.x(), hoveredTile.y());
        TileOverride override = toOverride(action);
        if (overrides.get(key) == override) {
            return false;
        }

        overrides.put(key, override);
        applyTileOverride(map, hoveredTile.x(), hoveredTile.y(), override);
        return true;
    }

    public HoveredTile hoveredTile(GameMap map,
                                   int mouseX,
                                   int mouseY,
                                   double cameraX,
                                   double cameraY,
                                   double zoom) {
        int tileX = (int) (screenToWorldX(cameraX, mouseX, zoom) / map.getTileSize());
        int tileY = (int) (screenToWorldY(cameraY, mouseY, zoom) / map.getTileSize());
        boolean inBounds = map.inBounds(tileX, tileY);
        return new HoveredTile(inBounds, tileX, tileY, inBounds && isEditableTile(map, tileX, tileY));
    }

    public void drawOverlay(Graphics2D g2,
                            GameMap map,
                            int panelWidth,
                            int panelHeight,
                            double cameraX,
                            double cameraY,
                            double zoom,
                            int mouseX,
                            int mouseY) {
        if (!editMode) {
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int tileSize = (int) Math.round(map.getTileSize() * zoom);
        for (Map.Entry<Long, TileOverride> entry : overrides.entrySet()) {
            int tileX = decodeTileX(entry.getKey());
            int tileY = decodeTileY(entry.getKey());
            if (!map.inBounds(tileX, tileY)) {
                continue;
            }

            int sx = worldToScreenX(cameraX, tileX * map.getTileSize(), zoom);
            int sy = worldToScreenY(cameraY, tileY * map.getTileSize(), zoom);
            if (sx + tileSize < 0 || sy + tileSize < 0 || sx > panelWidth || sy > panelHeight) {
                continue;
            }

            g2.setColor(entry.getValue() == TileOverride.BLOCKED
                    ? new Color(255, 120, 96, 110)
                    : new Color(100, 225, 220, 110));
            g2.fillRect(sx, sy, tileSize, tileSize);
        }

        HoveredTile hoveredTile = hoveredTile(map, mouseX, mouseY, cameraX, cameraY, zoom);
        if (!hoveredTile.inBounds()) {
            return;
        }

        int sx = worldToScreenX(cameraX, hoveredTile.x() * map.getTileSize(), zoom);
        int sy = worldToScreenY(cameraY, hoveredTile.y() * map.getTileSize(), zoom);
        g2.setColor(hoveredTile.editable()
                ? new Color(255, 245, 170, 210)
                : new Color(255, 90, 90, 210));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(sx, sy, tileSize, tileSize);
    }

    private void applyTileOverride(GameMap map, int tileX, int tileY, TileOverride override) {
        if (!map.inBounds(tileX, tileY) || !isEditableTile(map, tileX, tileY)) {
            return;
        }

        map.setProp(tileX, tileY, PropType.NONE);
        if (override == TileOverride.BLOCKED) {
            map.setBlocked(tileX, tileY, true);
            map.setGround(tileX, tileY, GroundType.FOREST);
            return;
        }

        map.setBlocked(tileX, tileY, false);
        map.setGround(tileX, tileY, map.getElevation(tileX, tileY) > 0 ? GroundType.HIGH_GROUND : GroundType.GRASS_ALT);
    }

    private boolean isEditableTile(GameMap map, int tileX, int tileY) {
        return map.inBounds(tileX, tileY)
                && !map.isLane(tileX, tileY)
                && !map.isRiver(tileX, tileY)
                && map.getGround(tileX, tileY) != GroundType.BASE;
    }

    private TileOverride toOverride(TilePaintAction action) {
        return action == TilePaintAction.BLOCKED ? TileOverride.BLOCKED : TileOverride.CLEARED;
    }

    private long tileKey(int tileX, int tileY) {
        return ((long) tileY << 32) | (tileX & 0xffffffffL);
    }

    private int decodeTileX(long key) {
        return (int) key;
    }

    private int decodeTileY(long key) {
        return (int) (key >>> 32);
    }

    private int worldToScreenX(double cameraX, double worldX, double zoom) {
        return (int) Math.round((worldX - cameraX) * zoom);
    }

    private int worldToScreenY(double cameraY, double worldY, double zoom) {
        return (int) Math.round((worldY - cameraY) * zoom);
    }

    private double screenToWorldX(double cameraX, double screenX, double zoom) {
        return cameraX + screenX / zoom;
    }

    private double screenToWorldY(double cameraY, double screenY, double zoom) {
        return cameraY + screenY / zoom;
    }
}
