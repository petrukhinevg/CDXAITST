package com.example.demo.game;

import com.example.demo.game.collision.UnitCollisionResolver;
import com.example.demo.game.config.GameConfig;
import com.example.demo.game.model.AnimationState;
import com.example.demo.game.model.Bullet;
import com.example.demo.game.model.CombatEntity;
import com.example.demo.game.model.CombatUnit;
import com.example.demo.game.model.Creep;
import com.example.demo.game.model.CreepRole;
import com.example.demo.game.model.ExperienceOrb;
import com.example.demo.game.model.HeroAbility;
import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Player;
import com.example.demo.game.model.Structure;
import com.example.demo.game.model.StructureType;
import com.example.demo.game.model.Team;
import com.example.demo.game.model.WeaponType;
import com.example.demo.game.model.AbilitySlot;
import com.example.demo.game.render.HudRenderer;
import com.example.demo.game.render.MapRenderer;
import com.example.demo.game.render.SpriteLibrary;
import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.blueprint.MapBlueprint;
import com.example.demo.game.world.blueprint.MapBlueprintLoader;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.geom.Path2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener {
    private static final double ZOOM = 2.0;
    private static final double HERO_RENDER_SCALE = 1.18;
    private static final double HERO_RESPAWN_TIME = 1.0;
    private static final double EXPERIENCE_ORB_LIFETIME = 15.0;
    private static final double EXPERIENCE_MAGNET_RADIUS = 140.0;
    private static final double EXPERIENCE_PICKUP_RADIUS = 38.0;
    private static final double PLAYER_MOVE_SPEED = 107.5;
    private static final double PLAYER_WAYPOINT_REACHED_DISTANCE = 8.0;
    private static final double PLAYER_DESTINATION_REACHED_DISTANCE = 10.0;
    private static final double PLAYER_PATH_REBUILD_INTERVAL = 0.18;
    private static final double CLICK_TARGET_PADDING = 8.0;
    private static final double CREEP_PATH_REBUILD_INTERVAL = 0.35;
    private static final double CREEP_WAYPOINT_REACHED_DISTANCE = 8.0;
    private static final double CAMERA_MOVE_SPEED = 240.0;
    private static final double CAMERA_EDGE_MOVE_SPEED = 420.0;
    private static final int CAMERA_EDGE_SCROLL_MARGIN = 28;

    private final Random random = new Random();

    private final GameMap map = new GameMap();
    private final UnitCollisionResolver unitCollisionResolver = new UnitCollisionResolver();
    private final MapBlueprintLoader mapBlueprintLoader = new MapBlueprintLoader();
    private final HudRenderer hudRenderer = new HudRenderer();
    private final MapRenderer mapRenderer = new MapRenderer();
    private final SpriteLibrary sprites = SpriteLibrary.loadDefault();

    private final Player player = new Player();
    private final List<Player> heroes = new ArrayList<>();
    private final Team heroTeam = Team.LIGHT;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<ExperienceOrb> experienceOrbs = new ArrayList<>();
    private final List<Creep> laneCreeps = new ArrayList<>();
    private final List<Creep> neutralCreeps = new ArrayList<>();
    private final List<Structure> structures = new ArrayList<>();
    private final List<HeroAbility> heroAbilities = new ArrayList<>();

    private final EnumMap<Team, EnumMap<LaneType, List<Point>>> lanePaths = new EnumMap<>(Team.class);

    private Structure lightThrone;
    private Structure darkThrone;
    private MapBlueprint mapBlueprint;

    private BufferedImage mapLayer;
    private BufferedImage miniMapLayer;

    private WeaponType currentWeapon = WeaponType.STONE;

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean cameraUp;
    private boolean cameraDown;
    private boolean cameraLeft;
    private boolean cameraRight;
    private boolean middleMouseDragging;
    private boolean mouseInsideWindow;
    private int dragLastMouseX;
    private int dragLastMouseY;
    private int mouseX;
    private int mouseY;

    private double cameraX;
    private double cameraY;

    private double attackCooldown;
    private double muzzleFlashTime;
    private double swordSwingTime;
    private double laneWaveTimer;
    private double playerPathRefreshCooldown;

    private int kills;
    private boolean gameOver;
    private String victoryText = "";

    private boolean playerMoveOrderActive;
    private double playerOrderX;
    private double playerOrderY;
    private double playerPathFinalX;
    private double playerPathFinalY;
    private Creep playerAttackTarget;
    private final List<Point> playerPath = new ArrayList<>();

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

        Point playerStart = mapBlueprint.playerStart();
        initHeroes(playerStart);

        currentWeapon = WeaponType.STONE;
        initHeroAbilities();

        attackCooldown = 0.0;
        muzzleFlashTime = 0.0;
        swordSwingTime = 0.0;
        laneWaveTimer = 15.0;
        playerPathRefreshCooldown = 0.0;

        kills = 0;
        gameOver = false;
        victoryText = "";
        clearPlayerOrders();

        spawnLaneWave();

        centerCameraOnPlayer();
        lastTickNanos = System.nanoTime();
        fpsWindowStartNanos = lastTickNanos;
        framesThisSecond = 0;
        currentFps = 0;
        requestFocusInWindow();
    }

    private void initHeroes(Point playerStart) {
        heroes.clear();
        configureHero(player, heroTeam, playerStart);
        heroes.add(player);
    }

    private void configureHero(Player hero, Team team, Point startTile) {
        hero.x = map.tileCenter(startTile.x);
        hero.y = map.tileCenter(startTile.y);
        hero.spawnX = hero.x;
        hero.spawnY = hero.y;
        hero.radius = 14.5;
        hero.team = team;
        hero.maxHp = 120;
        hero.hp = hero.maxHp;
        hero.defense = 2;
        hero.level = 1;
        hero.xp = 0;
        hero.xpToNextLevel = 50;
        hero.moving = false;
        hero.animPhase = 0.0;
        hero.aimAngle = 0.0;
        hero.hitCooldown = 0.0;
        hero.respawnTimer = 0.0;
        hero.attackTimer = 0.0;
        hero.attackAnimationTimer = 0.0;
        hero.state = AnimationState.IDLE;
        if (hero == player) {
            clearPlayerOrders();
        }
    }

    private void regenerateMap() {
        mapBlueprint = mapBlueprintLoader.load("/maps/default.map");
        mapBlueprint.applyTo(map, random);
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
                teamPaths.put(lane, mapBlueprint.laneTilesForTeam(lane, team));
            }
            lanePaths.put(team, teamPaths);
        }
    }

    private void initStructures() {
        lightThrone = createThrone(Team.LIGHT, mapBlueprint.throneTile(Team.LIGHT));
        darkThrone = createThrone(Team.DARK, mapBlueprint.throneTile(Team.DARK));
        structures.add(lightThrone);
        structures.add(darkThrone);

        for (LaneType lane : LaneType.values()) {
            List<Point> lt = mapBlueprint.towerTiles(Team.LIGHT, lane);
            for (int i = 0; i < lt.size(); i++) {
                structures.add(createTower(Team.LIGHT, lane, i, lt.get(i)));
            }

            List<Point> dt = mapBlueprint.towerTiles(Team.DARK, lane);
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
    }

    private void initHeroAbilities() {
        heroAbilities.clear();
        heroAbilities.add(new HeroAbility(AbilitySlot.PRIMARY, "Навык I", 8.0, false));
        heroAbilities.add(new HeroAbility(AbilitySlot.SECONDARY, "Навык II", 12.0, false));
        heroAbilities.add(new HeroAbility(AbilitySlot.ULTIMATE, "Ультимейт", 40.0, true));
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
        creep.laneNavigationGoalIndex = -1;
        creep.laneRepathCooldown = 0.0;
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
        muzzleFlashTime = Math.max(0.0, muzzleFlashTime - dt);
        swordSwingTime = Math.max(0.0, swordSwingTime - dt);
        playerPathRefreshCooldown = Math.max(0.0, playerPathRefreshCooldown - dt);

        for (Player hero : heroes) {
            if (hero.hp <= 0) {
                hero.respawnTimer = Math.max(0.0, hero.respawnTimer - dt);
                if (!gameOver && hero.respawnTimer <= 0.0) {
                    respawnHero(hero);
                }
                continue;
            }
            hero.hitCooldown = Math.max(0.0, hero.hitCooldown - dt);
            hero.attackTimer = Math.max(0.0, hero.attackTimer - dt);
            hero.attackAnimationTimer = Math.max(0.0, hero.attackAnimationTimer - dt);
        }

        for (HeroAbility ability : heroAbilities) {
            ability.tick(dt);
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
        if (!gameOver && player.hp > 0) {
            moved = updatePlayerControl(dt);
        }

        updatePlayerAnimation(dt, moved);
        updateBullets(dt);
        updateLaneCreeps(dt);
        updateNeutralCreeps(dt);
        resolveUnitCollisions();
        updateStructures(dt);
        updateExperienceOrbs(dt);

        checkVictory();
        updateCamera(dt);
    }

    private void updatePlayerAnimation(double dt, boolean moved) {
        player.moving = moved;

        if (player.hp <= 0) {
            player.state = AnimationState.DEAD;
            player.animPhase += dt * 7.0;
            return;
        }

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

    private boolean updatePlayerControl(double dt) {
        if (hasMovementInput()) {
            clearPlayerOrders();
            return movePlayerByKeyboard(dt);
        }

        if (playerAttackTarget != null) {
            return updatePlayerAttackOrder(dt);
        }

        if (playerMoveOrderActive) {
            return updatePlayerMoveOrder(dt);
        }

        return false;
    }

    private boolean hasMovementInput() {
        return up || down || left || right;
    }

    private boolean movePlayerByKeyboard(double dt) {
        if (player.hp <= 0) {
            return false;
        }

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

        return moveCombatUnit(player, inputX * PLAYER_MOVE_SPEED * dt, inputY * PLAYER_MOVE_SPEED * dt);
    }

    private boolean updatePlayerMoveOrder(double dt) {
        boolean moved = followPlayerPath(dt, playerOrderX, playerOrderY);
        if (distance(player.x, player.y, playerOrderX, playerOrderY) <= PLAYER_DESTINATION_REACHED_DISTANCE) {
            clearPlayerOrders();
        }
        return moved;
    }

    private boolean updatePlayerAttackOrder(double dt) {
        if (playerAttackTarget == null || playerAttackTarget.hp <= 0) {
            clearPlayerOrders();
            return false;
        }

        player.aimAngle = Math.atan2(playerAttackTarget.y - player.y, playerAttackTarget.x - player.x);
        double attackReach = playerAttackReach(playerAttackTarget);
        if (canPlayerAttackTargetFrom(player.x, player.y, playerAttackTarget, attackReach)) {
            clearPlayerPath();
            if (attackCooldown <= 0.0) {
                attackWithCurrentWeapon();
            }
            return false;
        }

        if (playerPathRefreshCooldown <= 0.0 || playerPath.isEmpty()) {
            rebuildPlayerAttackPath(playerAttackTarget);
        }

        return followPlayerPath(dt, playerPathFinalX, playerPathFinalY);
    }

    private boolean followPlayerPath(double dt, double finalX, double finalY) {
        while (!playerPath.isEmpty()) {
            Point waypoint = playerPath.get(0);
            double waypointX = map.tileCenter(waypoint.x);
            double waypointY = map.tileCenter(waypoint.y);
            if (distance(player.x, player.y, waypointX, waypointY) <= PLAYER_WAYPOINT_REACHED_DISTANCE) {
                playerPath.remove(0);
                continue;
            }
            return movePlayerTowards(dt, waypointX, waypointY);
        }

        if (distance(player.x, player.y, finalX, finalY) <= PLAYER_DESTINATION_REACHED_DISTANCE) {
            return false;
        }

        return movePlayerTowards(dt, finalX, finalY);
    }

    private boolean movePlayerTowards(double dt, double targetX, double targetY) {
        double dx = targetX - player.x;
        double dy = targetY - player.y;
        double len = Math.hypot(dx, dy);
        if (len < 0.0001) {
            return false;
        }

        double step = Math.min(PLAYER_MOVE_SPEED * dt, len);
        double dirX = dx / len;
        double dirY = dy / len;
        player.aimAngle = Math.atan2(dirY, dirX);
        return moveCombatUnit(player, dirX * step, dirY * step);
    }

    private void issueMoveOrder(double worldX, double worldY) {
        clearPlayerOrders();
        if (gameOver || player.hp <= 0) {
            return;
        }

        double clampedX = clamp(worldX, player.radius, map.getPixelWidth() - player.radius);
        double clampedY = clamp(worldY, player.radius, map.getPixelHeight() - player.radius);
        Point goalTile = findNearestWalkableTileForPlayer(clampedX, clampedY);
        if (goalTile == null) {
            return;
        }

        PathSearchResult path = findPlayerPath(new TileGoal() {
            @Override
            public boolean isGoal(int tileX, int tileY) {
                return tileX == goalTile.x && tileY == goalTile.y;
            }

            @Override
            public double heuristic(int tileX, int tileY) {
                return tileDistance(tileX, tileY, goalTile.x, goalTile.y);
            }
        });
        if (path == null) {
            return;
        }

        playerMoveOrderActive = true;
        playerOrderX = canOccupy(player, clampedX, clampedY) ? clampedX : map.tileCenter(goalTile.x);
        playerOrderY = canOccupy(player, clampedX, clampedY) ? clampedY : map.tileCenter(goalTile.y);
        applyPlayerPath(path, playerOrderX, playerOrderY);
    }

    private void issueAttackOrder(Creep target) {
        clearPlayerOrders();
        if (gameOver || player.hp <= 0 || target == null || target.hp <= 0) {
            return;
        }

        playerAttackTarget = target;
        rebuildPlayerAttackPath(target);
    }

    private void rebuildPlayerAttackPath(Creep target) {
        if (target == null || target.hp <= 0) {
            clearPlayerOrders();
            return;
        }

        double attackReach = playerAttackReach(target);
        PathSearchResult path = findPlayerPath(new TileGoal() {
            @Override
            public boolean isGoal(int tileX, int tileY) {
                double tileCenterX = map.tileCenter(tileX);
                double tileCenterY = map.tileCenter(tileY);
                return isTileWalkableForPlayer(tileX, tileY)
                        && canPlayerAttackTargetFrom(tileCenterX, tileCenterY, target, attackReach);
            }

            @Override
            public double heuristic(int tileX, int tileY) {
                double dist = distance(map.tileCenter(tileX), map.tileCenter(tileY), target.x, target.y);
                return Math.max(0.0, dist - attackReach) / map.getTileSize();
            }
        });

        playerPathRefreshCooldown = PLAYER_PATH_REBUILD_INTERVAL;
        if (path == null) {
            clearPlayerPath();
            return;
        }

        double goalX = map.tileCenter(path.goalTile().x);
        double goalY = map.tileCenter(path.goalTile().y);
        applyPlayerPath(path, goalX, goalY);
    }

    private void applyPlayerPath(PathSearchResult path, double finalX, double finalY) {
        clearPlayerPath();
        playerPath.addAll(path.tiles());
        playerPathFinalX = finalX;
        playerPathFinalY = finalY;
    }

    private void clearPlayerOrders() {
        playerMoveOrderActive = false;
        playerAttackTarget = null;
        clearPlayerPath();
    }

    private void clearPlayerPath() {
        playerPath.clear();
        playerPathFinalX = player.x;
        playerPathFinalY = player.y;
        playerPathRefreshCooldown = 0.0;
    }

    private double playerAttackReach(CombatEntity target) {
        if (currentWeapon.projectile()) {
            return currentWeapon.projectileSpeed() * currentWeapon.projectileLife()
                    + currentWeapon.projectileRadius()
                    + target.getRadius();
        }
        return currentWeapon.meleeRange() + target.getRadius();
    }

    private boolean canPlayerAttackTargetFrom(double sourceX, double sourceY, CombatEntity target, double attackReach) {
        if (distance(sourceX, sourceY, target.getX(), target.getY()) > attackReach) {
            return false;
        }
        return !currentWeapon.projectile() || hasLineOfSight(sourceX, sourceY, target.getX(), target.getY());
    }

    private boolean hasLineOfSight(double startX, double startY, double endX, double endY) {
        double dx = endX - startX;
        double dy = endY - startY;
        double totalDistance = Math.hypot(dx, dy);
        if (totalDistance < 0.001) {
            return true;
        }

        int steps = Math.max(1, (int) Math.ceil(totalDistance / 6.0));
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            if (map.isBlockedPixel(startX + dx * t, startY + dy * t)) {
                return false;
            }
        }
        return true;
    }

    private Point findNearestWalkableTileForPlayer(double worldX, double worldY) {
        int targetTileX = clampTile((int) (worldX / map.getTileSize()), map.getWidth());
        int targetTileY = clampTile((int) (worldY / map.getTileSize()), map.getHeight());
        Point best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                if (!isTileWalkableForPlayer(tx, ty)) {
                    continue;
                }

                double dist = tileDistance(tx, ty, targetTileX, targetTileY);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    best = new Point(tx, ty);
                }
            }
        }

        return best;
    }

    private PathSearchResult findPlayerPath(TileGoal goal) {
        int startTileX = clampTile((int) (player.x / map.getTileSize()), map.getWidth());
        int startTileY = clampTile((int) (player.y / map.getTileSize()), map.getHeight());
        return findPath(
                new Point(startTileX, startTileY),
                goal,
                this::isTileWalkableForPlayer
        );
    }

    private PathSearchResult findPath(Point startTile, TileGoal goal, TileWalkability walkability) {
        int width = map.getWidth();
        int height = map.getHeight();
        if (goal.isGoal(startTile.x, startTile.y)) {
            return new PathSearchResult(Collections.emptyList(), new Point(startTile.x, startTile.y));
        }

        int totalTiles = width * height;
        double[] cost = new double[totalTiles];
        int[] parent = new int[totalTiles];
        boolean[] closed = new boolean[totalTiles];
        Arrays.fill(cost, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PathNode::priority));
        int startIndex = tileIndex(startTile.x, startTile.y);
        cost[startIndex] = 0.0;
        open.add(new PathNode(startTile.x, startTile.y, goal.heuristic(startTile.x, startTile.y)));

        int[][] directions = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0},           {1, 0},
                {-1, 1},  {0, 1},  {1, 1}
        };

        while (!open.isEmpty()) {
            PathNode current = open.poll();
            int currentIndex = tileIndex(current.tileX(), current.tileY());
            if (closed[currentIndex]) {
                continue;
            }
            closed[currentIndex] = true;

            if (goal.isGoal(current.tileX(), current.tileY())) {
                return new PathSearchResult(
                        reconstructPath(parent, currentIndex, startIndex, width),
                        new Point(current.tileX(), current.tileY())
                );
            }

            for (int[] direction : directions) {
                int nextX = current.tileX() + direction[0];
                int nextY = current.tileY() + direction[1];
                if (!walkability.isWalkable(nextX, nextY)) {
                    continue;
                }
                if (direction[0] != 0 && direction[1] != 0
                        && (!walkability.isWalkable(current.tileX() + direction[0], current.tileY())
                        || !walkability.isWalkable(current.tileX(), current.tileY() + direction[1]))) {
                    continue;
                }

                int nextIndex = tileIndex(nextX, nextY);
                if (closed[nextIndex]) {
                    continue;
                }

                double stepCost = direction[0] != 0 && direction[1] != 0 ? Math.sqrt(2.0) : 1.0;
                double tentativeCost = cost[currentIndex] + stepCost;
                if (tentativeCost >= cost[nextIndex]) {
                    continue;
                }

                cost[nextIndex] = tentativeCost;
                parent[nextIndex] = currentIndex;
                open.add(new PathNode(nextX, nextY, tentativeCost + goal.heuristic(nextX, nextY)));
            }
        }

        return null;
    }

    private List<Point> reconstructPath(int[] parent, int goalIndex, int startIndex, int width) {
        List<Point> path = new ArrayList<>();
        int current = goalIndex;
        while (current != startIndex && current >= 0) {
            int tileX = current % width;
            int tileY = current / width;
            path.add(new Point(tileX, tileY));
            current = parent[current];
        }
        Collections.reverse(path);
        return path;
    }

    private boolean isTileWalkableForPlayer(int tileX, int tileY) {
        if (!map.inBounds(tileX, tileY)) {
            return false;
        }
        return canOccupy(player, map.tileCenter(tileX), map.tileCenter(tileY));
    }

    private int tileIndex(int tileX, int tileY) {
        return tileY * map.getWidth() + tileX;
    }

    private int clampTile(int tile, int limit) {
        return Math.max(0, Math.min(limit - 1, tile));
    }

    private double tileDistance(int tileX1, int tileY1, int tileX2, int tileY2) {
        return Math.hypot(tileX1 - tileX2, tileY1 - tileY2);
    }

    private void respawnHero(Player hero) {
        hero.x = hero.spawnX;
        hero.y = hero.spawnY;
        hero.hp = hero.maxHp;
        hero.moving = false;
        hero.hitCooldown = 0.0;
        hero.respawnTimer = 0.0;
        hero.attackTimer = 0.0;
        hero.attackAnimationTimer = 0.0;
        hero.state = AnimationState.IDLE;
        hero.animPhase = 0.0;
        if (hero == player) {
            clearPlayerOrders();
        }
    }

    private void attackWithCurrentWeapon() {
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

        for (Player hero : heroes) {
            if (isHostileHero(hero) && inMeleeArc(hero.x, hero.y, weapon.meleeRange() + hero.radius, arcHalf)) {
                damageHero(hero, weapon.damage());
            }
        }

        for (Creep creep : laneCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, weapon.meleeRange() + creep.radius, arcHalf)) {
                damageCreepByHero(creep, weapon.damage());
            }
        }

        for (Creep creep : neutralCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, weapon.meleeRange() + creep.radius, arcHalf)) {
                damageCreepByHero(creep, weapon.damage());
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
            for (Player hero : heroes) {
                if (!isHostileHero(hero)) {
                    continue;
                }
                if (distance(bullet.x, bullet.y, hero.x, hero.y) <= bullet.radius + hero.radius) {
                    damageHero(hero, bullet.damage);
                    hit = true;
                    break;
                }
            }

            if (hit) {
                it.remove();
                continue;
            }

            for (Creep creep : laneCreeps) {
                if (!isHostileCreep(creep) || creep.hp <= 0) {
                    continue;
                }
                if (distance(bullet.x, bullet.y, creep.x, creep.y) <= bullet.radius + creep.radius) {
                    damageCreepByHero(creep, bullet.damage);
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
                        damageCreepByHero(creep, bullet.damage);
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
                handleCreepDeathRewards(creep, 8 + random.nextInt(4));
                it.remove();
                continue;
            }

            creep.attackTimer = Math.max(0.0, creep.attackTimer - dt);
            creep.attackAnimationTimer = Math.max(0.0, creep.attackAnimationTimer - dt);
            creep.laneRepathCooldown = Math.max(0.0, creep.laneRepathCooldown - dt);

            Creep enemyCreep = findNearestEnemyLaneCreep(creep, 130.0);
            if (enemyCreep != null) {
                engageCreepTarget(creep, enemyCreep, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            Player heroTarget = findLaneHeroTarget(creep);
            if (heroTarget != null) {
                engageHeroTarget(creep, heroTarget, dt);
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
            damageCreepByCreep(target, creep.damage);
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

    private void engageHeroTarget(Creep creep, Player hero, double dt) {
        double dist = distance(creep.x, creep.y, hero.x, hero.y);
        if (dist > creep.attackRange + hero.radius) {
            moveTowards(creep, hero.x, hero.y, creep.moveSpeed * dt);
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageHero(hero, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, hero.x, hero.y);
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
            clearCreepLanePath(creep);
            targetPoint = path.get(creep.waypointIndex);
            tx = map.tileCenter(targetPoint.x);
            ty = map.tileCenter(targetPoint.y);
        }

        followLanePath(creep, dt, targetPoint, tx, ty);
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

    private void followLanePath(Creep creep, double dt, Point goalTile, double goalX, double goalY) {
        if (creep.laneNavigationGoalIndex != creep.waypointIndex
                || creep.laneNavigationPath.isEmpty()
                || creep.laneRepathCooldown <= 0.0) {
            rebuildCreepLanePath(creep, goalTile);
        }

        while (!creep.laneNavigationPath.isEmpty()) {
            Point waypoint = creep.laneNavigationPath.get(0);
            double waypointX = map.tileCenter(waypoint.x);
            double waypointY = map.tileCenter(waypoint.y);
            if (distance(creep.x, creep.y, waypointX, waypointY) <= CREEP_WAYPOINT_REACHED_DISTANCE) {
                creep.laneNavigationPath.remove(0);
                continue;
            }

            moveTowards(creep, waypointX, waypointY, creep.moveSpeed * dt);
            return;
        }

        moveTowards(creep, goalX, goalY, creep.moveSpeed * dt);
    }

    private void rebuildCreepLanePath(Creep creep, Point goalTile) {
        Point startTile = findNearestLaneTile(creep.x, creep.y, creep.lane);
        if (startTile == null) {
            clearCreepLanePath(creep);
            return;
        }

        PathSearchResult path = findPath(
                startTile,
                new TileGoal() {
                    @Override
                    public boolean isGoal(int tileX, int tileY) {
                        return tileX == goalTile.x && tileY == goalTile.y;
                    }

                    @Override
                    public double heuristic(int tileX, int tileY) {
                        return tileDistance(tileX, tileY, goalTile.x, goalTile.y);
                    }
                },
                (tileX, tileY) -> isLaneTileWalkableForCreep(creep, tileX, tileY)
        );

        creep.laneNavigationPath.clear();
        creep.laneNavigationGoalIndex = creep.waypointIndex;
        creep.laneRepathCooldown = CREEP_PATH_REBUILD_INTERVAL;
        if (path == null) {
            return;
        }

        creep.laneNavigationPath.addAll(path.tiles());
    }

    private void clearCreepLanePath(Creep creep) {
        creep.laneNavigationPath.clear();
        creep.laneNavigationGoalIndex = -1;
        creep.laneRepathCooldown = 0.0;
    }

    private Point findNearestLaneTile(double worldX, double worldY, LaneType laneType) {
        Point best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                if (!map.hasLaneType(tx, ty, laneType)) {
                    continue;
                }

                double dist = distance(worldX, worldY, map.tileCenter(tx), map.tileCenter(ty));
                if (dist < bestDistance) {
                    bestDistance = dist;
                    best = new Point(tx, ty);
                }
            }
        }

        return best;
    }

    private boolean isLaneTileWalkableForCreep(Creep creep, int tileX, int tileY) {
        if (!map.inBounds(tileX, tileY) || !map.hasLaneType(tileX, tileY, creep.lane)) {
            return false;
        }
        return canOccupy(creep, map.tileCenter(tileX), map.tileCenter(tileY));
    }

    private void updateNeutralCreeps(double dt) {
        Iterator<Creep> it = neutralCreeps.iterator();
        while (it.hasNext()) {
            Creep creep = it.next();

            if (creep.hp <= 0) {
                handleCreepDeathRewards(creep, 14 + random.nextInt(6));
                it.remove();
                continue;
            }

            creep.attackTimer = Math.max(0.0, creep.attackTimer - dt);
            creep.attackAnimationTimer = Math.max(0.0, creep.attackAnimationTimer - dt);

            double distToHome = distance(creep.x, creep.y, creep.homeX, creep.homeY);
            Player nearbyHero = gameOver ? null : findNearestHero(creep.homeX, creep.homeY, creep.aggroRadius);
            Player leashedHero = gameOver ? null : findNearestHero(creep.homeX, creep.homeY, creep.leashRadius + 8.0);
            if (creep.aggroedToHero && leashedHero == null) {
                creep.aggroedToHero = false;
            }
            if (!creep.aggroedToHero && nearbyHero != null) {
                creep.aggroedToHero = true;
            }

            if (distToHome > creep.leashRadius) {
                creep.aggroedToHero = false;
                moveTowards(creep, creep.homeX, creep.homeY, creep.moveSpeed * dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            if (creep.aggroedToHero && !gameOver) {
                Player targetHero = findNearestHero(creep.x, creep.y, creep.aggroRadius * 1.5);
                if (targetHero != null) {
                    engageHeroTarget(creep, targetHero, dt);
                    updateCreepAnimation(creep, dt);
                    continue;
                }
                creep.aggroedToHero = false;
            }

            if (creep.aggroedToHero) {
                updateCreepAnimation(creep, dt);
                continue;
            }

            Creep targetLaneCreep = findNearestLaneCreepAround(creep.homeX, creep.homeY, creep.aggroRadius);
            if (targetLaneCreep != null) {
                engageCreepTarget(creep, targetLaneCreep, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            if (distToHome > 6.0) {
                moveTowards(creep, creep.homeX, creep.homeY, creep.moveSpeed * dt);
            } else {
                creep.moving = false;
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

            List<Player> heroCandidates = new ArrayList<>();
            if (!gameOver) {
                for (Player hero : heroes) {
                    if (hero.hp <= 0 || hero.team != enemyTeam) {
                        continue;
                    }
                    if (distance(structure.x, structure.y, hero.x, hero.y) <= structure.attackRange) {
                        heroCandidates.add(hero);
                    }
                }
            }

            int totalTargets = candidates.size() + heroCandidates.size();
            if (totalTargets == 0) {
                continue;
            }

            int picked = random.nextInt(totalTargets);
            if (picked < candidates.size()) {
                damageCreepByStructure(candidates.get(picked), structure.damage);
            } else {
                damageHero(heroCandidates.get(picked - candidates.size()), structure.damage);
            }

            structure.attackTimer = structure.attackCooldown;
        }
    }

    private void resolveUnitCollisions() {
        List<CombatEntity> collidables = new ArrayList<>();
        addLivingHeroes(collidables);
        addLivingUnits(collidables, laneCreeps);
        addLivingUnits(collidables, neutralCreeps);
        addLivingStructures(collidables);
        unitCollisionResolver.resolve(collidables, this::tryMoveCollidableEntity);
    }

    private void addLivingHeroes(List<CombatEntity> collidables) {
        if (gameOver) {
            return;
        }
        for (Player hero : heroes) {
            if (hero.hp > 0) {
                collidables.add(hero);
            }
        }
    }

    private void addLivingUnits(List<CombatEntity> units, List<Creep> creeps) {
        for (Creep creep : creeps) {
            if (creep.hp > 0) {
                units.add(creep);
            }
        }
    }

    private void addLivingStructures(List<CombatEntity> collidables) {
        for (Structure structure : structures) {
            if (structure.hp > 0) {
                collidables.add(structure);
            }
        }
    }

    private boolean tryMoveCollidableEntity(CombatEntity entity, double targetX, double targetY) {
        if (entity instanceof Structure) {
            return false;
        }
        return tryMoveCombatUnit((CombatUnit) entity, targetX, targetY);
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

    private Player findLaneHeroTarget(Creep creep) {
        if (gameOver) {
            return null;
        }

        Player hero = findNearestHeroByTeam(creep.x, creep.y, 140.0, creep.team.opposite());
        if (hero == null) {
            return null;
        }
        return findNearestLaneCreepByTeam(hero.x, hero.y, 96.0, hero.team) == null ? hero : null;
    }

    private void damageCreepByHero(Creep creep, int damage) {
        creep.lastHitByHero = true;
        creep.lastHitByCreep = false;
        damageEntity(creep, damage);
    }

    private void damageCreepByCreep(Creep creep, int damage) {
        creep.lastHitByHero = false;
        creep.lastHitByCreep = true;
        damageEntity(creep, damage);
    }

    private void damageCreepByStructure(Creep creep, int damage) {
        creep.lastHitByHero = false;
        creep.lastHitByCreep = false;
        damageEntity(creep, damage);
    }

    private void damageStructure(Structure structure, int damage) {
        damageEntity(structure, damage);
    }

    private void damageHero(Player hero, int damage) {
        if (hero.hp <= 0 || hero.hitCooldown > 0.0) {
            return;
        }
        hero.hitCooldown = 0.35;
        damageEntity(hero, damage);
        if (hero.hp <= 0) {
            hero.hp = 0;
            hero.respawnTimer = HERO_RESPAWN_TIME;
            hero.moving = false;
            hero.attackTimer = 0.0;
            hero.attackAnimationTimer = 0.0;
            hero.state = AnimationState.DEAD;
            hero.animPhase = 0.0;
            if (hero == player) {
                clearPlayerOrders();
            }
        }
    }

    private void damageEntity(CombatEntity entity, int damage) {
        entity.applyDamage(damage);
    }

    private void handleCreepDeathRewards(Creep creep, int totalXp) {
        if (creep.lastHitByHero) {
            spawnExperienceOrbs(creep.x, creep.y, totalXp, true);
            return;
        }

        if (creep.lastHitByCreep) {
            spawnExperienceOrbs(creep.x, creep.y, Math.max(1, totalXp / 2), false);
        }
    }

    private void spawnExperienceOrbs(double x, double y, int totalXp, boolean globalMagnet) {
        if (totalXp <= 0) {
            return;
        }

        int orbCount = 2 + random.nextInt(2);

        for (int i = 0; i < orbCount; i++) {
            ExperienceOrb orb = new ExperienceOrb();
            orb.x = x + random.nextDouble() * 14.0 - 7.0;
            orb.y = y + random.nextDouble() * 14.0 - 7.0;
            orb.radius = 3.8 + random.nextDouble() * 1.2;
            orb.value = Math.max(1, totalXp / orbCount + random.nextInt(2));
            orb.phase = random.nextDouble() * Math.PI * 2.0;
            orb.lifetime = EXPERIENCE_ORB_LIFETIME;
            orb.globalMagnet = globalMagnet;
            experienceOrbs.add(orb);
        }
    }

    private void updateExperienceOrbs(double dt) {
        Iterator<ExperienceOrb> it = experienceOrbs.iterator();
        while (it.hasNext()) {
            ExperienceOrb orb = it.next();
            orb.phase += dt * 6.0;
            orb.lifetime -= dt;
            if (orb.lifetime <= 0.0) {
                it.remove();
                continue;
            }

            if (gameOver || player.hp <= 0) {
                continue;
            }

            double dx = player.x - orb.x;
            double dy = player.y - orb.y;
            double dist = Math.hypot(dx, dy);

            if (dist > 0.001 && (orb.globalMagnet || dist < EXPERIENCE_MAGNET_RADIUS)) {
                double speed;
                if (orb.globalMagnet) {
                    speed = 180.0 + Math.min(260.0, dist * 0.12);
                } else {
                    double factor = 1.0 - dist / EXPERIENCE_MAGNET_RADIUS;
                    speed = 40.0 + factor * 210.0;
                }
                orb.x += dx / dist * speed * dt;
                orb.y += dy / dist * speed * dt;
            }

            if (dist <= EXPERIENCE_PICKUP_RADIUS) {
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
                .min(Comparator.comparingDouble(s -> distance(creep.x, creep.y, s.x, s.y)))
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

    private Player findNearestHeroByTeam(double x, double y, double radius, Team team) {
        Player bestHero = null;
        double best = Double.MAX_VALUE;

        for (Player hero : heroes) {
            if (hero.hp <= 0 || hero.team != team) {
                continue;
            }

            double dist = distance(x, y, hero.x, hero.y);
            if (dist < radius && dist < best) {
                best = dist;
                bestHero = hero;
            }
        }

        return bestHero;
    }

    private Player findNearestHero(double x, double y, double radius) {
        Player bestHero = null;
        double best = Double.MAX_VALUE;

        for (Player hero : heroes) {
            if (hero.hp <= 0) {
                continue;
            }

            double dist = distance(x, y, hero.x, hero.y);
            if (dist < radius && dist < best) {
                best = dist;
                bestHero = hero;
            }
        }

        return bestHero;
    }

    private Creep findClickedHostileCreep(double worldX, double worldY) {
        Creep target = null;
        double bestDistance = Double.MAX_VALUE;

        for (Creep creep : laneCreeps) {
            if (!isHostileCreep(creep)) {
                continue;
            }

            double dist = distance(worldX, worldY, creep.x, creep.y);
            if (dist <= creep.radius + CLICK_TARGET_PADDING && dist < bestDistance) {
                bestDistance = dist;
                target = creep;
            }
        }

        for (Creep creep : neutralCreeps) {
            if (!isHostileCreep(creep)) {
                continue;
            }

            double dist = distance(worldX, worldY, creep.x, creep.y);
            if (dist <= creep.radius + CLICK_TARGET_PADDING && dist < bestDistance) {
                bestDistance = dist;
                target = creep;
            }
        }

        return target;
    }

    private boolean isHostileHero(Player hero) {
        return hero != player && hero.hp > 0 && hero.team != player.team;
    }

    private boolean isHostileCreep(Creep creep) {
        return creep.hp > 0 && (creep.team == Team.DARK || creep.team == Team.NEUTRAL);
    }

    private boolean isHostileStructure(Structure structure) {
        return structure.hp > 0 && structure.team == Team.DARK;
    }

    private void switchWeapon(WeaponType weapon) {
        if (currentWeapon == weapon) {
            return;
        }

        currentWeapon = weapon;
        attackCooldown = Math.min(attackCooldown, 0.12);
        playerPathRefreshCooldown = 0.0;
    }

    private void triggerAbility(AbilitySlot slot) {
        if (gameOver) {
            return;
        }

        HeroAbility ability = findAbility(slot);
        if (ability == null || !ability.isReady()) {
            return;
        }

        ability.triggerPlaceholder();
    }

    private HeroAbility findAbility(AbilitySlot slot) {
        for (HeroAbility ability : heroAbilities) {
            if (ability.slot() == slot) {
                return ability;
            }
        }
        return null;
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
        boolean freeFromTerrain = !map.isBlockedPixel(x - radius, y - radius)
                && !map.isBlockedPixel(x + radius, y - radius)
                && !map.isBlockedPixel(x - radius, y + radius)
                && !map.isBlockedPixel(x + radius, y + radius);
        if (!freeFromTerrain) {
            return false;
        }

        for (Structure structure : structures) {
            if (structure == entity || structure.hp <= 0) {
                continue;
            }
            if (distance(x, y, structure.x, structure.y) < radius + structure.radius) {
                return false;
            }
        }

        return true;
    }

    private void centerCameraOnPlayer() {
        double visibleWorldW = viewportWidth() / ZOOM;
        double visibleWorldH = viewportHeight() / ZOOM;
        cameraX = clamp(player.x - visibleWorldW / 2.0, 0.0, Math.max(0.0, map.getPixelWidth() - visibleWorldW));
        cameraY = clamp(player.y - visibleWorldH / 2.0, 0.0, Math.max(0.0, map.getPixelHeight() - visibleWorldH));
    }

    private void updateCamera(double dt) {
        double velocityX = 0.0;
        double velocityY = 0.0;

        double inputX = 0.0;
        double inputY = 0.0;
        if (cameraUp) inputY -= 1.0;
        if (cameraDown) inputY += 1.0;
        if (cameraLeft) inputX -= 1.0;
        if (cameraRight) inputX += 1.0;

        if (inputX != 0.0 || inputY != 0.0) {
            double len = Math.hypot(inputX, inputY);
            velocityX += inputX / len * CAMERA_MOVE_SPEED;
            velocityY += inputY / len * CAMERA_MOVE_SPEED;
        }

        if (mouseInsideWindow && !middleMouseDragging) {
            double edgeX = 0.0;
            double edgeY = 0.0;
            int width = viewportWidth();
            int height = viewportHeight();
            if (mouseX <= CAMERA_EDGE_SCROLL_MARGIN) edgeX -= 1.0;
            if (mouseX >= width - CAMERA_EDGE_SCROLL_MARGIN) edgeX += 1.0;
            if (mouseY <= CAMERA_EDGE_SCROLL_MARGIN) edgeY -= 1.0;
            if (mouseY >= height - CAMERA_EDGE_SCROLL_MARGIN) edgeY += 1.0;

            if (edgeX != 0.0 || edgeY != 0.0) {
                double len = Math.hypot(edgeX, edgeY);
                velocityX += edgeX / len * CAMERA_EDGE_MOVE_SPEED;
                velocityY += edgeY / len * CAMERA_EDGE_MOVE_SPEED;
            }
        }

        if (velocityX == 0.0 && velocityY == 0.0) {
            return;
        }

        double visibleWorldW = viewportWidth() / ZOOM;
        double visibleWorldH = viewportHeight() / ZOOM;
        cameraX = clamp(cameraX + velocityX * dt, 0.0, Math.max(0.0, map.getPixelWidth() - visibleWorldW));
        cameraY = clamp(cameraY + velocityY * dt, 0.0, Math.max(0.0, map.getPixelHeight() - visibleWorldH));
    }

    private void panCameraByScreenDelta(int deltaX, int deltaY) {
        double visibleWorldW = viewportWidth() / ZOOM;
        double visibleWorldH = viewportHeight() / ZOOM;
        cameraX = clamp(cameraX - deltaX / ZOOM, 0.0, Math.max(0.0, map.getPixelWidth() - visibleWorldW));
        cameraY = clamp(cameraY - deltaY / ZOOM, 0.0, Math.max(0.0, map.getPixelHeight() - visibleWorldH));
    }

    private int viewportWidth() {
        return getWidth() > 0 ? getWidth() : GameConfig.VIEW_W;
    }

    private int viewportHeight() {
        return getHeight() > 0 ? getHeight() : GameConfig.VIEW_H;
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
        drawHeroes(g2);
        drawHeroOverheadHud(g2);
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
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                g2.setColor(border);
                g2.setStroke(new BasicStroke((float) (2.2f * ZOOM / 2.0)));
                g2.drawOval(sx - r, sy - r, r * 2, r * 2);
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

    private void drawHeroes(Graphics2D g2) {
        for (Player hero : heroes) {
            if (hero.hp <= 0) {
                continue;
            }

            int px = worldToScreenX(hero.x);
            int py = worldToScreenY(hero.y);

            if (hero == player) {
                BufferedImage sprite = customizePlayerSprite(sprites.getPlayerFrame(hero.state, hero.animPhase));
                drawTintedSpriteWithFacing(g2, sprite, px, py, hero.aimAngle, HERO_RENDER_SCALE, null);
                drawPlayerHelmet(g2, px, py);

                if (hero.state != AnimationState.DEAD) {
                    drawHeldWeapon(g2, px, py);
                }
                continue;
            }

            BufferedImage sprite = hero.team == Team.LIGHT
                    ? sprites.getPlayerFrame(hero.state, hero.animPhase)
                    : sprites.getEnemyFrame(hero.state, hero.animPhase);
            drawTintedSpriteWithFacing(g2, sprite, px, py, hero.aimAngle, HERO_RENDER_SCALE, null);
        }
    }

    private void drawHeroOverheadHud(Graphics2D g2) {
        if (gameOver || player.hp <= 0) {
            return;
        }

        int px = worldToScreenX(player.x);
        int py = worldToScreenY(player.y);
        int topY = py - (int) Math.round(40 * ZOOM);

        g2.setColor(new Color(0, 0, 0, 125));
        g2.fillRoundRect(px - 58, topY - 20, 116, 34, 12, 12);

        g2.setColor(new Color(28, 34, 42, 220));
        g2.fillOval(px - 58, topY - 18, 24, 24);
        g2.setColor(new Color(230, 230, 230, 180));
        g2.drawOval(px - 58, topY - 18, 24, 24);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String levelText = String.valueOf(player.level);
        int levelW = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, px - 46 - levelW / 2, topY - 2);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String weaponText = currentWeapon.displayName();
        int weaponW = g2.getFontMetrics().stringWidth(weaponText);
        g2.drawString(weaponText, px - weaponW / 2, topY - 6);

        drawHealthBar(g2, px + 8, topY + 7, 86, 8,
                player.maxHp == 0 ? 0.0 : (double) player.hp / player.maxHp,
                new Color(223, 79, 77));
    }

    private void drawHeldWeapon(Graphics2D g2, int px, int py) {
        int handX = (int) (px + Math.cos(player.aimAngle) * 5 * ZOOM);
        int handY = (int) (py + Math.sin(player.aimAngle) * 5 * ZOOM);

        switch (currentWeapon) {
            case SWORD -> {
                double dirX = Math.cos(player.aimAngle);
                double dirY = Math.sin(player.aimAngle);
                double perpX = -dirY;
                double perpY = dirX;

                int pommelX = (int) Math.round(handX - dirX * 5 * ZOOM);
                int pommelY = (int) Math.round(handY - dirY * 5 * ZOOM);
                int tipX = (int) Math.round(handX + dirX * 18 * ZOOM);
                int tipY = (int) Math.round(handY + dirY * 18 * ZOOM);
                int guardHalf = (int) Math.round(3.5 * ZOOM);

                g2.setColor(new Color(110, 72, 46));
                g2.setStroke(new BasicStroke((float) (2.6f * ZOOM / 2.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(pommelX, pommelY, handX, handY);

                g2.setColor(new Color(186, 192, 198));
                g2.setStroke(new BasicStroke((float) (3.2f * ZOOM / 2.0)));
                g2.drawLine(handX, handY, tipX, tipY);

                g2.setColor(new Color(96, 104, 112));
                g2.setStroke(new BasicStroke((float) (1.8f * ZOOM / 2.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(
                        (int) Math.round(handX - perpX * guardHalf),
                        (int) Math.round(handY - perpY * guardHalf),
                        (int) Math.round(handX + perpX * guardHalf),
                        (int) Math.round(handY + perpY * guardHalf)
                );

                g2.setColor(new Color(230, 235, 238, 170));
                g2.setStroke(new BasicStroke((float) (1.1f * ZOOM / 2.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(
                        (int) Math.round(handX + dirX * 2 * ZOOM - perpX),
                        (int) Math.round(handY + dirY * 2 * ZOOM - perpY),
                        (int) Math.round(tipX - perpX * 1.2),
                        (int) Math.round(tipY - perpY * 1.2)
                );

                if (swordSwingTime > 0.0) {
                    drawSwordAttackArea(g2, px, py);
                }
            }
            case BOW -> {
                double dirX = Math.cos(player.aimAngle);
                double dirY = Math.sin(player.aimAngle);
                double perpX = -dirY;
                double perpY = dirX;
                double bowCenterX = px + dirX * 16.0 * ZOOM;
                double bowCenterY = py + dirY * 16.0 * ZOOM;
                double limbHalf = 7.0 * ZOOM;
                double bowDepth = 4.5 * ZOOM;

                double topX = bowCenterX + perpX * limbHalf;
                double topY = bowCenterY + perpY * limbHalf;
                double bottomX = bowCenterX - perpX * limbHalf;
                double bottomY = bowCenterY - perpY * limbHalf;
                double gripX = bowCenterX - dirX * bowDepth;
                double gripY = bowCenterY - dirY * bowDepth;

                Path2D.Double bowShape = new Path2D.Double();
                bowShape.moveTo(topX, topY);
                bowShape.quadTo(bowCenterX + dirX * bowDepth, bowCenterY + dirY * bowDepth, gripX, gripY);
                bowShape.quadTo(bowCenterX + dirX * bowDepth, bowCenterY + dirY * bowDepth, bottomX, bottomY);

                g2.setColor(new Color(116, 78, 44));
                g2.setStroke(new BasicStroke((float) (2.5f * ZOOM / 2.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(bowShape);

                g2.setColor(new Color(225, 225, 220, 200));
                g2.setStroke(new BasicStroke((float) (1.3f * ZOOM / 2.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) Math.round(topX), (int) Math.round(topY), (int) Math.round(bottomX), (int) Math.round(bottomY));

                int arrowX1 = (int) Math.round(gripX - dirX * 4.0 * ZOOM);
                int arrowY1 = (int) Math.round(gripY - dirY * 4.0 * ZOOM);
                int arrowX2 = (int) Math.round(bowCenterX + dirX * 12.0 * ZOOM);
                int arrowY2 = (int) Math.round(bowCenterY + dirY * 12.0 * ZOOM);
                g2.setColor(new Color(170, 170, 170));
                g2.drawLine(arrowX1, arrowY1, arrowX2, arrowY2);
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
        drawTintedSpriteWithFacing(g2, sprite, centerX, centerY, angle, 1.0, null);
    }

    private void drawTintedSpriteWithFacing(Graphics2D g2,
                                            BufferedImage sprite,
                                            int centerX,
                                            int centerY,
                                            double angle,
                                            double scale,
                                            Color tint) {
        int w = (int) Math.round(sprite.getWidth() * ZOOM);
        int h = (int) Math.round(sprite.getHeight() * ZOOM);
        w = (int) Math.round(w * scale);
        h = (int) Math.round(h * scale);
        int drawX = centerX - w / 2;
        int drawY = centerY - h / 2;

        BufferedImage imageToDraw = tint == null ? sprite : tintedSprite(sprite, tint);
        if (Math.cos(angle) < 0) {
            g2.drawImage(imageToDraw, drawX + w, drawY, -w, h, null);
        } else {
            g2.drawImage(imageToDraw, drawX, drawY, w, h, null);
        }
    }

    private BufferedImage customizePlayerSprite(BufferedImage sprite) {
        PlayerPalette palette = playerPalette();
        BufferedImage customized = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int argb = sprite.getRGB(x, y);
                customized.setRGB(x, y, remapPlayerSpriteColor(argb, palette));
            }
        }

        return customized;
    }

    private int remapPlayerSpriteColor(int argb, PlayerPalette palette) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) {
            return argb;
        }

        if (isCloseColor(argb, 255, 220, 110, 28) || isCloseColor(argb, 220, 255, 110, 40)) {
            return 0x00000000;
        }
        if (isCloseColor(argb, 52, 126, 226, 14)) {
            return withAlpha(palette.primary(), alpha);
        }
        if (isCloseColor(argb, 30, 76, 152, 14)) {
            return withAlpha(palette.shadow(), alpha);
        }
        if (isCloseColor(argb, 43, 66, 104, 16)) {
            return withAlpha(palette.darkCloth(), alpha);
        }

        return argb;
    }

    private BufferedImage tintedSprite(BufferedImage sprite, Color tint) {
        BufferedImage tinted = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tinted.createGraphics();
        tg.drawImage(sprite, 0, 0, null);
        Composite oldComposite = tg.getComposite();
        tg.setComposite(AlphaComposite.SrcAtop.derive(0.42f));
        tg.setColor(tint);
        tg.fillRect(0, 0, sprite.getWidth(), sprite.getHeight());
        tg.setComposite(oldComposite);
        tg.dispose();
        return tinted;
    }

    private void drawPlayerHelmet(Graphics2D g2, int px, int py) {
        PlayerPalette palette = playerPalette();
        int helmetW = (int) Math.round(16 * ZOOM * HERO_RENDER_SCALE);
        int helmetH = (int) Math.round(9 * ZOOM * HERO_RENDER_SCALE);
        int helmetX = px - helmetW / 2;
        int helmetY = py - (int) Math.round(17 * ZOOM);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(helmetX + 2, helmetY + 4, helmetW - 4, helmetH);

        g2.setColor(palette.helmetShadow());
        g2.fillRoundRect(helmetX, helmetY + helmetH / 2 - 1, helmetW, helmetH / 2 + 3, 8, 8);
        g2.fillArc(helmetX, helmetY - 1, helmetW, helmetH + 6, 0, 180);

        g2.setColor(palette.helmetMain());
        g2.fillRoundRect(helmetX + 1, helmetY + helmetH / 2, helmetW - 2, helmetH / 2 + 1, 7, 7);
        g2.fillArc(helmetX + 1, helmetY, helmetW - 2, helmetH + 4, 0, 180);

        g2.setColor(new Color(232, 236, 240, 150));
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawArc(helmetX + 2, helmetY + 1, helmetW - 5, helmetH + 1, 12, 88);

        g2.setColor(palette.shadow());
        g2.fillRect(px - 1, helmetY + helmetH / 2 + 1, 3, helmetH / 2 + 3);
    }

    private PlayerPalette playerPalette() {
        return switch (currentWeapon) {
            case BOW -> new PlayerPalette(
                    new Color(72, 156, 86),
                    new Color(38, 98, 52),
                    new Color(34, 58, 39),
                    new Color(156, 164, 172),
                    new Color(94, 102, 112)
            );
            case SWORD -> new PlayerPalette(
                    new Color(148, 154, 161),
                    new Color(95, 101, 109),
                    new Color(58, 63, 70),
                    new Color(174, 179, 186),
                    new Color(108, 114, 122)
            );
            case STONE -> new PlayerPalette(
                    new Color(136, 98, 66),
                    new Color(88, 60, 40),
                    new Color(56, 40, 28),
                    new Color(147, 124, 103),
                    new Color(94, 76, 61)
            );
        };
    }

    private int withAlpha(Color color, int alpha) {
        return (alpha << 24)
                | (color.getRed() << 16)
                | (color.getGreen() << 8)
                | color.getBlue();
    }

    private boolean isCloseColor(int argb, int red, int green, int blue, int tolerance) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Math.abs(r - red) <= tolerance
                && Math.abs(g - green) <= tolerance
                && Math.abs(b - blue) <= tolerance;
    }

    private void drawSwordAttackArea(Graphics2D g2, int px, int py) {
        double arcHalf = Math.toRadians(currentWeapon.meleeArcDegrees() / 2.0);
        double start = player.aimAngle - arcHalf;
        double end = player.aimAngle + arcHalf;
        double outerR = currentWeapon.meleeRange() * ZOOM;
        double innerR = 16.0 * ZOOM;
        double centerR = (outerR + innerR) * 0.5;

        for (int layer = 0; layer < 6; layer++) {
            double t = layer / 5.0;
            double angleInset = arcHalf * 0.36 * (1.0 - t);
            double inner = innerR + (centerR - innerR) * (1.0 - t);
            double outer = outerR - (outerR - centerR) * (1.0 - t);
            Path2D.Double area = buildSwordSector(
                    px,
                    py,
                    start + angleInset,
                    end - angleInset,
                    inner,
                    outer
            );
            int alpha = 8 + (int) Math.round(t * 30.0);
            g2.setColor(new Color(240, 236, 198, alpha));
            g2.fill(area);
        }
    }

    private Path2D.Double buildSwordSector(int px, int py, double start, double end, double innerR, double outerR) {
        Path2D.Double area = new Path2D.Double();
        area.moveTo(px + Math.cos(start) * innerR, py + Math.sin(start) * innerR);
        for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            double angle = start + (end - start) * t;
            area.lineTo(px + Math.cos(angle) * outerR, py + Math.sin(angle) * outerR);
        }
        for (int i = 16; i >= 0; i--) {
            double t = i / 16.0;
            double angle = start + (end - start) * t;
            area.lineTo(px + Math.cos(angle) * innerR, py + Math.sin(angle) * innerR);
        }
        area.closePath();
        return area;
    }

    private record PlayerPalette(Color primary, Color shadow, Color darkCloth, Color helmetMain, Color helmetShadow) {
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
                heroes,
                currentWeapon,
                heroAbilities,
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
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_UP -> cameraUp = true;
            case KeyEvent.VK_DOWN -> cameraDown = true;
            case KeyEvent.VK_LEFT -> cameraLeft = true;
            case KeyEvent.VK_RIGHT -> cameraRight = true;
            case KeyEvent.VK_1 -> switchWeapon(WeaponType.STONE);
            case KeyEvent.VK_2 -> switchWeapon(WeaponType.BOW);
            case KeyEvent.VK_3 -> switchWeapon(WeaponType.SWORD);
            case KeyEvent.VK_Q -> triggerAbility(AbilitySlot.PRIMARY);
            case KeyEvent.VK_E -> triggerAbility(AbilitySlot.SECONDARY);
            case KeyEvent.VK_R -> {
                if (gameOver) {
                    resetGame();
                } else {
                    triggerAbility(AbilitySlot.ULTIMATE);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_UP -> cameraUp = false;
            case KeyEvent.VK_DOWN -> cameraDown = false;
            case KeyEvent.VK_LEFT -> cameraLeft = false;
            case KeyEvent.VK_RIGHT -> cameraRight = false;
            default -> {
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (middleMouseDragging) {
            int deltaX = e.getX() - dragLastMouseX;
            int deltaY = e.getY() - dragLastMouseY;
            panCameraByScreenDelta(deltaX, deltaY);
            dragLastMouseX = e.getX();
            dragLastMouseY = e.getY();
        }
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseInsideWindow = true;
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
            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());
            Creep clickedCreep = findClickedHostileCreep(worldX, worldY);
            if (clickedCreep != null) {
                issueAttackOrder(clickedCreep);
            } else {
                issueMoveOrder(worldX, worldY);
            }
        } else if (e.getButton() == MouseEvent.BUTTON2) {
            middleMouseDragging = true;
            dragLastMouseX = e.getX();
            dragLastMouseY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2) {
            middleMouseDragging = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseInsideWindow = true;
        mouseMoved(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseInsideWindow = false;
    }

    @Override
    public void removeNotify() {
        gameTimer.stop();
        super.removeNotify();
    }

    private interface TileGoal {
        boolean isGoal(int tileX, int tileY);

        double heuristic(int tileX, int tileY);
    }

    private interface TileWalkability {
        boolean isWalkable(int tileX, int tileY);
    }

    private record PathNode(int tileX, int tileY, double priority) {
    }

    private record PathSearchResult(List<Point> tiles, Point goalTile) {
    }
}
