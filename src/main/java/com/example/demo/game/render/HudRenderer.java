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

    public record Model(int panelWidth,
                        int panelHeight,
                        double zoom,
                        Player player,
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

    public void draw(Graphics2D g2, Model model) {
        drawObjectivesPanel(g2, model);
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

    private void drawObjectivesPanel(Graphics2D g2, Model model) {
        drawPanel(g2, 16, 16, 278, 102);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Объекты", 30, 40);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));

        g2.drawString("Трон света: " + Math.max(0, model.lightThrone().hp), 30, 62);
        drawBar(g2, 30, 68, 236, 10,
                model.lightThrone().maxHp == 0 ? 0.0 : (double) model.lightThrone().hp / model.lightThrone().maxHp,
                new Color(90, 176, 255), new Color(32, 55, 83), new Color(190, 190, 190));

        g2.drawString("Трон тьмы: " + Math.max(0, model.darkThrone().hp), 30, 92);
        drawBar(g2, 30, 98, 236, 10,
                model.darkThrone().maxHp == 0 ? 0.0 : (double) model.darkThrone().hp / model.darkThrone().maxHp,
                new Color(237, 95, 88), new Color(83, 34, 31), new Color(190, 190, 190));
    }

    private void drawProgressPanel(Graphics2D g2, Model model) {
        int panelX = 16;
        int panelY = model.panelHeight() - 164;
        drawPanel(g2, panelX, panelY, 344, 148);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Прогресс", panelX + 14, panelY + 24);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));

        g2.drawString("Оружие: " + model.currentWeapon().displayName(), panelX + 14, panelY + 48);

        g2.drawString("XP: " + model.player().xp + " / " + model.player().xpToNextLevel, panelX + 14, panelY + 74);
        drawBar(g2, panelX + 14, panelY + 82, 314, 12,
                model.player().xpToNextLevel == 0 ? 0.0 : (double) model.player().xp / model.player().xpToNextLevel,
                new Color(91, 190, 255), new Color(31, 67, 93), new Color(190, 190, 190));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int abilityX = panelX + 14;
        int abilityY = panelY + 104;
        int abilityW = 98;
        int abilityH = 28;
        for (HeroAbility ability : model.abilities()) {
            drawAbilitySlot(g2, abilityX, abilityY, abilityW, abilityH, ability);
            abilityX += abilityW + 10;
        }
    }

    private void drawMetaPanel(Graphics2D g2, Model model) {
        int panelW = 278;
        int panelH = 118;
        int panelX = model.panelWidth() - panelW - 16;
        int panelY = model.panelHeight() - panelH - 16;
        drawPanel(g2, panelX, panelY, panelW, panelH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Сводка", panelX + 14, panelY + 24);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString("Крипы на линиях: " + model.laneCreepCount(), panelX + 14, panelY + 48);
        g2.drawString("Нейтралы: " + model.neutralCreepCount(), panelX + 14, panelY + 66);
        g2.drawString("Фраги героя: " + model.kills(), panelX + 14, panelY + 84);
        g2.drawString("FPS: " + model.currentFps() + " / " + model.targetFps(), panelX + 14, panelY + 102);

        g2.drawString("1 Камень  2 Лук  3 Меч", panelX + 132, panelY + 48);
        g2.drawString("WASD движение", panelX + 132, panelY + 66);
        g2.drawString("ЛКМ атака", panelX + 132, panelY + 84);
        g2.drawString("Q / E / R способности", panelX + 132, panelY + 102);
    }

    private void drawMiniMap(Graphics2D g2, Model model) {
        int panelX = model.panelWidth() - MINIMAP_SIZE - 16;
        int panelY = 16;
        int mapX = panelX + MINIMAP_PAD;
        int mapY = panelY + MINIMAP_PAD;
        int mapW = MINIMAP_SIZE - MINIMAP_PAD * 2;
        int mapH = MINIMAP_SIZE - MINIMAP_PAD * 2;

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
