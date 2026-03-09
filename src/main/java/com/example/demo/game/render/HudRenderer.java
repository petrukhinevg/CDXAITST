package com.example.demo.game.render;

import com.example.demo.game.model.Creep;
import com.example.demo.game.model.HeroAbility;
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
    private static final int MINIMAP_SIZE = 220;
    private static final int MINIMAP_PAD = 8;

    public record MiniMapBounds(int panelX, int panelY, int mapX, int mapY, int mapW, int mapH) {
    }

    public record Model(int panelWidth,
                        int panelHeight,
                        double zoom,
                        Player player,
                        List<Player> heroes,
                        WeaponType currentWeapon,
                        List<HeroAbility> abilities,
                        Structure lightThrone,
                        Structure darkThrone,
                        int laneCreepCount,
                        int neutralCreepCount,
                        int kills,
                        int currentFps,
                        int targetFps,
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

        int miniSize = Math.max(1, MINIMAP_SIZE - MINIMAP_PAD * 2);
        BufferedImage mini = new BufferedImage(miniSize, miniSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mini.createGraphics();
        g2.drawImage(mapLayer, 0, 0, miniSize, miniSize, null);
        g2.dispose();
        return mini;
    }

    public static MiniMapBounds miniMapBounds(int panelHeight) {
        int panelX = 16;
        int panelY = panelHeight - MINIMAP_SIZE - 16;
        int mapX = panelX + MINIMAP_PAD;
        int mapY = panelY + MINIMAP_PAD;
        int mapW = MINIMAP_SIZE - MINIMAP_PAD * 2;
        int mapH = MINIMAP_SIZE - MINIMAP_PAD * 2;
        return new MiniMapBounds(panelX, panelY, mapX, mapY, mapW, mapH);
    }

    public void draw(Graphics2D g2, Model model) {
        drawProgressPanel(g2, model);
        drawMetaPanel(g2, model);
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

    private void drawProgressPanel(Graphics2D g2, Model model) {
        int panelW = Math.min(560, Math.max(420, model.panelWidth() - 420));
        int panelH = 180;
        int panelX = (model.panelWidth() - panelW) / 2;
        int panelY = model.panelHeight() - panelH - 18;

        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        drawHudText(g2, "Прогресс", panelX, panelY + 18);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        drawHudText(g2, "Оружие: " + model.currentWeapon().displayName(), panelX, panelY + 44);
        drawHudText(g2, "HP: " + model.player().hp + " / " + model.player().maxHp, panelX, panelY + 70);
        drawBar(g2, panelX, panelY + 80, panelW, 13,
                model.player().maxHp == 0 ? 0.0 : (double) model.player().hp / model.player().maxHp,
                new Color(224, 88, 84), new Color(96, 32, 30), new Color(190, 190, 190));

        drawHudText(g2, "XP: " + model.player().xp + " / " + model.player().xpToNextLevel, panelX, panelY + 102);
        drawBar(g2, panelX, panelY + 112, panelW, 13,
                model.player().xpToNextLevel == 0 ? 0.0 : (double) model.player().xp / model.player().xpToNextLevel,
                new Color(91, 190, 255), new Color(31, 67, 93), new Color(190, 190, 190));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int slotGap = 10;
        int abilityY = panelY + 136;
        int abilityW = (panelW - slotGap * Math.max(0, model.abilities().size() - 1)) / Math.max(1, model.abilities().size());
        int abilityH = 28;
        int abilityX = panelX;
        for (HeroAbility ability : model.abilities()) {
            drawAbilitySlot(g2, abilityX, abilityY, abilityW, abilityH, ability);
            abilityX += abilityW + slotGap;
        }
    }

    private void drawMetaPanel(Graphics2D g2, Model model) {
        int panelX = 18;
        int panelY = 20;
        int columnGap = 42;
        int rightColumnX = panelX + 220 + columnGap;

        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        drawHudText(g2, "Сводка", panelX, panelY + 18);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        drawHudText(g2, "Крипы на линиях: " + model.laneCreepCount(), panelX, panelY + 44);
        drawHudText(g2, "Нейтралы: " + model.neutralCreepCount(), panelX, panelY + 66);
        drawHudText(g2, "Фраги героя: " + model.kills(), panelX, panelY + 88);
        drawHudText(g2, "FPS: " + model.currentFps() + " / " + model.targetFps(), panelX, panelY + 110);

        drawHudText(g2, "1 Камень  2 Лук  3 Меч", rightColumnX, panelY + 44);
        drawHudText(g2, "WASD движение", rightColumnX, panelY + 66);
        drawHudText(g2, "ЛКМ атака", rightColumnX, panelY + 88);
        drawHudText(g2, "Q / E / R способности", rightColumnX, panelY + 110);
    }

    private void drawMiniMap(Graphics2D g2, Model model) {
        MiniMapBounds bounds = miniMapBounds(model.panelHeight());
        int panelX = bounds.panelX();
        int panelY = bounds.panelY();
        int mapX = bounds.mapX();
        int mapY = bounds.mapY();
        int mapW = bounds.mapW();
        int mapH = bounds.mapH();

        g2.setColor(new Color(0, 0, 0, 165));
        g2.fillRoundRect(panelX, panelY, MINIMAP_SIZE, MINIMAP_SIZE, 10, 10);
        g2.setColor(new Color(235, 235, 235, 180));
        g2.drawRoundRect(panelX, panelY, MINIMAP_SIZE, MINIMAP_SIZE, 10, 10);

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

        for (Player hero : model.heroes()) {
            if (hero.hp <= 0) {
                continue;
            }
            int px = mapX + (int) Math.round(hero.x * scaleX);
            int py = mapY + (int) Math.round(hero.y * scaleY);
            g2.setColor(hero == model.player()
                    ? new Color(255, 248, 180)
                    : hero.team == Team.LIGHT ? new Color(164, 222, 255) : new Color(255, 140, 132));
            g2.fillOval(px - 3, py - 3, 6, 6);
        }

        int camX = mapX + (int) Math.round(model.cameraX() * scaleX);
        int camY = mapY + (int) Math.round(model.cameraY() * scaleY);
        int camW = (int) Math.max(2, Math.round((model.panelWidth() / model.zoom()) * scaleX));
        int camH = (int) Math.max(2, Math.round((model.panelHeight() / model.zoom()) * scaleY));
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRect(camX, camY, camW, camH);
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

    private void drawPanel(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(x, y, width, height, 12, 12);
        g2.setColor(new Color(230, 230, 230, 120));
        g2.drawRoundRect(x, y, width, height, 12, 12);
    }

    private void drawHudText(Graphics2D g2, String text, int x, int y) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }

    private void drawAbilitySlot(Graphics2D g2, int x, int y, int width, int height, HeroAbility ability) {
        Color fill = ability.ultimate()
                ? new Color(130, 82, 40, 205)
                : new Color(38, 70, 88, 205);
        if (!ability.isReady()) {
            fill = ability.ultimate()
                    ? new Color(78, 54, 30, 205)
                    : new Color(28, 42, 54, 205);
        }

        g2.setColor(fill);
        g2.fillRoundRect(x, y, width, height, 10, 10);
        g2.setColor(new Color(225, 225, 225, 130));
        g2.drawRoundRect(x, y, width, height, 10, 10);

        g2.setColor(Color.WHITE);
        g2.drawString(ability.slot().keyLabel(), x + 8, y + 18);

        String name = ability.displayName();
        int maxLen = 11;
        if (name.length() > maxLen) {
            name = name.substring(0, maxLen - 1) + "…";
        }
        g2.drawString(name, x + 28, y + 18);

        String stateText = ability.isReady()
                ? "готово"
                : String.format("%.0fs", Math.ceil(ability.remainingCooldown()));
        int stateW = g2.getFontMetrics().stringWidth(stateText);
        g2.drawString(stateText, x + width - stateW - 8, y + 18);
    }
}
