package com.example.demo.game.render;

import com.example.demo.game.model.AnimationState;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

public class SpriteLibrary {
    private static final int PLAYER_W = 40;
    private static final int PLAYER_H = 40;
    private static final int ENEMY_W = 38;
    private static final int ENEMY_H = 38;

    private final EnumMap<AnimationState, BufferedImage[]> playerFrames;
    private final EnumMap<AnimationState, BufferedImage[]> enemyFrames;

    private SpriteLibrary(EnumMap<AnimationState, BufferedImage[]> playerFrames,
                          EnumMap<AnimationState, BufferedImage[]> enemyFrames) {
        this.playerFrames = playerFrames;
        this.enemyFrames = enemyFrames;
    }

    public static SpriteLibrary loadDefault() {
        EnumMap<AnimationState, BufferedImage[]> player = new EnumMap<>(AnimationState.class);
        EnumMap<AnimationState, BufferedImage[]> enemy = new EnumMap<>(AnimationState.class);

        player.put(AnimationState.IDLE,
                loadStripOrFallback("/sprites/player_idle.png", PLAYER_W, PLAYER_H, fallbackPlayer(AnimationState.IDLE, 4)));
        player.put(AnimationState.WALK,
                loadStripOrFallback("/sprites/player_walk.png", PLAYER_W, PLAYER_H, fallbackPlayer(AnimationState.WALK, 6)));
        player.put(AnimationState.ATTACK,
                loadStripOrFallback("/sprites/player_attack.png", PLAYER_W, PLAYER_H, fallbackPlayer(AnimationState.ATTACK, 4)));
        player.put(AnimationState.DEAD,
                loadStripOrFallback("/sprites/player_dead.png", PLAYER_W, PLAYER_H, fallbackPlayer(AnimationState.DEAD, 5)));

        enemy.put(AnimationState.IDLE,
                loadStripOrFallback("/sprites/enemy_idle.png", ENEMY_W, ENEMY_H, fallbackEnemy(AnimationState.IDLE, 4)));
        enemy.put(AnimationState.WALK,
                loadStripOrFallback("/sprites/enemy_walk.png", ENEMY_W, ENEMY_H, fallbackEnemy(AnimationState.WALK, 6)));
        enemy.put(AnimationState.ATTACK,
                loadStripOrFallback("/sprites/enemy_attack.png", ENEMY_W, ENEMY_H, fallbackEnemy(AnimationState.ATTACK, 4)));
        enemy.put(AnimationState.DEAD,
                loadStripOrFallback("/sprites/enemy_dead.png", ENEMY_W, ENEMY_H, fallbackEnemy(AnimationState.DEAD, 5)));

        return new SpriteLibrary(player, enemy);
    }

    public BufferedImage getPlayerFrame(AnimationState state, double phase) {
        return pickFrame(playerFrames.get(state), state, phase);
    }

    public BufferedImage getEnemyFrame(AnimationState state, double phase) {
        return pickFrame(enemyFrames.get(state), state, phase);
    }

    private static BufferedImage pickFrame(BufferedImage[] frames, AnimationState state, double phase) {
        if (frames == null || frames.length == 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        int index;
        if (state == AnimationState.DEAD || state == AnimationState.ATTACK) {
            index = Math.min((int) phase, frames.length - 1);
        } else {
            index = ((int) Math.floor(phase)) % frames.length;
            if (index < 0) {
                index += frames.length;
            }
        }
        return frames[index];
    }

    private static BufferedImage[] loadStripOrFallback(String resource,
                                                       int frameW,
                                                       int frameH,
                                                       BufferedImage[] fallback) {
        try (InputStream in = SpriteLibrary.class.getResourceAsStream(resource)) {
            if (in == null) {
                return fallback;
            }
            BufferedImage sheet = ImageIO.read(in);
            if (sheet == null || sheet.getWidth() < frameW || sheet.getHeight() < frameH) {
                return fallback;
            }

            int frames = sheet.getWidth() / frameW;
            if (frames <= 0) {
                return fallback;
            }

            BufferedImage[] result = new BufferedImage[frames];
            for (int i = 0; i < frames; i++) {
                result[i] = sheet.getSubimage(i * frameW, 0, frameW, frameH);
            }
            return result;
        } catch (IOException ignored) {
            return fallback;
        }
    }

    private static BufferedImage[] fallbackPlayer(AnimationState state, int frames) {
        BufferedImage[] out = new BufferedImage[frames];
        int[] swingTable = {0, 2, 1, -2, -1, 0};

        for (int i = 0; i < frames; i++) {
            BufferedImage image = new BufferedImage(PLAYER_W, PLAYER_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int swing = swingTable[i % swingTable.length];
            int bob = (state == AnimationState.IDLE) ? (int) Math.round(Math.sin(i * 0.8) * 1.2) : 0;

            if (state == AnimationState.DEAD) {
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillOval(7, 27, 24, 8);

                g2.setColor(new Color(52, 126, 226));
                g2.fillOval(8, 16, 24, 12);
                g2.setColor(new Color(30, 76, 152));
                g2.drawOval(8, 16, 24, 12);

                g2.setColor(new Color(241, 203, 164));
                g2.fillOval(26, 17, 8, 8);
                g2.setColor(new Color(120, 30, 30));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(12, 18, 20, 24);
                g2.drawLine(20, 18, 12, 24);
                g2.dispose();
                out[i] = image;
                continue;
            }

            g2.setColor(new Color(0, 0, 0, 75));
            g2.fillOval(10, 27, 20, 8);

            g2.setColor(new Color(43, 66, 104));
            int legOffset = (state == AnimationState.WALK) ? swing : 0;
            g2.fillRoundRect(13, 20 + legOffset + bob, 6, 10, 3, 3);
            g2.fillRoundRect(21, 20 - legOffset + bob, 6, 10, 3, 3);

            int torsoX = state == AnimationState.ATTACK ? 10 : 11;
            g2.setColor(new Color(52, 126, 226));
            g2.fillRoundRect(torsoX, 12 + bob, 18, 14, 6, 6);
            g2.setColor(new Color(30, 76, 152));
            g2.drawRoundRect(torsoX, 12 + bob, 18, 14, 6, 6);

            g2.setColor(new Color(241, 203, 164));
            g2.fillOval(13, 6 + bob, 14, 12);
            g2.setColor(new Color(201, 166, 132));
            g2.drawOval(13, 6 + bob, 14, 12);

            g2.setColor(new Color(35, 47, 66));
            g2.fillRect(15, 5 + bob, 10, 3);

            if (state == AnimationState.ATTACK && i == frames - 1) {
                g2.setColor(new Color(255, 220, 110, 220));
                g2.fillOval(31, 18 + bob, 6, 6);
            }

            g2.dispose();
            out[i] = image;
        }

        return out;
    }

    private static BufferedImage[] fallbackEnemy(AnimationState state, int frames) {
        BufferedImage[] out = new BufferedImage[frames];
        int[] swingTable = {0, 2, 1, -2, -1, 0};

        for (int i = 0; i < frames; i++) {
            BufferedImage image = new BufferedImage(ENEMY_W, ENEMY_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int swing = swingTable[i % swingTable.length];
            int bob = (state == AnimationState.IDLE) ? (int) Math.round(Math.sin(i * 0.7) * 1.0) : 0;

            if (state == AnimationState.DEAD) {
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillOval(9, 25, 20, 8);

                g2.setColor(new Color(185, 56, 46));
                g2.fillOval(8, 16, 22, 11);
                g2.setColor(new Color(120, 28, 23));
                g2.drawOval(8, 16, 22, 11);
                g2.setColor(new Color(255, 224, 170));
                g2.fillOval(25, 17, 7, 7);
                g2.dispose();
                out[i] = image;
                continue;
            }

            g2.setColor(new Color(0, 0, 0, 72));
            g2.fillOval(10, 26, 18, 7);

            g2.setColor(new Color(116, 26, 22));
            int legOffset = (state == AnimationState.WALK) ? swing : 0;
            g2.fillRoundRect(13, 19 + legOffset + bob, 4, 9, 3, 3);
            g2.fillRoundRect(21, 19 - legOffset + bob, 4, 9, 3, 3);

            int bodyW = state == AnimationState.ATTACK ? 22 : 20;
            int bodyX = state == AnimationState.ATTACK ? 8 : 9;
            g2.setColor(new Color(190, 56, 46));
            g2.fillOval(bodyX, 10 + bob, bodyW, 15);
            g2.setColor(new Color(120, 28, 23));
            g2.drawOval(bodyX, 10 + bob, bodyW, 15);

            g2.setColor(new Color(255, 224, 170));
            g2.fillOval(14, 14 + bob, 3, 3);
            g2.fillOval(20, 14 + bob, 3, 3);

            g2.setColor(new Color(140, 34, 28));
            Polygon horn1 = new Polygon(new int[]{13, 16, 14}, new int[]{12 + bob, 7 + bob, 11 + bob}, 3);
            Polygon horn2 = new Polygon(new int[]{24, 21, 23}, new int[]{12 + bob, 7 + bob, 11 + bob}, 3);
            g2.fillPolygon(horn1);
            g2.fillPolygon(horn2);

            if (state == AnimationState.ATTACK && i == frames - 1) {
                g2.setColor(new Color(255, 90, 66, 210));
                g2.fillOval(4, 16 + bob, 6, 6);
            }

            g2.dispose();
            out[i] = image;
        }

        return out;
    }
}
