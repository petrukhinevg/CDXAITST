package com.example.demo.game.render;

import com.example.demo.game.editor.MapEditor;
import com.example.demo.game.model.Creep;
import com.example.demo.game.model.Player;
import com.example.demo.game.model.Structure;
import com.example.demo.game.model.StructureType;
import com.example.demo.game.model.Team;
import com.example.demo.game.model.WeaponType;
import com.example.demo.game.world.GameMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class HudRenderer {
    private static final int MINIMAP_W = 252;
    private static final int MINIMAP_H = 168;
    private static final int MINIMAP_PAD = 8;

    public record Model(int panelWidth,
                        int panelHeight,
                        double zoom,
                        Player player,
                        WeaponType currentWeapon,
                        int ammoInMagazine,
                        double reloadTimer,
                        Structure lightThrone,
                        Structure darkThrone,
                        int laneCreepCount,
                        int neutralCreepCount,
                        int kills,
                        int currentFps,
                        int targetFps,
                        boolean editMode,
                        int manualTileCount,
                        MapEditor.HoveredTile hoveredTile,
                        BufferedImage miniMapLayer,
                        GameMap map,
                        double cameraX,
                        double cameraY,
                        List<Structure> structures,
                        List<Creep> laneCreeps,
                        List<Creep> neutralCreeps,
                        boolean gameOver,
                        String victoryText) {
    }

    public BufferedImage buildMiniMapLayer(BufferedImage mapLayer) {
        if (mapLayer == null) {
            return null;
        }

        int miniW = Math.max(1, MINIMAP_W - MINIMAP_PAD * 2);
        int miniH = Math.max(1, MINIMAP_H - MINIMAP_PAD * 2);
        BufferedImage mini = new BufferedImage(miniW, miniH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mini.createGraphics();
        g2.drawImage(mapLayer, 0, 0, miniW, miniH, null);
        g2.dispose();
        return mini;
    }

    public void draw(Graphics2D g2, Model model) {
        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(16, 16, 640, 302, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Силы света (герой)", 30, 42);
        g2.drawString("Оружие: " + model.currentWeapon().displayName(), 30, 116);
        g2.drawString("Уровень: " + model.player().level, 30, 178);

        drawBar(g2, 30, 50, 300, 14,
                model.player().maxHp == 0 ? 0.0 : (double) model.player().hp / model.player().maxHp,
                new Color(223, 79, 77), new Color(75, 33, 32), new Color(205, 205, 205));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString(Math.max(0, model.player().hp) + " / " + model.player().maxHp, 344, 62);

        double reloadRatio = model.currentWeapon().reloadSeconds() == 0.0
                ? 1.0
                : 1.0 - model.reloadTimer() / model.currentWeapon().reloadSeconds();
        if (model.reloadTimer() > 0.0) {
            g2.drawString("Перезарядка...", 30, 137);
            drawBar(g2, 138, 125, 192, 11, reloadRatio,
                    new Color(255, 206, 109), new Color(92, 74, 39), new Color(190, 190, 190));
        } else {
            g2.drawString("Магазин: " + model.ammoInMagazine() + " / " + model.currentWeapon().magazineSize(), 30, 137);
            drawBar(g2, 198, 125, 132, 11,
                    model.currentWeapon().magazineSize() == 0 ? 0.0 : (double) model.ammoInMagazine() / model.currentWeapon().magazineSize(),
                    new Color(245, 220, 143), new Color(92, 74, 39), new Color(190, 190, 190));
        }

        g2.drawString("XP: " + model.player().xp + " / " + model.player().xpToNextLevel, 30, 200);
        drawBar(g2, 30, 208, 300, 12,
                model.player().xpToNextLevel == 0 ? 0.0 : (double) model.player().xp / model.player().xpToNextLevel,
                new Color(91, 190, 255), new Color(31, 67, 93), new Color(190, 190, 190));

        int rightX = 380;
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Силы света / тьмы", rightX, 42);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString("Трон света: " + Math.max(0, model.lightThrone().hp), rightX, 66);
        drawBar(g2, rightX, 72, 220, 10,
                model.lightThrone().maxHp == 0 ? 0.0 : (double) model.lightThrone().hp / model.lightThrone().maxHp,
                new Color(90, 176, 255), new Color(32, 55, 83), new Color(190, 190, 190));

        g2.drawString("Трон тьмы: " + Math.max(0, model.darkThrone().hp), rightX, 96);
        drawBar(g2, rightX, 102, 220, 10,
                model.darkThrone().maxHp == 0 ? 0.0 : (double) model.darkThrone().hp / model.darkThrone().maxHp,
                new Color(237, 95, 88), new Color(83, 34, 31), new Color(190, 190, 190));

        g2.drawString("Крипы на линиях: " + model.laneCreepCount(), rightX, 132);
        g2.drawString("Нейтралы: " + model.neutralCreepCount(), rightX, 150);
        g2.drawString("Фраги героя: " + model.kills(), rightX, 168);
        g2.drawString("FPS: " + model.currentFps() + " / " + model.targetFps(), rightX, 186);

        g2.drawString("1 Камень  2 Лук  3 Меч", rightX, 210);
        g2.drawString("WASD - движение, ЛКМ - атака", rightX, 228);
        g2.drawString("E - режим правки, N - новая база", rightX, 246);
        g2.drawString("В правке: ЛКМ ставит блок, ПКМ очищает", rightX, 264);
        g2.drawString("C - сбросить ручные правки", rightX, 282);
        g2.drawString("Ручных тайлов: " + model.manualTileCount(), rightX, 300);

        g2.setColor(model.editMode() ? new Color(255, 228, 157) : new Color(188, 188, 188));
        g2.drawString("Редактор карты: " + (model.editMode() ? "включен" : "выключен"), 30, 246);
        g2.drawString(hoverText(model.hoveredTile()), 30, 264);
        g2.setColor(Color.WHITE);

        drawMiniMap(g2, model);

        if (model.gameOver()) {
            g2.setColor(new Color(0, 0, 0, 185));
            g2.fillRect(0, 0, model.panelWidth(), model.panelHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 42));
            g2.drawString(model.victoryText(), model.panelWidth() / 2 - 240, model.panelHeight() / 2 - 10);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
            g2.drawString("Press R to restart", model.panelWidth() / 2 - 96, model.panelHeight() / 2 + 30);
        }
    }

    private void drawMiniMap(Graphics2D g2, Model model) {
        int panelX = model.panelWidth() - MINIMAP_W - 16;
        int panelY = 16;
        int mapX = panelX + MINIMAP_PAD;
        int mapY = panelY + MINIMAP_PAD;
        int mapW = MINIMAP_W - MINIMAP_PAD * 2;
        int mapH = MINIMAP_H - MINIMAP_PAD * 2;

        g2.setColor(new Color(0, 0, 0, 165));
        g2.fillRoundRect(panelX, panelY, MINIMAP_W, MINIMAP_H, 10, 10);
        g2.setColor(new Color(235, 235, 235, 180));
        g2.drawRoundRect(panelX, panelY, MINIMAP_W, MINIMAP_H, 10, 10);

        if (model.miniMapLayer() != null) {
            g2.drawImage(model.miniMapLayer(), mapX, mapY, mapW, mapH, null);
        }

        double scaleX = mapW / (double) model.map().getPixelWidth();
        double scaleY = mapH / (double) model.map().getPixelHeight();

        for (Structure structure : model.structures()) {
            if (structure.hp <= 0) {
                continue;
            }
            int x = mapX + (int) Math.round(structure.x * scaleX);
            int y = mapY + (int) Math.round(structure.y * scaleY);
            g2.setColor(structure.team == Team.LIGHT ? new Color(110, 188, 255) : new Color(243, 99, 90));
            int size = structure.type == StructureType.THRONE ? 5 : 4;
            g2.fillRect(x - size / 2, y - size / 2, size, size);
        }

        for (Creep creep : model.laneCreeps()) {
            if (creep.hp <= 0) {
                continue;
            }
            int x = mapX + (int) Math.round(creep.x * scaleX);
            int y = mapY + (int) Math.round(creep.y * scaleY);
            g2.setColor(creep.team == Team.LIGHT ? new Color(137, 209, 255, 220) : new Color(255, 120, 109, 220));
            g2.fillRect(x, y, 2, 2);
        }

        for (Creep creep : model.neutralCreeps()) {
            if (creep.hp <= 0) {
                continue;
            }
            int x = mapX + (int) Math.round(creep.x * scaleX);
            int y = mapY + (int) Math.round(creep.y * scaleY);
            g2.setColor(new Color(208, 186, 124, 190));
            g2.fillRect(x, y, 2, 2);
        }

        int px = mapX + (int) Math.round(model.player().x * scaleX);
        int py = mapY + (int) Math.round(model.player().y * scaleY);
        g2.setColor(new Color(255, 248, 180));
        g2.fillOval(px - 3, py - 3, 6, 6);

        int camX = mapX + (int) Math.round(model.cameraX() * scaleX);
        int camY = mapY + (int) Math.round(model.cameraY() * scaleY);
        int camW = (int) Math.max(2, Math.round((model.panelWidth() / model.zoom()) * scaleX));
        int camH = (int) Math.max(2, Math.round((model.panelHeight() / model.zoom()) * scaleY));
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRect(camX, camY, camW, camH);
    }

    private String hoverText(MapEditor.HoveredTile hoveredTile) {
        if (hoveredTile == null || !hoveredTile.inBounds()) {
            return "Тайл: вне карты";
        }
        return "Тайл: " + hoveredTile.x() + ", " + hoveredTile.y() + (hoveredTile.editable() ? "" : " (защищён)");
    }

    private void drawBar(Graphics2D g2,
                         int x,
                         int y,
                         int width,
                         int height,
                         double ratio,
                         Color fill,
                         Color bg,
                         Color border) {
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        g2.setColor(bg);
        g2.fillRoundRect(x, y, width, height, 8, 8);

        int fillW = (int) Math.round((width - 2) * ratio);
        g2.setColor(fill);
        g2.fillRoundRect(x + 1, y + 1, fillW, height - 2, 7, 7);

        g2.setColor(border);
        g2.drawRoundRect(x, y, width, height, 8, 8);
    }
}
