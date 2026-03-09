package com.example.demo.game;

import com.example.demo.game.config.GameConfig;
import com.example.demo.game.model.AnimationState;
import com.example.demo.game.model.Bullet;
import com.example.demo.game.model.CombatEntity;
import com.example.demo.game.model.CombatUnit;
import com.example.demo.game.model.Creep;
import com.example.demo.game.model.CreepRole;
import com.example.demo.game.model.ExperienceOrb;
import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Player;
import com.example.demo.game.model.Structure;
import com.example.demo.game.model.StructureType;
import com.example.demo.game.model.Team;
import com.example.demo.game.model.WeaponType;
import com.example.demo.game.render.HudRenderer;
import com.example.demo.game.render.MapRenderer;
import com.example.demo.game.render.SpriteLibrary;
import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.MapGenerator;
import com.example.demo.game.world.MapLayout;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener {
    private static final double ZOOM = 2.0;

    private final Random random = new Random();

    private final GameMap map = new GameMap();
    private final MapGenerator mapGenerator = new MapGenerator();
    private final HudRenderer hudRenderer = new HudRenderer();
    private final MapRenderer mapRenderer = new MapRenderer();
    private final SpriteLibrary sprites = SpriteLibrary.loadDefault();

    private final Player player = new Player();
    private final Team heroTeam = Team.LIGHT;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<ExperienceOrb> experienceOrbs = new ArrayList<>();
    private final List<Creep> laneCreeps = new ArrayList<>();
    private final List<Creep> neutralCreeps = new ArrayList<>();
    private final List<Structure> structures = new ArrayList<>();

    private final EnumMap<Team, EnumMap<LaneType, List<Point>>> lanePaths = new EnumMap<>(Team.class);

    private Structure lightThrone;
    private Structure darkThrone;

    private BufferedImage mapLayer;
    private BufferedImage miniMapLayer;

    private WeaponType currentWeapon = WeaponType.STONE;
    private int ammoInMagazine;
    private double reloadTimer;

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean attackMouseDown;
    private int mouseX;
    private int mouseY;

    private double cameraX;
    private double cameraY;

    private double attackCooldown;
    private double playerHitCooldown;
    private double muzzleFlashTime;
    private double swordSwingTime;
    private double laneWaveTimer;

    private int kills;
    private boolean gameOver;
    private String victoryText = "";

    private final int targetFps = detectTargetFps();
    private final Timer gameTimer;
    private long lastTickNanos;

    private int currentFps;
    private int framesThisSecond;
    private long fpsWindowStartNanos;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.VIEW_W, GameConfig.VIEW_H));
        setBackground(new Color(16, 32, 18));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        resetGame();

        int delayMs = Math.max(1, (int) Math.round(1000.0 / targetFps));
        gameTimer = new Timer(delayMs, e -> tick());
        gameTimer.setCoalesce(true);
        gameTimer.start();
    }

    private void resetGame() {
        regenerateMap();

        bullets.clear();
        experienceOrbs.clear();
        laneCreeps.clear();
        neutralCreeps.clear();
        structures.clear();

        initLanePathCache();
        initStructures();
        initNeutralCreeps();

        player.x = map.tileCenter(GameConfig.PLAYER_START_TILE_X);
        player.y = map.tileCenter(GameConfig.PLAYER_START_TILE_Y);
        player.team = heroTeam;
        player.maxHp = 120;
        player.hp = player.maxHp;
        player.defense = 2;
        player.level = 1;
        player.xp = 0;
        player.xpToNextLevel = 50;
        player.animPhase = 0.0;
        player.attackTimer = 0.0;
        player.attackAnimationTimer = 0.0;
        player.state = AnimationState.IDLE;

        currentWeapon = WeaponType.STONE;
        ammoInMagazine = currentWeapon.magazineSize();
        reloadTimer = 0.0;

        attackCooldown = 0.0;
        playerHitCooldown = 0.0;
        muzzleFlashTime = 0.0;
        swordSwingTime = 0.0;
        laneWaveTimer = 15.0;

        kills = 0;
        gameOver = false;
        victoryText = "";
        attackMouseDown = false;

        spawnLaneWave();

        centerCameraOnPlayer();
        lastTickNanos = System.nanoTime();
        fpsWindowStartNanos = lastTickNanos;
        framesThisSecond = 0;
        currentFps = 0;
        requestFocusInWindow();
    }

    private void regenerateMap() {
        mapGenerator.generate(map, random);
        rebuildMapLayers();
    }

    private void rebuildMapLayers() {
        mapLayer = mapRenderer.buildLayer(map);
        miniMapLayer = buildMiniMapLayer();
    }

    private void initLanePathCache() {
        lanePaths.clear();

        for (Team team : List.of(Team.LIGHT, Team.DARK)) {
            EnumMap<LaneType, List<Point>> teamPaths = new EnumMap<>(LaneType.class);
            for (LaneType lane : LaneType.values()) {
                teamPaths.put(lane, MapLayout.laneTilesForTeam(lane, team));
            }
            lanePaths.put(team, teamPaths);
        }
    }

    private void initStructures() {
        lightThrone = createThrone(Team.LIGHT, MapLayout.LIGHT_THRONE_TILE);
        darkThrone = createThrone(Team.DARK, MapLayout.DARK_THRONE_TILE);
        structures.add(lightThrone);
        structures.add(darkThrone);

        Map<LaneType, List<Point>> lightTowers = MapLayout.towerTilesForTeam(Team.LIGHT);
        Map<LaneType, List<Point>> darkTowers = MapLayout.towerTilesForTeam(Team.DARK);

        for (LaneType lane : LaneType.values()) {
            List<Point> lt = lightTowers.get(lane);
            for (int i = 0; i < lt.size(); i++) {
                structures.add(createTower(Team.LIGHT, lane, i, lt.get(i)));
            }

            List<Point> dt = darkTowers.get(lane);
            for (int i = 0; i < dt.size(); i++) {
                structures.add(createTower(Team.DARK, lane, i, dt.get(i)));
            }
        }
    }

    private Structure createTower(Team team, LaneType lane, int laneOrder, Point tile) {
        Structure s = new Structure();
        s.team = team;
        s.type = StructureType.TOWER;
        s.lane = lane;
        s.laneOrder = laneOrder;
        s.x = map.tileCenter(tile.x);
        s.y = map.tileCenter(tile.y);
        s.radius = 18;
        s.maxHp = 360;
        s.hp = s.maxHp;
        s.damage = 14;
        s.defense = 3;
        s.attackRange = 185;
        s.attackCooldown = 1.1;
        return s;
    }

    private Structure createThrone(Team team, Point tile) {
        Structure s = new Structure();
        s.team = team;
        s.type = StructureType.THRONE;
        s.lane = null;
        s.laneOrder = 999;
        s.x = map.tileCenter(tile.x);
        s.y = map.tileCenter(tile.y);
        s.radius = 26;
        s.maxHp = 1300;
        s.hp = s.maxHp;
        s.damage = 18;
        s.defense = 6;
        s.attackRange = 230;
        s.attackCooldown = 1.0;
        return s;
    }

    private void initNeutralCreeps() {
        for (Point camp : MapLayout.neutralCampTiles()) {
            for (int i = 0; i < 2; i++) {
                Creep c = new Creep();
                c.team = Team.NEUTRAL;
                c.role = CreepRole.NEUTRAL;
                c.lane = null;
                c.x = map.tileCenter(camp.x) + random.nextDouble() * 26.0 - 13.0;
                c.y = map.tileCenter(camp.y) + random.nextDouble() * 26.0 - 13.0;
                c.radius = 11;
                c.maxHp = 90;
                c.hp = c.maxHp;
                c.damage = 8;
                c.defense = 1;
                c.moveSpeed = 0;
                c.attackRange = 34;
                c.attackCooldown = 1.1;
                c.attackTimer = random.nextDouble() * 0.8;
                c.lookAngle = random.nextDouble() * Math.PI * 2.0;
                neutralCreeps.add(c);
            }
        }
    }

    private void spawnLaneWave() {
        for (LaneType lane : LaneType.values()) {
            for (Team team : List.of(Team.LIGHT, Team.DARK)) {
                for (int i = 0; i < 3; i++) {
                    spawnLaneCreep(team, lane, i);
                }
            }
        }
    }

    private void spawnLaneCreep(Team team, LaneType lane, int idx) {
        List<Point> path = lanePaths.get(team).get(lane);
        if (path.size() < 2) {
            return;
        }

        Point start = path.get(0);
        Point next = path.get(1);

        double dirX = next.x - start.x;
        double dirY = next.y - start.y;
        double len = Math.hypot(dirX, dirY);
        if (len < 0.001) {
            len = 1.0;
        }
        dirX /= len;
        dirY /= len;

        double perpX = -dirY;
        double perpY = dirX;
        double shift = (idx - 1) * 2.2;

        Creep creep = new Creep();
        creep.team = team;
        creep.role = CreepRole.LANE;
        creep.lane = lane;
        creep.x = map.tileCenter(start.x) + perpX * shift * map.getTileSize();
        creep.y = map.tileCenter(start.y) + perpY * shift * map.getTileSize();
        creep.radius = 9.5;
        creep.maxHp = 58;
        creep.hp = creep.maxHp;
        creep.damage = 7;
        creep.defense = 1;
        creep.moveSpeed = 66;
        creep.attackRange = 31;
        creep.attackCooldown = 0.82;
        creep.attackTimer = 0.12 * idx;
        creep.waypointIndex = 1;
        creep.animPhase = random.nextDouble() * 3.0;
        creep.lookAngle = Math.atan2(next.y - start.y, next.x - start.x);
        laneCreeps.add(creep);
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastTickNanos) / 1_000_000_000.0, 0.033);
        lastTickNanos = now;

        updateWorld(dt);
        repaint();

        framesThisSecond++;
        if (now - fpsWindowStartNanos >= 1_000_000_000L) {
            currentFps = framesThisSecond;
            framesThisSecond = 0;
            fpsWindowStartNanos = now;
        }
    }

    private void updateWorld(double dt) {
        attackCooldown = Math.max(0.0, attackCooldown - dt);
        playerHitCooldown = Math.max(0.0, playerHitCooldown - dt);
        muzzleFlashTime = Math.max(0.0, muzzleFlashTime - dt);
        swordSwingTime = Math.max(0.0, swordSwingTime - dt);

        player.attackTimer = Math.max(0.0, player.attackTimer - dt);
        player.attackAnimationTimer = Math.max(0.0, player.attackAnimationTimer - dt);

        if (reloadTimer > 0.0) {
            reloadTimer = Math.max(0.0, reloadTimer - dt);
            if (reloadTimer == 0.0) {
                ammoInMagazine = currentWeapon.magazineSize();
            }
        }

        laneWaveTimer -= dt;
        if (!gameOver && laneWaveTimer <= 0.0) {
            spawnLaneWave();
            laneWaveTimer = 15.0;
        }

        double worldMouseX = screenToWorldX(mouseX);
        double worldMouseY = screenToWorldY(mouseY);
        player.aimAngle = Math.atan2(worldMouseY - player.y, worldMouseX - player.x);

        boolean moved = false;
        if (!gameOver) {
            moved = movePlayer(dt);
            if (attackMouseDown && attackCooldown <= 0.0) {
                attackWithCurrentWeapon();
            }
        }

        updatePlayerAnimation(dt, moved);
        updateBullets(dt);
        updateLaneCreeps(dt);
        updateNeutralCreeps(dt);
        resolveUnitCollisions();
        updateStructures(dt);
        updateExperienceOrbs(dt);

        if (!gameOver && player.hp <= 0) {
            gameOver = true;
            victoryText = "Силы тьмы победили";
            player.state = AnimationState.DEAD;
            player.animPhase = 0.0;
        }

        checkVictory();
        centerCameraOnPlayer();
    }

    private void updatePlayerAnimation(double dt, boolean moved) {
        player.moving = moved;

        if (gameOver) {
            player.state = AnimationState.DEAD;
            player.animPhase += dt * 7.0;
            return;
        }

        if (player.attackAnimationTimer > 0.0) {
            player.state = AnimationState.ATTACK;
            player.animPhase += dt * 16.0;
            return;
        }

        if (moved) {
            player.state = AnimationState.WALK;
            player.animPhase += dt * 9.0;
        } else {
            player.state = AnimationState.IDLE;
            player.animPhase += dt * 4.0;
        }
    }

    private boolean movePlayer(double dt) {
        double inputX = 0.0;
        double inputY = 0.0;
        if (up) inputY -= 1.0;
        if (down) inputY += 1.0;
        if (left) inputX -= 1.0;
        if (right) inputX += 1.0;

        if (inputX == 0.0 && inputY == 0.0) {
            return false;
        }

        double len = Math.hypot(inputX, inputY);
        inputX /= len;
        inputY /= len;

        double speed = 215.0;
        return moveCombatUnit(player, inputX * speed * dt, inputY * speed * dt);
    }

    private void attackWithCurrentWeapon() {
        if (reloadTimer > 0.0) {
            return;
        }

        if (ammoInMagazine <= 0) {
            startReload();
            return;
        }

        ammoInMagazine--;
        player.attackTimer = currentWeapon.attackAnimationTime();
        player.attackAnimationTimer = currentWeapon.attackAnimationTime();
        player.animPhase = 0.0;
        attackCooldown = currentWeapon.cooldown();

        if (currentWeapon.projectile()) {
            fireProjectile(currentWeapon);
            muzzleFlashTime = 0.06;
        } else {
            performMeleeAttack(currentWeapon);
            swordSwingTime = 0.10;
        }

        if (ammoInMagazine <= 0) {
            startReload();
        }
    }

    private void fireProjectile(WeaponType weapon) {
        double dx = Math.cos(player.aimAngle);
        double dy = Math.sin(player.aimAngle);

        Bullet bullet = new Bullet();
        bullet.x = player.x + dx * (player.radius + 9);
        bullet.y = player.y + dy * (player.radius + 9);
        bullet.vx = dx * weapon.projectileSpeed();
        bullet.vy = dy * weapon.projectileSpeed();
        bullet.radius = weapon.projectileRadius();
        bullet.life = weapon.projectileLife();
        bullet.damage = weapon.damage();
        bullet.colorArgb = weapon.projectileColorArgb();
        bullets.add(bullet);
    }

    private void performMeleeAttack(WeaponType weapon) {
        double arcHalf = Math.toRadians(weapon.meleeArcDegrees() / 2.0);

        for (Creep creep : laneCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, weapon.meleeRange() + creep.radius, arcHalf)) {
                damageCreep(creep, weapon.damage());
            }
        }

        for (Creep creep : neutralCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, weapon.meleeRange() + creep.radius, arcHalf)) {
                damageCreep(creep, weapon.damage());
            }
        }

        for (Structure structure : structures) {
            if (isHostileStructure(structure) && structure.hp > 0
                    && inMeleeArc(structure.x, structure.y, weapon.meleeRange() + structure.radius, arcHalf)) {
                damageStructure(structure, weapon.damage());
            }
        }
    }

    private boolean inMeleeArc(double tx, double ty, double range, double arcHalf) {
        double dx = tx - player.x;
        double dy = ty - player.y;
        double dist = Math.hypot(dx, dy);
        if (dist > range) {
            return false;
        }

        double angle = Math.atan2(dy, dx);
        double diff = Math.abs(normalizeAngle(angle - player.aimAngle));
        return diff <= arcHalf;
    }

    private void updateBullets(double dt) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.x += bullet.vx * dt;
            bullet.y += bullet.vy * dt;
            bullet.life -= dt;

            if (bullet.life <= 0.0 || map.isBlockedPixel(bullet.x, bullet.y)) {
                it.remove();
                continue;
            }

            boolean hit = false;
            for (Creep creep : laneCreeps) {
                if (!isHostileCreep(creep) || creep.hp <= 0) {
                    continue;
                }
                if (distance(bullet.x, bullet.y, creep.x, creep.y) <= bullet.radius + creep.radius) {
                    damageCreep(creep, bullet.damage);
                    hit = true;
                    break;
                }
            }

            if (!hit) {
                for (Creep creep : neutralCreeps) {
                    if (!isHostileCreep(creep) || creep.hp <= 0) {
                        continue;
                    }
                    if (distance(bullet.x, bullet.y, creep.x, creep.y) <= bullet.radius + creep.radius) {
                        damageCreep(creep, bullet.damage);
                        hit = true;
                        break;
                    }
                }
            }

            if (!hit) {
                for (Structure structure : structures) {
                    if (!isHostileStructure(structure) || structure.hp <= 0) {
                        continue;
                    }
                    if (distance(bullet.x, bullet.y, structure.x, structure.y) <= bullet.radius + structure.radius) {
                        damageStructure(structure, bullet.damage);
                        hit = true;
                        break;
                    }
                }
            }

            if (hit) {
                it.remove();
            }
        }
    }

    private void updateLaneCreeps(double dt) {
        Iterator<Creep> it = laneCreeps.iterator();
        while (it.hasNext()) {
            Creep creep = it.next();

            if (creep.hp <= 0) {
                spawnExperienceOrbs(creep.x, creep.y, 8 + random.nextInt(4));
                it.remove();
                continue;
            }

            creep.attackTimer = Math.max(0.0, creep.attackTimer - dt);
            creep.attackAnimationTimer = Math.max(0.0, creep.attackAnimationTimer - dt);

            Creep enemyCreep = findNearestEnemyLaneCreep(creep, 130.0);
            if (enemyCreep != null) {
                engageCreepTarget(creep, enemyCreep, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            if (shouldLaneCreepAttackHero(creep)) {
                engageHeroTarget(creep, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            Structure enemyStructure = findLaneTargetStructure(creep);
            if (enemyStructure != null && enemyStructure.hp > 0 && distance(creep.x, creep.y, enemyStructure.x, enemyStructure.y) < 180.0) {
                engageStructureTarget(creep, enemyStructure, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            moveAlongLane(creep, dt);
            updateCreepAnimation(creep, dt);
        }
    }

    private void engageCreepTarget(Creep creep, Creep target, double dt) {
        double dist = distance(creep.x, creep.y, target.x, target.y);
        if (dist > creep.attackRange + target.radius) {
            moveTowards(creep, target.x, target.y, creep.moveSpeed * dt);
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageCreep(target, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, target.x, target.y);
        }
    }

    private void engageStructureTarget(Creep creep, Structure target, double dt) {
        double dist = distance(creep.x, creep.y, target.x, target.y);
        if (dist > creep.attackRange + target.radius) {
            moveTowards(creep, target.x, target.y, creep.moveSpeed * dt);
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageStructure(target, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, target.x, target.y);
        }
    }

    private void engageHeroTarget(Creep creep, double dt) {
        double dist = distance(creep.x, creep.y, player.x, player.y);
        if (dist > creep.attackRange + player.radius) {
            moveTowards(creep, player.x, player.y, creep.moveSpeed * dt);
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damagePlayer(creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, player.x, player.y);
        }
    }

    private void moveAlongLane(Creep creep, double dt) {
        List<Point> path = lanePaths.get(creep.team).get(creep.lane);
        if (path == null || path.isEmpty()) {
            return;
        }

        if (creep.waypointIndex >= path.size()) {
            creep.waypointIndex = path.size() - 1;
        }

        Point targetPoint = path.get(creep.waypointIndex);
        double tx = map.tileCenter(targetPoint.x);
        double ty = map.tileCenter(targetPoint.y);

        if (distance(creep.x, creep.y, tx, ty) < 10.0 && creep.waypointIndex < path.size() - 1) {
            creep.waypointIndex++;
            targetPoint = path.get(creep.waypointIndex);
            tx = map.tileCenter(targetPoint.x);
            ty = map.tileCenter(targetPoint.y);
        }

        moveTowards(creep, tx, ty, creep.moveSpeed * dt);
    }

    private void moveTowards(Creep creep, double tx, double ty, double distanceStep) {
        double dx = tx - creep.x;
        double dy = ty - creep.y;
        double len = Math.hypot(dx, dy);
        if (len < 0.0001) {
            creep.moving = false;
            return;
        }

        double step = Math.min(distanceStep, len);
        double dirX = dx / len;
        double dirY = dy / len;
        creep.lookAngle = Math.atan2(dirY, dirX);
        creep.moving = moveCombatUnit(creep, dirX * step, dirY * step);
    }

    private void updateNeutralCreeps(double dt) {
        Iterator<Creep> it = neutralCreeps.iterator();
        while (it.hasNext()) {
            Creep creep = it.next();

            if (creep.hp <= 0) {
                spawnExperienceOrbs(creep.x, creep.y, 14 + random.nextInt(6));
                it.remove();
                continue;
            }

            creep.attackTimer = Math.max(0.0, creep.attackTimer - dt);
            creep.attackAnimationTimer = Math.max(0.0, creep.attackAnimationTimer - dt);

            Creep targetLaneCreep = findNearestLaneCreepAround(creep.x, creep.y, 92.0);
            double heroDist = gameOver ? Double.MAX_VALUE : distance(creep.x, creep.y, player.x, player.y);

            if (targetLaneCreep != null && distance(creep.x, creep.y, targetLaneCreep.x, targetLaneCreep.y) <= creep.attackRange + targetLaneCreep.radius) {
                if (creep.attackTimer <= 0.0) {
                    damageCreep(targetLaneCreep, creep.damage);
                    creep.attackTimer = creep.attackCooldown;
                    triggerUnitAttackAnimation(creep, targetLaneCreep.x, targetLaneCreep.y);
                }
            } else if (heroDist <= creep.attackRange + player.radius) {
                if (creep.attackTimer <= 0.0) {
                    damagePlayer(creep.damage);
                    creep.attackTimer = creep.attackCooldown;
                    triggerUnitAttackAnimation(creep, player.x, player.y);
                }
            }

            updateCreepAnimation(creep, dt);
        }
    }

    private void updateStructures(double dt) {
        for (Structure structure : structures) {
            if (structure.hp <= 0) {
                continue;
            }

            structure.attackTimer = Math.max(0.0, structure.attackTimer - dt);
            if (structure.attackTimer > 0.0) {
                continue;
            }

            Team enemyTeam = structure.team.opposite();
            List<Creep> candidates = new ArrayList<>();
            for (Creep creep : laneCreeps) {
                if (creep.hp <= 0 || creep.team != enemyTeam) {
                    continue;
                }
                if (distance(structure.x, structure.y, creep.x, creep.y) <= structure.attackRange) {
                    candidates.add(creep);
                }
            }

            boolean heroInRange = !gameOver
                    && enemyTeam == heroTeam
                    && distance(structure.x, structure.y, player.x, player.y) <= structure.attackRange;

            int totalTargets = candidates.size() + (heroInRange ? 1 : 0);
            if (totalTargets == 0) {
                continue;
            }

            int picked = random.nextInt(totalTargets);
            if (picked < candidates.size()) {
                damageCreep(candidates.get(picked), structure.damage);
            } else {
                damagePlayer(structure.damage);
            }

            structure.attackTimer = structure.attackCooldown;
        }
    }

    private void resolveUnitCollisions() {
        List<CombatUnit> units = new ArrayList<>();
        if (!gameOver && player.hp > 0) {
            units.add(player);
        }
        addLivingUnits(units, laneCreeps);
        addLivingUnits(units, neutralCreeps);

        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < units.size(); i++) {
                for (int j = i + 1; j < units.size(); j++) {
                    separateUnits(units.get(i), units.get(j));
                }
            }
        }
    }

    private void addLivingUnits(List<CombatUnit> units, List<Creep> creeps) {
        for (Creep creep : creeps) {
            if (creep.hp > 0) {
                units.add(creep);
            }
        }
    }

    private void separateUnits(CombatUnit a, CombatUnit b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dist = Math.hypot(dx, dy);
        double minDist = a.getRadius() + b.getRadius();
        if (dist >= minDist) {
            return;
        }

        if (dist < 0.0001) {
            dx = 1.0;
            dy = 0.0;
            dist = 1.0;
        }

        double overlap = minDist - dist;
        double nx = dx / dist;
        double ny = dy / dist;

        boolean movedA = tryMoveCombatUnit(a, a.getX() - nx * overlap * 0.5, a.getY() - ny * overlap * 0.5);
        boolean movedB = tryMoveCombatUnit(b, b.getX() + nx * overlap * 0.5, b.getY() + ny * overlap * 0.5);

        if (!movedA) {
            tryMoveCombatUnit(b, b.getX() + nx * overlap, b.getY() + ny * overlap);
        }
        if (!movedB) {
            tryMoveCombatUnit(a, a.getX() - nx * overlap, a.getY() - ny * overlap);
        }
    }

    private void triggerUnitAttackAnimation(CombatUnit unit, double targetX, double targetY) {
        unit.setLookAngle(Math.atan2(targetY - unit.getY(), targetX - unit.getX()));
        unit.setAttackAnimationTimer(0.18);
        unit.setAnimPhase(0.0);
    }

    private void updateCreepAnimation(Creep creep, double dt) {
        if (creep.attackAnimationTimer > 0.0) {
            creep.state = AnimationState.ATTACK;
            creep.animPhase += dt * 18.0;
        } else if (creep.moving) {
            creep.state = AnimationState.WALK;
            creep.animPhase += dt * 8.0;
        } else {
            creep.state = AnimationState.IDLE;
            creep.animPhase += dt * 4.5;
        }
    }

    private boolean shouldLaneCreepAttackHero(Creep creep) {
        return !gameOver
                && creep.team != heroTeam
                && distance(creep.x, creep.y, player.x, player.y) <= 140.0
                && findNearestLaneCreepByTeam(player.x, player.y, 96.0, heroTeam) == null;
    }

    private void damageCreep(Creep creep, int damage) {
        damageEntity(creep, damage);
    }

    private void damageStructure(Structure structure, int damage) {
        damageEntity(structure, damage);
    }

    private void damagePlayer(int damage) {
        if (playerHitCooldown > 0.0) {
            return;
        }
        playerHitCooldown = 0.35;
        damageEntity(player, damage);
    }

    private void damageEntity(CombatEntity entity, int damage) {
        entity.applyDamage(damage);
    }

    private void spawnExperienceOrbs(double x, double y, int totalXp) {
        int orbCount = 2 + random.nextInt(2);

        for (int i = 0; i < orbCount; i++) {
            ExperienceOrb orb = new ExperienceOrb();
            orb.x = x + random.nextDouble() * 14.0 - 7.0;
            orb.y = y + random.nextDouble() * 14.0 - 7.0;
            orb.radius = 3.8 + random.nextDouble() * 1.2;
            orb.value = Math.max(1, totalXp / orbCount + random.nextInt(2));
            orb.phase = random.nextDouble() * Math.PI * 2.0;
            experienceOrbs.add(orb);
        }
    }

    private void updateExperienceOrbs(double dt) {
        Iterator<ExperienceOrb> it = experienceOrbs.iterator();
        while (it.hasNext()) {
            ExperienceOrb orb = it.next();
            orb.phase += dt * 6.0;

            if (gameOver) {
                continue;
            }

            double dx = player.x - orb.x;
            double dy = player.y - orb.y;
            double dist = Math.hypot(dx, dy);

            double magnetRadius = 140.0;
            if (dist < magnetRadius && dist > 0.001) {
                double factor = 1.0 - dist / magnetRadius;
                double speed = 40.0 + factor * 210.0;
                orb.x += dx / dist * speed * dt;
                orb.y += dy / dist * speed * dt;
            }

            if (dist <= 38.0) {
                gainExperience(orb.value);
                it.remove();
            }
        }
    }

    private void gainExperience(int value) {
        player.xp += value;
        while (player.xp >= player.xpToNextLevel) {
            player.xp -= player.xpToNextLevel;
            player.level += 1;
            player.xpToNextLevel = (int) Math.round(player.xpToNextLevel * 1.33 + 12);
            player.maxHp += 10;
            player.hp = Math.min(player.maxHp, player.hp + 18);
        }
    }

    private void checkVictory() {
        if (gameOver) {
            return;
        }

        if (lightThrone.hp <= 0) {
            gameOver = true;
            victoryText = "Силы тьмы победили";
            player.state = AnimationState.DEAD;
        } else if (darkThrone.hp <= 0) {
            gameOver = true;
            victoryText = "Силы света победили";
        }
    }

    private Creep findNearestEnemyLaneCreep(Creep source, double radius) {
        double best = Double.MAX_VALUE;
        Creep result = null;

        for (Creep creep : laneCreeps) {
            if (creep == source || creep.hp <= 0 || creep.team == source.team || creep.role != CreepRole.LANE) {
                continue;
            }
            if (creep.lane != source.lane) {
                continue;
            }

            double dist = distance(source.x, source.y, creep.x, creep.y);
            if (dist < radius && dist < best) {
                best = dist;
                result = creep;
            }
        }

        return result;
    }

    private Structure findLaneTargetStructure(Creep creep) {
        Team enemyTeam = creep.team.opposite();

        Structure laneTower = structures.stream()
                .filter(s -> s.hp > 0)
                .filter(s -> s.team == enemyTeam)
                .filter(s -> s.type == StructureType.TOWER && s.lane == creep.lane)
                .max(Comparator.comparingInt(s -> s.laneOrder))
                .orElse(null);

        if (laneTower != null) {
            return laneTower;
        }

        return structures.stream()
                .filter(s -> s.hp > 0)
                .filter(s -> s.team == enemyTeam)
                .filter(s -> s.type == StructureType.THRONE)
                .findFirst()
                .orElse(null);
    }

    private Creep findNearestLaneCreepAround(double x, double y, double radius) {
        Creep bestCreep = null;
        double best = Double.MAX_VALUE;

        for (Creep creep : laneCreeps) {
            if (creep.hp <= 0) {
                continue;
            }

            double dist = distance(x, y, creep.x, creep.y);
            if (dist < radius && dist < best) {
                best = dist;
                bestCreep = creep;
            }
        }

        return bestCreep;
    }

    private Creep findNearestLaneCreepByTeam(double x, double y, double radius, Team team) {
        Creep bestCreep = null;
        double best = Double.MAX_VALUE;

        for (Creep creep : laneCreeps) {
            if (creep.hp <= 0 || creep.team != team) {
                continue;
            }

            double dist = distance(x, y, creep.x, creep.y);
            if (dist < radius && dist < best) {
                best = dist;
                bestCreep = creep;
            }
        }

        return bestCreep;
    }

    private boolean isHostileCreep(Creep creep) {
        return creep.hp > 0 && (creep.team == Team.DARK || creep.team == Team.NEUTRAL);
    }

    private boolean isHostileStructure(Structure structure) {
        return structure.hp > 0 && structure.team == Team.DARK;
    }

    private void startReload() {
        if (reloadTimer > 0.0 || ammoInMagazine == currentWeapon.magazineSize()) {
            return;
        }
        reloadTimer = currentWeapon.reloadSeconds();
    }

    private void switchWeapon(WeaponType weapon) {
        if (currentWeapon == weapon) {
            return;
        }

        currentWeapon = weapon;
        ammoInMagazine = currentWeapon.magazineSize();
        reloadTimer = 0.0;
        attackCooldown = Math.min(attackCooldown, 0.12);
    }

    private boolean moveCombatUnit(CombatUnit unit, double deltaX, double deltaY) {
        double oldX = unit.getX();
        double oldY = unit.getY();
        tryMoveCombatUnit(unit, unit.getX() + deltaX, unit.getY() + deltaY);
        return distance(oldX, oldY, unit.getX(), unit.getY()) > 0.01;
    }

    private boolean tryMoveCombatUnit(CombatUnit unit, double targetX, double targetY) {
        boolean moved = false;
        if (canOccupy(unit, targetX, unit.getY())) {
            unit.setX(targetX);
            moved = true;
        }
        if (canOccupy(unit, unit.getX(), targetY)) {
            unit.setY(targetY);
            moved = true;
        }
        return moved;
    }

    private boolean canOccupy(CombatEntity entity, double x, double y) {
        double radius = entity.getRadius();
        return !map.isBlockedPixel(x - radius, y - radius)
                && !map.isBlockedPixel(x + radius, y - radius)
                && !map.isBlockedPixel(x - radius, y + radius)
                && !map.isBlockedPixel(x + radius, y + radius);
    }

    private void centerCameraOnPlayer() {
        double visibleWorldW = getWidth() / ZOOM;
        double visibleWorldH = getHeight() / ZOOM;
        cameraX = clamp(player.x - visibleWorldW / 2.0, 0.0, Math.max(0.0, map.getPixelWidth() - visibleWorldW));
        cameraY = clamp(player.y - visibleWorldH / 2.0, 0.0, Math.max(0.0, map.getPixelHeight() - visibleWorldH));
    }

    private int detectTargetFps() {
        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDisplayMode();
        int refreshRate = displayMode.getRefreshRate();
        if (refreshRate <= 0 || refreshRate > 360) {
            return 60;
        }
        return refreshRate;
    }

    private double normalizeAngle(double a) {
        while (a > Math.PI) a -= Math.PI * 2.0;
        while (a < -Math.PI) a += Math.PI * 2.0;
        return a;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private int worldToScreenX(double worldX) {
        return (int) Math.round((worldX - cameraX) * ZOOM);
    }

    private int worldToScreenY(double worldY) {
        return (int) Math.round((worldY - cameraY) * ZOOM);
    }

    private double screenToWorldX(double screenX) {
        return cameraX + screenX / ZOOM;
    }

    private double screenToWorldY(double screenY) {
        return cameraY + screenY / ZOOM;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawMap(g2);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawStructures(g2);
        drawCreeps(g2);
        drawExperienceOrbs(g2);
        drawBullets(g2);
        drawPlayer(g2);
        hudRenderer.draw(g2, buildHudModel());
    }

    private void drawMap(Graphics2D g2) {
        if (mapLayer == null) {
            return;
        }

        int sx1 = (int) cameraX;
        int sy1 = (int) cameraY;
        int sourceW = (int) Math.ceil(getWidth() / ZOOM);
        int sourceH = (int) Math.ceil(getHeight() / ZOOM);
        int sx2 = Math.min(sx1 + sourceW, mapLayer.getWidth());
        int sy2 = Math.min(sy1 + sourceH, mapLayer.getHeight());

        g2.drawImage(mapLayer, 0, 0, getWidth(), getHeight(), sx1, sy1, sx2, sy2, null);
    }

    private BufferedImage buildMiniMapLayer() {
        return hudRenderer.buildMiniMapLayer(mapLayer);
    }

    private void drawStructures(Graphics2D g2) {
        for (Structure s : structures) {
            if (s.hp <= 0) {
                continue;
            }

            int sx = worldToScreenX(s.x);
            int sy = worldToScreenY(s.y);
            int r = (int) Math.round(s.radius * ZOOM);

            Color body = s.team == Team.LIGHT ? new Color(128, 186, 255) : new Color(236, 100, 92);
            Color border = s.team == Team.LIGHT ? new Color(38, 82, 136) : new Color(128, 45, 40);

            if (s.type == StructureType.TOWER) {
                g2.setColor(body);
                g2.fillRoundRect(sx - r, sy - r, r * 2, r * 2, 8, 8);
                g2.setColor(border);
                g2.setStroke(new BasicStroke((float) (2.2f * ZOOM / 2.0)));
                g2.drawRoundRect(sx - r, sy - r, r * 2, r * 2, 8, 8);
            } else {
                g2.setColor(body);
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                g2.setColor(border);
                g2.setStroke(new BasicStroke((float) (3f * ZOOM / 2.0)));
                g2.drawOval(sx - r, sy - r, r * 2, r * 2);
            }

            drawHealthBar(g2, sx, sy - r - 12, 54, 7,
                    (double) s.hp / s.maxHp,
                    s.team == Team.LIGHT ? new Color(88, 168, 255) : new Color(248, 96, 88));
        }
    }

    private void drawCreeps(Graphics2D g2) {
        for (Creep creep : laneCreeps) {
            drawCreep(g2, creep);
        }
        for (Creep creep : neutralCreeps) {
            drawCreep(g2, creep);
        }
    }

    private void drawCreep(Graphics2D g2, Creep creep) {
        if (creep.hp <= 0) {
            return;
        }

        int sx = worldToScreenX(creep.x);
        int sy = worldToScreenY(creep.y);
        int r = (int) Math.round(creep.radius * ZOOM);
        BufferedImage sprite = creep.team == Team.LIGHT
                ? sprites.getPlayerFrame(creep.state, creep.animPhase)
                : sprites.getEnemyFrame(creep.state, creep.animPhase);
        drawSpriteWithFacing(g2, sprite, sx, sy, creep.lookAngle);

        if (creep.state == AnimationState.ATTACK) {
            g2.setColor(creep.team == Team.LIGHT
                    ? new Color(107, 198, 255, 110)
                    : new Color(255, 118, 92, 110));
            g2.fillOval(sx - r - 4, sy - r - 4, r * 2 + 8, r * 2 + 8);
        }

        drawHealthBar(g2, sx, sy - r - 10, 28, 5,
                (double) creep.hp / creep.maxHp,
                creep.team == Team.DARK ? new Color(241, 94, 85) : new Color(94, 188, 255));
    }

    private void drawExperienceOrbs(Graphics2D g2) {
        for (ExperienceOrb orb : experienceOrbs) {
            int ox = worldToScreenX(orb.x);
            int oy = worldToScreenY(orb.y + Math.sin(orb.phase) * 0.8);
            int r = (int) Math.round(orb.radius * ZOOM);

            g2.setColor(new Color(79, 210, 255, 85));
            g2.fillOval(ox - r - 3, oy - r - 3, (r + 3) * 2, (r + 3) * 2);

            g2.setColor(new Color(128, 235, 255, 220));
            g2.fillOval(ox - r, oy - r, r * 2, r * 2);
            g2.setColor(new Color(206, 255, 255, 220));
            g2.fillOval(ox - Math.max(1, r / 3), oy - Math.max(1, r / 3), Math.max(2, r / 2), Math.max(2, r / 2));
        }
    }

    private void drawPlayer(Graphics2D g2) {
        int px = worldToScreenX(player.x);
        int py = worldToScreenY(player.y);

        BufferedImage sprite = sprites.getPlayerFrame(player.state, player.animPhase);
        drawSpriteWithFacing(g2, sprite, px, py, player.aimAngle);

        if (player.state != AnimationState.DEAD) {
            drawHeldWeapon(g2, px, py);
        }
    }

    private void drawHeldWeapon(Graphics2D g2, int px, int py) {
        int handX = (int) (px + Math.cos(player.aimAngle) * 4 * ZOOM);
        int handY = (int) (py + Math.sin(player.aimAngle) * 4 * ZOOM);

        switch (currentWeapon) {
            case SWORD -> {
                int swordLen = (int) Math.round(18 * ZOOM);
                int swordX2 = (int) (px + Math.cos(player.aimAngle) * swordLen);
                int swordY2 = (int) (py + Math.sin(player.aimAngle) * swordLen);
                g2.setColor(new Color(175, 182, 189));
                g2.setStroke(new BasicStroke((float) (3.2f * ZOOM / 2.0)));
                g2.drawLine(px, py, swordX2, swordY2);
                g2.setColor(new Color(108, 70, 48));
                g2.setStroke(new BasicStroke((float) (1.9f * ZOOM / 2.0)));
                g2.drawLine(px - 2, py - 2, px + 2, py + 2);

                if (swordSwingTime > 0.0) {
                    int arcR = (int) Math.round(currentWeapon.meleeRange() * ZOOM);
                    g2.setColor(new Color(255, 236, 165, 110));
                    g2.setStroke(new BasicStroke((float) (2.5f * ZOOM / 2.0)));
                    int start = (int) Math.toDegrees(player.aimAngle - Math.toRadians(currentWeapon.meleeArcDegrees() / 2.0));
                    g2.drawArc(px - arcR, py - arcR, arcR * 2, arcR * 2, -start, (int) currentWeapon.meleeArcDegrees());
                }
            }
            case BOW -> {
                int tipX = (int) (px + Math.cos(player.aimAngle) * 20 * ZOOM);
                int tipY = (int) (py + Math.sin(player.aimAngle) * 20 * ZOOM);
                int side = Math.cos(player.aimAngle) >= 0 ? 1 : -1;
                int arcW = (int) Math.round(10 * ZOOM);
                int arcH = (int) Math.round(16 * ZOOM);

                g2.setColor(new Color(116, 78, 44));
                g2.setStroke(new BasicStroke((float) (2.5f * ZOOM / 2.0)));
                g2.drawArc(tipX - arcW / 2, tipY - arcH / 2, arcW, arcH, side > 0 ? 80 : 260, 200);

                g2.setColor(new Color(225, 225, 220, 200));
                g2.setStroke(new BasicStroke((float) (1.3f * ZOOM / 2.0)));
                g2.drawLine(tipX, tipY - arcH / 2 + 1, tipX, tipY + arcH / 2 - 1);

                int arrowX2 = (int) (tipX + Math.cos(player.aimAngle) * 8 * ZOOM);
                int arrowY2 = (int) (tipY + Math.sin(player.aimAngle) * 8 * ZOOM);
                g2.setColor(new Color(170, 170, 170));
                g2.drawLine(handX, handY, arrowX2, arrowY2);
            }
            case STONE -> {
                int stoneX = (int) (px + Math.cos(player.aimAngle) * 14 * ZOOM);
                int stoneY = (int) (py + Math.sin(player.aimAngle) * 14 * ZOOM);
                int r = (int) Math.round(3.6 * ZOOM);
                g2.setColor(new Color(106, 95, 83));
                g2.fillOval(stoneX - r, stoneY - r, r * 2, r * 2);
                g2.setColor(new Color(148, 136, 121));
                g2.fillOval(stoneX - r / 2, stoneY - r / 2, Math.max(2, r), Math.max(2, r));
            }
        }

        if (currentWeapon != WeaponType.SWORD && muzzleFlashTime > 0.0) {
            int muzzleX = (int) (px + Math.cos(player.aimAngle) * 20 * ZOOM);
            int muzzleY = (int) (py + Math.sin(player.aimAngle) * 20 * ZOOM);
            g2.setColor(new Color(255, 230, 125, 220));
            int flashSize = (int) Math.round(8 * ZOOM);
            g2.fillOval(muzzleX - flashSize / 2, muzzleY - flashSize / 2, flashSize, flashSize);
        }
    }

    private void drawSpriteWithFacing(Graphics2D g2, BufferedImage sprite, int centerX, int centerY, double angle) {
        int w = (int) Math.round(sprite.getWidth() * ZOOM);
        int h = (int) Math.round(sprite.getHeight() * ZOOM);
        int drawX = centerX - w / 2;
        int drawY = centerY - h / 2;

        if (Math.cos(angle) < 0) {
            g2.drawImage(sprite, drawX + w, drawY, -w, h, null);
        } else {
            g2.drawImage(sprite, drawX, drawY, w, h, null);
        }
    }

    private void drawBullets(Graphics2D g2) {
        for (Bullet bullet : bullets) {
            int bx = worldToScreenX(bullet.x);
            int by = worldToScreenY(bullet.y);
            int r = (int) Math.round(bullet.radius * ZOOM);
            g2.setColor(new Color(bullet.colorArgb, true));
            g2.fillOval(bx - r, by - r, r * 2, r * 2);
        }
    }

    private void drawHealthBar(Graphics2D g2,
                               int centerX,
                               int y,
                               int width,
                               int height,
                               double ratio,
                               Color fillColor) {
        int x = centerX - width / 2;
        ratio = clamp(ratio, 0.0, 1.0);

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(x, y, width, height, 6, 6);

        int fillW = (int) Math.round((width - 2) * ratio);
        g2.setColor(fillColor);
        g2.fillRoundRect(x + 1, y + 1, fillW, height - 2, 5, 5);

        g2.setColor(new Color(220, 220, 220, 170));
        g2.drawRoundRect(x, y, width, height, 6, 6);
    }

    private HudRenderer.Model buildHudModel() {
        return new HudRenderer.Model(
                getWidth(),
                getHeight(),
                ZOOM,
                player,
                currentWeapon,
                ammoInMagazine,
                reloadTimer,
                lightThrone,
                darkThrone,
                laneCreeps.size(),
                neutralCreeps.size(),
                kills,
                currentFps,
                targetFps,
                miniMapLayer,
                map,
                cameraX,
                cameraY,
                structures,
                laneCreeps,
                neutralCreeps,
                gameOver,
                victoryText
        );
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> down = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_1 -> switchWeapon(WeaponType.STONE);
            case KeyEvent.VK_2 -> switchWeapon(WeaponType.BOW);
            case KeyEvent.VK_3 -> switchWeapon(WeaponType.SWORD);
            case KeyEvent.VK_R -> {
                if (gameOver) {
                    resetGame();
                } else {
                    startReload();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> down = false;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = false;
            default -> {
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseMoved(e);

        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouseDown = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouseDown = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void removeNotify() {
        gameTimer.stop();
        super.removeNotify();
    }
}
