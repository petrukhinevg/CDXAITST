package com.example.demo.game;

import com.example.demo.game.audio.GameAudio;
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
import com.example.demo.game.model.LaneCreepType;
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
import com.example.demo.game.world.blueprint.MapBlueprintWriter;
import com.example.demo.game.world.element.GroundElement;
import com.example.demo.game.world.element.GroundKind;
import com.example.demo.game.world.element.MapElements;
import com.example.demo.game.world.element.PropElement;
import com.example.demo.game.world.element.WaterElement;

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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener {
    private static final double ZOOM = 2.0;
    private static final double HERO_RENDER_SCALE = 0.8;
    private static final double HERO_RESPAWN_TIME = 1.0;
    private static final double EXPERIENCE_ORB_LIFETIME = 15.0;
    private static final double EXPERIENCE_MAGNET_RADIUS = 140.0;
    private static final double EXPERIENCE_PICKUP_RADIUS = 38.0;
    private static final double PLAYER_MOVE_SPEED = 107.5;
    private static final double PLAYER_WAYPOINT_REACHED_DISTANCE = 8.0;
    private static final double PLAYER_DESTINATION_REACHED_DISTANCE = 10.0;
    private static final double PLAYER_ATTACK_DESTINATION_REACHED_DISTANCE = 0.75;
    private static final double PLAYER_PATH_REBUILD_INTERVAL = 0.18;
    private static final double PLAYER_ATTACK_APPROACH_RATIO = 0.95;
    private static final double CLICK_TARGET_PADDING = 8.0;
    private static final double CREEP_PATH_REBUILD_INTERVAL = 0.35;
    private static final double CREEP_WAYPOINT_REACHED_DISTANCE = 8.0;
    private static final double UNIT_DIAMETER_TILES = 0.8;
    private static final double STANDARD_UNIT_RADIUS = GameConfig.TILE * UNIT_DIAMETER_TILES * 0.5;
    private static final double CREEP_LANE_SLOT_SPACING = STANDARD_UNIT_RADIUS * 0.55;
    private static final double CREEP_LANE_LOOKAHEAD = GameConfig.TILE * 1.1;
    private static final double CREEP_SEPARATION_RANGE = STANDARD_UNIT_RADIUS * 3.0;
    private static final double CREEP_STRUCTURE_AVOID_RANGE = GameConfig.TILE * 0.9;
    private static final double CREEP_TARGET_WEIGHT = 1.35;
    private static final double CREEP_SEPARATION_WEIGHT = 1.65;
    private static final double CREEP_STRUCTURE_AVOID_WEIGHT = 1.3;
    private static final double CREEP_FORWARD_WEIGHT = 0.18;
    private static final int MAX_CREEPS_ATTACKING_TOWER = 2;
    private static final int EDITOR_PANEL_WIDTH = 312;
    private static final int EDITOR_PANEL_MARGIN = 16;
    private static final int EDITOR_PANEL_PAD = 14;
    private static final int EDITOR_BUTTON_HEIGHT = 26;
    private static final int EDITOR_BUTTON_GAP = 6;
    private static final int EDITOR_SWATCH_SIZE = 34;
    private static final Path MAP_EDITOR_SAVE_PATH = Path.of("src/main/resources/maps/default.map");
    private static final double EDITOR_PANEL_EDGE_SWITCH_DISTANCE = GameConfig.TILE * 2.0;
    private static final double TREE_PLACEMENT_CHANCE = 0.18;
    private static final int TREE_LANE_CLEAR_RADIUS = 3;
    private static final int MAX_TREES_PER_ROW = 10;
    private static final int MAX_TREES_PER_COLUMN = 10;
    private static final int CREEP_STRUCTURE_APPROACH_SAMPLES = 16;
    private static final double CREEP_STRUCTURE_SLOT_ARC = Math.PI / 7.0;
    private static final double CREEP_COMBAT_SLOT_ARC = Math.PI / 10.0;
    private static final double CREEP_STRUCTURE_APPROACH_BUFFER = 2.0;
    private static final double UNIT_ATTACK_ANIMATION_DURATION = 0.28;
    private static final double UNIT_ATTACK_ANIMATION_PHASE_SPEED = 11.0;
    private static final double STRUCTURE_ATTACK_ANIMATION_DURATION = 0.42;
    private static final double CAMERA_MOVE_SPEED = 240.0;
    private static final double CAMERA_EDGE_MOVE_SPEED = 420.0;
    private static final int CAMERA_EDGE_SCROLL_MARGIN = 28;
    private static final double CLICK_MARKER_LIFETIME = 0.75;
    private static final double ATTACK_RANGE_BALANCE_SCALE = 1.0 / 1.25;

    private final Random random = new Random();

    private final GameMap map = new GameMap();
    private final UnitCollisionResolver unitCollisionResolver = new UnitCollisionResolver();
    private final MapBlueprintLoader mapBlueprintLoader = new MapBlueprintLoader();
    private final MapBlueprintWriter mapBlueprintWriter = new MapBlueprintWriter();
    private final HudRenderer hudRenderer = new HudRenderer();
    private final MapRenderer mapRenderer = new MapRenderer();
    private final SpriteLibrary sprites = SpriteLibrary.loadDefault();
    private final GameAudio audio = new GameAudio();

    private final Player player = new Player();
    private final List<Player> heroes = new ArrayList<>();
    private final Team heroTeam = Team.LIGHT;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<ExperienceOrb> experienceOrbs = new ArrayList<>();
    private final List<ClickMarker> clickMarkers = new ArrayList<>();
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

    private boolean cameraUp;
    private boolean cameraDown;
    private boolean cameraLeft;
    private boolean cameraRight;
    private boolean showAttackRanges;
    private boolean middleMouseDragging;
    private boolean miniMapDragging;
    private boolean editorMode;
    private boolean editorPainting;
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
    private CombatEntity playerAttackTarget;
    private final List<Point> playerPath = new ArrayList<>();

    private final int targetFps = detectTargetFps();
    private final Timer gameTimer;
    private long lastTickNanos;

    private int currentFps;
    private int framesThisSecond;
    private long fpsWindowStartNanos;
    private int laneWaveNumber;
    private EditorTool editorTool = EditorTool.PAINT;
    private GroundElement editorGround = MapElements.GRASS;
    private WaterElement editorWater;
    private PropElement editorProp;
    private int editorLaneMask;
    private boolean editorBlocked;
    private Structure editorDraggedStructure;
    private String editorStatusText = "Tab: editor mode";
    private double editorStatusTimer;
    private boolean editorDirty;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.VIEW_W, GameConfig.VIEW_H));
        setBackground(new Color(16, 32, 18));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
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
        clickMarkers.clear();
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
        editorMode = false;
        laneWaveNumber = 0;
        editorPainting = false;
        editorDraggedStructure = null;
        editorDirty = false;
        editorStatusText = "Tab: editor mode";
        editorStatusTimer = 0.0;
        audio.reset();
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
        hero.radius = STANDARD_UNIT_RADIUS;
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
        populateGrassTrees();
        rebuildMapLayers();
    }

    private void populateGrassTrees() {
        int width = map.getWidth();
        int height = map.getHeight();
        boolean[][] reserved = new boolean[height][width];
        reserveTreeTile(reserved, mapBlueprint.playerStart());
        reserveTreeTile(reserved, mapBlueprint.throneTile(Team.LIGHT));
        reserveTreeTile(reserved, mapBlueprint.throneTile(Team.DARK));
        for (Team team : List.of(Team.LIGHT, Team.DARK)) {
            for (LaneType lane : LaneType.values()) {
                for (Point tile : mapBlueprint.towerTiles(team, lane)) {
                    reserveTreeTile(reserved, tile);
                }
            }
        }

        List<Point> candidates = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isGrassTreeCandidate(x, y, reserved)) {
                    candidates.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(candidates, random);
        int[] rowTreeCounts = new int[height];
        int[] columnTreeCounts = new int[width];
        for (Point candidate : candidates) {
            if (rowTreeCounts[candidate.y] >= MAX_TREES_PER_ROW
                    || columnTreeCounts[candidate.x] >= MAX_TREES_PER_COLUMN
                    || random.nextDouble() >= TREE_PLACEMENT_CHANCE) {
                continue;
            }

            map.setBlocked(candidate.x, candidate.y, true);
            map.setProp(candidate.x, candidate.y, null);
            rowTreeCounts[candidate.y]++;
            columnTreeCounts[candidate.x]++;
        }
    }

    private void reserveTreeTile(boolean[][] reserved, Point tile) {
        if (tile != null && map.inBounds(tile.x, tile.y)) {
            reserved[tile.y][tile.x] = true;
        }
    }

    private boolean isGrassTreeCandidate(int x, int y, boolean[][] reserved) {
        if (reserved[y][x]
                || map.isBlocked(x, y)
                || map.isLane(x, y)
                || isWithinLaneTreeClearRadius(x, y)
                || map.getWater(x, y) != null
                || map.getProp(x, y) != null) {
            return false;
        }

        GroundKind groundKind = map.getGround(x, y).kind();
        return groundKind == GroundKind.GRASS || groundKind == GroundKind.GRASS_ALT;
    }

    private boolean isWithinLaneTreeClearRadius(int tileX, int tileY) {
        for (int y = tileY - TREE_LANE_CLEAR_RADIUS; y <= tileY + TREE_LANE_CLEAR_RADIUS; y++) {
            for (int x = tileX - TREE_LANE_CLEAR_RADIUS; x <= tileX + TREE_LANE_CLEAR_RADIUS; x++) {
                if (!map.inBounds(x, y) || !map.isLane(x, y)) {
                    continue;
                }
                if (tileDistance(tileX, tileY, x, y) <= TREE_LANE_CLEAR_RADIUS) {
                    return true;
                }
            }
        }
        return false;
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
        s.attackRange = scaleAttackRange(185);
        s.attackCooldown = 1.28;
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
        s.attackRange = scaleAttackRange(230);
        s.attackCooldown = 1.2;
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
        laneWaveNumber++;
        List<LaneCreepType> waveComposition = buildLaneWaveComposition(laneWaveNumber);
        for (LaneType lane : LaneType.values()) {
            for (Team team : List.of(Team.LIGHT, Team.DARK)) {
                for (int i = 0; i < waveComposition.size(); i++) {
                    spawnLaneCreep(team, lane, waveComposition.get(i), i, waveComposition.size());
                }
            }
        }
    }

    private List<LaneCreepType> buildLaneWaveComposition(int waveNumber) {
        List<LaneCreepType> composition = new ArrayList<>(List.of(
                LaneCreepType.MELEE,
                LaneCreepType.MELEE,
                LaneCreepType.MELEE,
                LaneCreepType.RANGED
        ));
        if (waveNumber % 5 == 0) {
            composition.add(LaneCreepType.CATAPULT);
        }
        return composition;
    }

    private void spawnLaneCreep(Team team, LaneType lane, LaneCreepType laneType, int idx, int waveSize) {
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
        double shift = (idx - (waveSize - 1) / 2.0) * 2.2;

        Creep creep = new Creep();
        creep.team = team;
        creep.role = CreepRole.LANE;
        creep.laneType = laneType;
        creep.lane = lane;
        creep.x = map.tileCenter(start.x) + perpX * shift * map.getTileSize();
        creep.y = map.tileCenter(start.y) + perpY * shift * map.getTileSize();
        configureLaneCreepStats(creep);
        creep.attackTimer = 0.12 * idx;
        creep.formationSlot = formationSlotForWaveIndex(idx, waveSize);
        creep.waypointIndex = 1;
        creep.animPhase = random.nextDouble() * 3.0;
        creep.lookAngle = Math.atan2(next.y - start.y, next.x - start.x);
        creep.laneNavigationGoalIndex = -1;
        creep.laneRepathCooldown = 0.0;
        laneCreeps.add(creep);
    }

    private void configureLaneCreepStats(Creep creep) {
        switch (creep.laneType) {
            case MELEE -> {
                creep.radius = STANDARD_UNIT_RADIUS;
                creep.maxHp = 58;
                creep.damage = 7;
                creep.defense = 1;
                creep.moveSpeed = 66;
                creep.attackRange = scaleAttackRange(31);
                creep.attackCooldown = 0.94;
            }
            case RANGED -> {
                creep.radius = STANDARD_UNIT_RADIUS * 0.92;
                creep.maxHp = 44;
                creep.damage = 10;
                creep.defense = 0;
                creep.moveSpeed = 63;
                creep.attackRange = scaleAttackRange(118);
                creep.attackCooldown = 1.16;
            }
            case CATAPULT -> {
                creep.radius = STANDARD_UNIT_RADIUS * 1.18;
                creep.maxHp = 112;
                creep.damage = 20;
                creep.defense = 2;
                creep.moveSpeed = 54;
                creep.attackRange = scaleAttackRange(168);
                creep.attackCooldown = 1.8;
            }
        }
        creep.hp = creep.maxHp;
    }

    private int formationSlotForWaveIndex(int idx, int waveSize) {
        int centered = idx - waveSize / 2;
        if (waveSize % 2 == 0 && centered >= 0) {
            return centered + 1;
        }
        return centered;
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastTickNanos) / 1_000_000_000.0, 0.033);
        lastTickNanos = now;

        if (editorMode) {
            editorStatusTimer = Math.max(0.0, editorStatusTimer - dt);
            updateCamera(dt);
        } else {
            updateWorld(dt);
        }
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
        audio.updatePlayerMovement(dt, moved, player.hp > 0 && !gameOver && player.attackAnimationTimer <= 0.0);
        updateBullets(dt);
        updateLaneCreeps(dt);
        updateNeutralCreeps(dt);
        resolveUnitCollisions();
        updateStructures(dt);
        updateExperienceOrbs(dt);
        updateClickMarkers(dt);

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
        if (playerAttackTarget != null) {
            return updatePlayerAttackOrder(dt);
        }

        if (playerMoveOrderActive) {
            return updatePlayerMoveOrder(dt);
        }

        return false;
    }

    private boolean updatePlayerMoveOrder(double dt) {
        boolean moved = followPlayerPath(dt, playerOrderX, playerOrderY, PLAYER_DESTINATION_REACHED_DISTANCE);
        if (distance(player.x, player.y, playerOrderX, playerOrderY) <= PLAYER_DESTINATION_REACHED_DISTANCE) {
            clearPlayerOrders();
        }
        return moved;
    }

    private boolean updatePlayerAttackOrder(double dt) {
        if (playerAttackTarget == null || !playerAttackTarget.isAlive()) {
            clearPlayerOrders();
            return false;
        }

        player.aimAngle = Math.atan2(playerAttackTarget.getY() - player.y, playerAttackTarget.getX() - player.x);
        double attackReach = desiredPlayerAttackReach(playerAttackTarget);
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

        return followPlayerPath(dt, playerPathFinalX, playerPathFinalY, PLAYER_ATTACK_DESTINATION_REACHED_DISTANCE);
    }

    private boolean followPlayerPath(double dt, double finalX, double finalY, double destinationReachedDistance) {
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

        if (distance(player.x, player.y, finalX, finalY) <= destinationReachedDistance) {
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

    private void issueAttackOrder(CombatEntity target) {
        clearPlayerOrders();
        if (gameOver || player.hp <= 0 || target == null || !target.isAlive()) {
            return;
        }

        playerAttackTarget = target;
        rebuildPlayerAttackPath(target);
    }

    private void addClickMarker(double worldX, double worldY, boolean attack) {
        ClickMarker marker = new ClickMarker();
        marker.x = worldX;
        marker.y = worldY;
        marker.attack = attack;
        marker.lifetime = CLICK_MARKER_LIFETIME;
        clickMarkers.add(marker);
    }

    private void rebuildPlayerAttackPath(CombatEntity target) {
        if (target == null || !target.isAlive()) {
            clearPlayerOrders();
            return;
        }

        double attackReach = desiredPlayerAttackReach(target);
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
                double dist = distance(map.tileCenter(tileX), map.tileCenter(tileY), target.getX(), target.getY());
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
            return playerWeaponReach()
                    + target.getRadius();
        }
        return playerWeaponReach() + target.getRadius();
    }

    private double desiredPlayerAttackReach(CombatEntity target) {
        return playerAttackReach(target) * PLAYER_ATTACK_APPROACH_RATIO;
    }

    private double playerWeaponReach() {
        if (currentWeapon.projectile()) {
            return currentWeapon.projectileSpeed() * currentWeapon.projectileLife()
                    * ATTACK_RANGE_BALANCE_SCALE
                    + currentWeapon.projectileRadius();
        }
        return currentWeapon.meleeRange() * ATTACK_RANGE_BALANCE_SCALE;
    }

    private double scaleAttackRange(double range) {
        return range * ATTACK_RANGE_BALANCE_SCALE;
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
            audio.resetPlayerMovementLoop();
        }
    }

    private void attackWithCurrentWeapon() {
        player.attackTimer = currentWeapon.attackAnimationTime();
        player.attackAnimationTimer = currentWeapon.attackAnimationTime();
        player.animPhase = 0.0;
        attackCooldown = currentWeapon.cooldown();
        audio.onPlayerAttack(currentWeapon);

        if (currentWeapon.projectile()) {
            fireProjectile(currentWeapon, playerAttackTarget);
            muzzleFlashTime = 0.06;
        } else {
            performMeleeAttack(currentWeapon);
            swordSwingTime = 0.10;
        }
    }

    private void fireProjectile(WeaponType weapon, CombatEntity target) {
        double aimAngle = player.aimAngle;
        if (target != null && target.isAlive()) {
            aimAngle = Math.atan2(target.getY() - player.y, target.getX() - player.x);
        }

        double dx = Math.cos(aimAngle);
        double dy = Math.sin(aimAngle);

        Bullet bullet = new Bullet();
        bullet.x = player.x + dx * (player.radius + 9);
        bullet.y = player.y + dy * (player.radius + 9);
        bullet.vx = dx * weapon.projectileSpeed();
        bullet.vy = dy * weapon.projectileSpeed();
        bullet.radius = weapon.projectileRadius();
        bullet.life = weapon.projectileLife() * ATTACK_RANGE_BALANCE_SCALE;
        bullet.damage = weapon.damage();
        bullet.colorArgb = weapon.projectileColorArgb();
        bullet.target = target;
        bullets.add(bullet);
    }

    private void performMeleeAttack(WeaponType weapon) {
        double arcHalf = Math.toRadians(weapon.meleeArcDegrees() / 2.0);
        double meleeRange = weapon.meleeRange() * ATTACK_RANGE_BALANCE_SCALE;
        boolean hitSomething = false;

        for (Player hero : heroes) {
            if (isHostileHero(hero) && inMeleeArc(hero.x, hero.y, meleeRange + hero.radius, arcHalf)) {
                damageHero(hero, weapon.damage());
                hitSomething = true;
            }
        }

        for (Creep creep : laneCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, meleeRange + creep.radius, arcHalf)) {
                damageCreepByHero(creep, weapon.damage());
                hitSomething = true;
            }
        }

        for (Creep creep : neutralCreeps) {
            if (isHostileCreep(creep) && inMeleeArc(creep.x, creep.y, meleeRange + creep.radius, arcHalf)) {
                damageCreepByHero(creep, weapon.damage());
                hitSomething = true;
            }
        }

        for (Structure structure : structures) {
            if (isHostileStructure(structure) && structure.hp > 0
                    && inMeleeArc(structure.x, structure.y, meleeRange + structure.radius, arcHalf)) {
                damageStructure(structure, weapon.damage());
                hitSomething = true;
            }
        }

        if (hitSomething) {
            audio.onMeleeImpact();
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
            if (bullet.target != null) {
                if (updateTargetedBullet(bullet, dt)) {
                    it.remove();
                }
                continue;
            }

            bullet.x += bullet.vx * dt;
            bullet.y += bullet.vy * dt;
            bullet.life -= dt;

            if (bullet.life <= 0.0 || map.isBlockedPixel(bullet.x, bullet.y)) {
                if (map.isBlockedPixel(bullet.x, bullet.y)) {
                    audio.onProjectileImpact();
                }
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
                    audio.onProjectileImpact();
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
                    audio.onProjectileImpact();
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
                        audio.onProjectileImpact();
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
                        audio.onProjectileImpact();
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

    private boolean updateTargetedBullet(Bullet bullet, double dt) {
        if (bullet.target == null) {
            return false;
        }
        if (!isValidPlayerProjectileTarget(bullet.target)) {
            return true;
        }

        double speed = Math.hypot(bullet.vx, bullet.vy);
        if (speed <= 0.0001) {
            return true;
        }

        double dx = bullet.target.getX() - bullet.x;
        double dy = bullet.target.getY() - bullet.y;
        double distanceToTarget = Math.hypot(dx, dy);
        double hitDistance = bullet.radius + bullet.target.getRadius();
        if (distanceToTarget <= hitDistance) {
            applyPlayerProjectileHit(bullet.target, bullet.damage);
            audio.onProjectileImpact();
            return true;
        }

        double dirX = dx / distanceToTarget;
        double dirY = dy / distanceToTarget;
        bullet.vx = dirX * speed;
        bullet.vy = dirY * speed;

        double travelDistance = speed * dt;
        if (travelDistance >= distanceToTarget - hitDistance) {
            bullet.x = bullet.target.getX() - dirX * hitDistance;
            bullet.y = bullet.target.getY() - dirY * hitDistance;
            applyPlayerProjectileHit(bullet.target, bullet.damage);
            audio.onProjectileImpact();
            return true;
        }

        bullet.x += bullet.vx * dt;
        bullet.y += bullet.vy * dt;
        bullet.life -= dt;
        if (bullet.life <= 0.0 || map.isBlockedPixel(bullet.x, bullet.y)) {
            if (map.isBlockedPixel(bullet.x, bullet.y)) {
                audio.onProjectileImpact();
            }
            return true;
        }
        return false;
    }

    private boolean isValidPlayerProjectileTarget(CombatEntity target) {
        return target != null
                && target.isAlive()
                && target.getTeam() != player.team;
    }

    private void applyPlayerProjectileHit(CombatEntity target, int damage) {
        if (target instanceof Player hero) {
            damageHero(hero, damage);
        } else if (target instanceof Creep creep) {
            damageCreepByHero(creep, damage);
        } else if (target instanceof Structure structure) {
            damageStructure(structure, damage);
        }
    }

    private void updateLaneCreeps(double dt) {
        Map<Creep, LaneGuidance> laneGuidance = buildLaneGuidanceMap();
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

            LaneGuidance guidance = laneGuidance.get(creep);
            Creep enemyCreep = findNearestEnemyLaneCreep(creep, laneCreepAggroRadius(creep));
            if (enemyCreep != null) {
                engageCreepTarget(creep, enemyCreep, guidance, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            Player heroTarget = findLaneHeroTarget(creep);
            if (heroTarget != null) {
                engageHeroTarget(creep, heroTarget, guidance, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            Structure enemyStructure = findLaneTargetStructure(creep);
            if (enemyStructure != null && enemyStructure.hp > 0
                    && distance(creep.x, creep.y, enemyStructure.x, enemyStructure.y) < laneStructureAggroRadius(creep, enemyStructure)) {
                if (enemyStructure.type == StructureType.TOWER && !canCommitToTowerAttack(creep, enemyStructure)) {
                    moveAlongLane(creep, guidance, dt);
                    updateCreepAnimation(creep, dt);
                    continue;
                }
                engageStructureTarget(creep, enemyStructure, guidance, dt);
                updateCreepAnimation(creep, dt);
                continue;
            }

            moveAlongLane(creep, guidance, dt);
            updateCreepAnimation(creep, dt);
        }
    }

    private void engageCreepTarget(Creep creep, Creep target, LaneGuidance guidance, double dt) {
        double dist = distance(creep.x, creep.y, target.x, target.y);
        if (dist > creep.attackRange + target.radius) {
            Point2D.Double approachPoint = findCombatApproachPoint(creep, target, creep.attackRange, CREEP_COMBAT_SLOT_ARC);
            steerCreepTowards(
                    creep,
                    approachPoint != null ? approachPoint.x : target.x,
                    approachPoint != null ? approachPoint.y : target.y,
                    guidance == null ? target.x - creep.x : guidance.dirX(),
                    guidance == null ? target.y - creep.y : guidance.dirY(),
                    dt
            );
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageCreepByCreep(target, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, target.x, target.y);
        }
    }

    private void engageCreepTarget(Creep creep, Creep target, double dt) {
        engageCreepTarget(creep, target, null, dt);
    }

    private void engageStructureTarget(Creep creep, Structure target, LaneGuidance guidance, double dt) {
        double dist = distance(creep.x, creep.y, target.x, target.y);
        if (dist > creep.attackRange + target.radius) {
            Point2D.Double approachPoint = findCombatApproachPoint(creep, target, creep.attackRange, CREEP_STRUCTURE_SLOT_ARC);
            if (approachPoint != null) {
                steerCreepTowards(
                        creep,
                        approachPoint.x,
                        approachPoint.y,
                        guidance == null ? target.x - creep.x : guidance.dirX(),
                        guidance == null ? target.y - creep.y : guidance.dirY(),
                        dt
                );
            } else {
                creep.moving = false;
            }
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageStructure(target, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, target.x, target.y);
        }
    }

    private void engageHeroTarget(Creep creep, Player hero, LaneGuidance guidance, double dt) {
        double dist = distance(creep.x, creep.y, hero.x, hero.y);
        if (dist > creep.attackRange + hero.radius) {
            Point2D.Double approachPoint = findCombatApproachPoint(creep, hero, creep.attackRange, CREEP_COMBAT_SLOT_ARC);
            steerCreepTowards(
                    creep,
                    approachPoint != null ? approachPoint.x : hero.x,
                    approachPoint != null ? approachPoint.y : hero.y,
                    guidance == null ? hero.x - creep.x : guidance.dirX(),
                    guidance == null ? hero.y - creep.y : guidance.dirY(),
                    dt
            );
            return;
        }

        if (creep.attackTimer <= 0.0) {
            damageHero(hero, creep.damage);
            creep.attackTimer = creep.attackCooldown;
            triggerUnitAttackAnimation(creep, hero.x, hero.y);
        }
    }

    private void engageHeroTarget(Creep creep, Player hero, double dt) {
        engageHeroTarget(creep, hero, null, dt);
    }

    private void moveAlongLane(Creep creep, LaneGuidance guidance, double dt) {
        if (guidance == null) {
            creep.moving = false;
            return;
        }
        steerCreepTowards(creep, guidance.targetX(), guidance.targetY(), guidance.dirX(), guidance.dirY(), dt);
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

    private Point2D.Double projectLaneFormationPoint(Creep creep, double baseX, double baseY, double dirX, double dirY) {
        double len = Math.hypot(dirX, dirY);
        if (len < 0.0001) {
            return canOccupy(creep, baseX, baseY) ? new Point2D.Double(baseX, baseY) : null;
        }

        double perpX = -dirY / len;
        double perpY = dirX / len;
        double[] scales = {1.0, 0.6, 0.3, 0.0};
        for (double scale : scales) {
            double offset = creep.formationSlot * CREEP_LANE_SLOT_SPACING * scale;
            double candidateX = baseX + perpX * offset;
            double candidateY = baseY + perpY * offset;
            if (canOccupy(creep, candidateX, candidateY)) {
                return new Point2D.Double(candidateX, candidateY);
            }
        }

        return null;
    }

    private Map<Creep, LaneGuidance> buildLaneGuidanceMap() {
        Map<Creep, LaneGuidance> guidance = new IdentityHashMap<>();
        int[] lateralPattern = {0};
        double rowSpacing = STANDARD_UNIT_RADIUS * 2.9;

        for (Team team : List.of(Team.LIGHT, Team.DARK)) {
            EnumMap<LaneType, List<Point>> teamPaths = lanePaths.get(team);
            if (teamPaths == null) {
                continue;
            }

            for (LaneType lane : LaneType.values()) {
                List<Point> path = teamPaths.get(lane);
                if (path == null || path.size() < 2) {
                    continue;
                }

                List<CreepLaneProgress> group = new ArrayList<>();
                for (Creep creep : laneCreeps) {
                    if (creep.hp > 0 && creep.team == team && creep.lane == lane) {
                        group.add(new CreepLaneProgress(creep, sampleLaneProgress(creep.x, creep.y, path)));
                    }
                }
                if (group.isEmpty()) {
                    continue;
                }

                group.sort((left, right) -> Double.compare(right.sample().progress(), left.sample().progress()));
                double frontProgress = Math.min(
                        group.get(0).sample().totalLength(),
                        group.get(0).sample().progress() + CREEP_LANE_LOOKAHEAD
                );
                for (int index = 0; index < group.size(); index++) {
                    CreepLaneProgress creepProgress = group.get(index);
                    int row = index / lateralPattern.length;
                    int lateralSlot = lateralPattern[index % lateralPattern.length];
                    double targetProgress = Math.min(
                            creepProgress.sample().progress() + CREEP_LANE_LOOKAHEAD,
                            frontProgress - row * rowSpacing
                    );
                    double minProgress = Math.max(0.0, creepProgress.sample().progress() - STANDARD_UNIT_RADIUS * 0.35);
                    targetProgress = clamp(targetProgress, minProgress, creepProgress.sample().totalLength());

                    LaneAnchor anchor = laneAnchorAtProgress(path, targetProgress);
                    Point2D.Double formationTarget = projectLaneOffsetPoint(
                            creepProgress.creep(),
                            anchor.x(),
                            anchor.y(),
                            anchor.dirX(),
                            anchor.dirY(),
                            lateralSlot * CREEP_LANE_SLOT_SPACING
                    );
                    if (formationTarget == null) {
                        formationTarget = new Point2D.Double(anchor.x(), anchor.y());
                    }

                    guidance.put(
                            creepProgress.creep(),
                            new LaneGuidance(formationTarget.x, formationTarget.y, anchor.dirX(), anchor.dirY())
                    );
                }
            }
        }

        return guidance;
    }

    private boolean canCommitToTowerAttack(Creep creep, Structure tower) {
        int attackersAhead = 0;
        double creepDistance = distance(creep.x, creep.y, tower.x, tower.y);

        for (Creep other : laneCreeps) {
            if (other == creep || other.hp <= 0 || other.team != creep.team || other.lane != creep.lane) {
                continue;
            }

            if (distance(other.x, other.y, tower.x, tower.y) < creepDistance) {
                attackersAhead++;
                if (attackersAhead >= MAX_CREEPS_ATTACKING_TOWER) {
                    return false;
                }
            }
        }

        return true;
    }

    private LaneSample sampleLaneProgress(double worldX, double worldY, List<Point> path) {
        double bestDistance = Double.MAX_VALUE;
        double bestProgress = 0.0;
        double totalLength = 0.0;
        double traversed = 0.0;

        for (int i = 1; i < path.size(); i++) {
            double startX = map.tileCenter(path.get(i - 1).x);
            double startY = map.tileCenter(path.get(i - 1).y);
            double endX = map.tileCenter(path.get(i).x);
            double endY = map.tileCenter(path.get(i).y);
            double segmentX = endX - startX;
            double segmentY = endY - startY;
            double segmentLength = Math.hypot(segmentX, segmentY);
            if (segmentLength < 0.001) {
                continue;
            }

            double projection = ((worldX - startX) * segmentX + (worldY - startY) * segmentY)
                    / (segmentLength * segmentLength);
            projection = clamp(projection, 0.0, 1.0);
            double closestX = startX + segmentX * projection;
            double closestY = startY + segmentY * projection;
            double distanceToSegment = distance(worldX, worldY, closestX, closestY);
            if (distanceToSegment < bestDistance) {
                bestDistance = distanceToSegment;
                bestProgress = traversed + segmentLength * projection;
            }

            traversed += segmentLength;
            totalLength = traversed;
        }

        return new LaneSample(bestProgress, totalLength);
    }

    private LaneAnchor laneAnchorAtProgress(List<Point> path, double progress) {
        double traversed = 0.0;

        for (int i = 1; i < path.size(); i++) {
            double startX = map.tileCenter(path.get(i - 1).x);
            double startY = map.tileCenter(path.get(i - 1).y);
            double endX = map.tileCenter(path.get(i).x);
            double endY = map.tileCenter(path.get(i).y);
            double segmentX = endX - startX;
            double segmentY = endY - startY;
            double segmentLength = Math.hypot(segmentX, segmentY);
            if (segmentLength < 0.001) {
                continue;
            }

            double nextTraversed = traversed + segmentLength;
            if (progress <= nextTraversed || i == path.size() - 1) {
                double localRatio = clamp((progress - traversed) / segmentLength, 0.0, 1.0);
                return new LaneAnchor(
                        startX + segmentX * localRatio,
                        startY + segmentY * localRatio,
                        segmentX,
                        segmentY
                );
            }
            traversed = nextTraversed;
        }

        Point last = path.get(path.size() - 1);
        Point previous = path.get(path.size() - 2);
        return new LaneAnchor(
                map.tileCenter(last.x),
                map.tileCenter(last.y),
                map.tileCenter(last.x) - map.tileCenter(previous.x),
                map.tileCenter(last.y) - map.tileCenter(previous.y)
        );
    }

    private Point2D.Double projectLaneOffsetPoint(Creep creep, double baseX, double baseY, double dirX, double dirY, double offset) {
        double len = Math.hypot(dirX, dirY);
        if (len < 0.0001) {
            return canOccupy(creep, baseX, baseY) ? new Point2D.Double(baseX, baseY) : null;
        }

        double perpX = -dirY / len;
        double perpY = dirX / len;
        double[] scales = {1.0, 0.6, 0.3, 0.0};
        for (double scale : scales) {
            double candidateX = baseX + perpX * offset * scale;
            double candidateY = baseY + perpY * offset * scale;
            if (canOccupy(creep, candidateX, candidateY)) {
                return new Point2D.Double(candidateX, candidateY);
            }
        }

        return null;
    }

    private void steerCreepTowards(Creep creep, double targetX, double targetY, double laneDirX, double laneDirY, double dt) {
        double desiredX = targetX - creep.x;
        double desiredY = targetY - creep.y;
        double desiredLength = Math.hypot(desiredX, desiredY);
        if (desiredLength < 0.001) {
            creep.moving = false;
            return;
        }

        double desiredNX = desiredX / desiredLength;
        double desiredNY = desiredY / desiredLength;
        double separationX = 0.0;
        double separationY = 0.0;
        for (Creep other : laneCreeps) {
            if (other == creep || other.hp <= 0 || other.team != creep.team || other.lane != creep.lane) {
                continue;
            }

            double dx = creep.x - other.x;
            double dy = creep.y - other.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 0.001 || dist >= CREEP_SEPARATION_RANGE) {
                continue;
            }

            double strength = (CREEP_SEPARATION_RANGE - dist) / CREEP_SEPARATION_RANGE;
            separationX += dx / dist * strength;
            separationY += dy / dist * strength;
        }

        double structureAvoidX = 0.0;
        double structureAvoidY = 0.0;
        for (Structure structure : structures) {
            if (structure.hp <= 0) {
                continue;
            }

            double dx = creep.x - structure.x;
            double dy = creep.y - structure.y;
            double dist = Math.hypot(dx, dy);
            double safeDistance = structure.radius + creep.radius + CREEP_STRUCTURE_AVOID_RANGE;
            if (dist < 0.001 || dist >= safeDistance) {
                continue;
            }

            double strength = (safeDistance - dist) / safeDistance;
            structureAvoidX += dx / dist * strength;
            structureAvoidY += dy / dist * strength;
        }

        double laneForwardLength = Math.hypot(laneDirX, laneDirY);
        double forwardX = laneForwardLength < 0.001 ? 0.0 : laneDirX / laneForwardLength;
        double forwardY = laneForwardLength < 0.001 ? 0.0 : laneDirY / laneForwardLength;

        double steerX = desiredNX * CREEP_TARGET_WEIGHT
                + separationX * CREEP_SEPARATION_WEIGHT
                + structureAvoidX * CREEP_STRUCTURE_AVOID_WEIGHT
                + forwardX * CREEP_FORWARD_WEIGHT;
        double steerY = desiredNY * CREEP_TARGET_WEIGHT
                + separationY * CREEP_SEPARATION_WEIGHT
                + structureAvoidY * CREEP_STRUCTURE_AVOID_WEIGHT
                + forwardY * CREEP_FORWARD_WEIGHT;
        double steerLength = Math.hypot(steerX, steerY);
        if (steerLength < 0.001) {
            steerX = desiredNX;
            steerY = desiredNY;
            steerLength = 1.0;
        }

        double step = Math.min(creep.moveSpeed * dt, desiredLength);
        creep.lookAngle = Math.atan2(steerY, steerX);
        creep.moving = moveCombatUnit(creep, steerX / steerLength * step, steerY / steerLength * step);
    }

    private Point2D.Double findCombatApproachPoint(Creep creep, CombatEntity target, double desiredRange, double slotArc) {
        double desiredDistance = Math.max(
                target.getRadius() + creep.radius + 1.0,
                target.getRadius() + desiredRange - CREEP_STRUCTURE_APPROACH_BUFFER
        );
        double preferredAngle = Math.atan2(creep.y - target.getY(), creep.x - target.getX());
        double slotAngle = preferredAngle + creep.formationSlot * slotArc;
        Point2D.Double bestPoint = null;
        double bestScore = Double.POSITIVE_INFINITY;

        double[] radii = {
                desiredDistance,
                desiredDistance + creep.radius * 0.8,
                desiredDistance + creep.radius * 1.6
        };
        for (double radius : radii) {
            for (int i = 0; i < CREEP_STRUCTURE_APPROACH_SAMPLES; i++) {
                double angle = (Math.PI * 2.0 * i) / CREEP_STRUCTURE_APPROACH_SAMPLES;
                double candidateX = target.getX() + Math.cos(angle) * radius;
                double candidateY = target.getY() + Math.sin(angle) * radius;
                if (!canOccupy(creep, candidateX, candidateY)) {
                    continue;
                }

                double anglePenalty = Math.abs(normalizeAngle(angle - slotAngle)) * radius * 0.45;
                double score = distance(creep.x, creep.y, candidateX, candidateY) + anglePenalty;
                if (score < bestScore) {
                    bestScore = score;
                    bestPoint = new Point2D.Double(candidateX, candidateY);
                }
            }

            if (bestPoint != null) {
                return bestPoint;
            }
        }

        return null;
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
                structure.attackTarget = null;
                continue;
            }

            structure.attackTimer = Math.max(0.0, structure.attackTimer - dt);
            structure.attackAnimationTimer = Math.max(0.0, structure.attackAnimationTimer - dt);
            if (!isValidStructureTarget(structure, structure.attackTarget)) {
                structure.attackTarget = null;
            }

            if (structure.attackTarget == null) {
                structure.attackTarget = acquireStructureTarget(structure);
            }

            if (structure.attackTimer > 0.0 || structure.attackTarget == null) {
                continue;
            }

            audio.onStructureAttack();
            triggerStructureAttackAnimation(structure, structure.attackTarget);
            damageStructureTarget(structure, structure.attackTarget);
            structure.attackTimer = structure.attackCooldown;
        }
    }

    private CombatEntity acquireStructureTarget(Structure structure) {
        Team enemyTeam = structure.team.opposite();
        List<CombatEntity> candidates = new ArrayList<>();

        for (Creep creep : laneCreeps) {
            if (creep.hp <= 0 || creep.team != enemyTeam) {
                continue;
            }
            if (distance(structure.x, structure.y, creep.x, creep.y) <= structure.attackRange) {
                candidates.add(creep);
            }
        }

        if (!gameOver) {
            for (Player hero : heroes) {
                if (hero.hp <= 0 || hero.team != enemyTeam) {
                    continue;
                }
                if (distance(structure.x, structure.y, hero.x, hero.y) <= structure.attackRange) {
                    candidates.add(hero);
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean isValidStructureTarget(Structure structure, CombatEntity target) {
        return target != null
                && target.isAlive()
                && target.getTeam() == structure.team.opposite()
                && distance(structure.x, structure.y, target.getX(), target.getY()) <= structure.attackRange;
    }

    private void damageStructureTarget(Structure structure, CombatEntity target) {
        if (target instanceof Creep creep) {
            damageCreepByStructure(creep, structure.damage);
        } else if (target instanceof Player hero) {
            damageHero(hero, structure.damage);
        }
    }

    private void triggerStructureAttackAnimation(Structure structure, CombatEntity target) {
        if (target == null) {
            return;
        }
        structure.attackAnimationTimer = STRUCTURE_ATTACK_ANIMATION_DURATION;
        structure.attackVisualTargetX = target.getX();
        structure.attackVisualTargetY = target.getY();
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
        unit.setAttackAnimationTimer(UNIT_ATTACK_ANIMATION_DURATION);
        unit.setAnimPhase(0.0);
    }

    private void updateCreepAnimation(Creep creep, double dt) {
        if (creep.attackAnimationTimer > 0.0) {
            creep.state = AnimationState.ATTACK;
            creep.animPhase += dt * UNIT_ATTACK_ANIMATION_PHASE_SPEED;
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

        Player hero = findNearestHeroByTeam(creep.x, creep.y, laneHeroAggroRadius(creep), creep.team.opposite());
        if (hero == null) {
            return null;
        }
        return findNearestLaneCreepByTeam(hero.x, hero.y, 96.0, hero.team) == null ? hero : null;
    }

    private double laneCreepAggroRadius(Creep creep) {
        return Math.max(130.0, creep.attackRange + STANDARD_UNIT_RADIUS * 2.0);
    }

    private double laneHeroAggroRadius(Creep creep) {
        return Math.max(140.0, creep.attackRange + STANDARD_UNIT_RADIUS * 2.0);
    }

    private double laneStructureAggroRadius(Creep creep, Structure structure) {
        return Math.max(180.0, creep.attackRange + structure.radius + 24.0);
    }

    private double structureAttackProgress(Structure structure) {
        return 1.0 - clamp(structure.attackAnimationTimer / STRUCTURE_ATTACK_ANIMATION_DURATION, 0.0, 1.0);
    }

    private double jellyWobble(double progress) {
        return Math.cos(progress * Math.PI * 2.8) * (1.0 - progress);
    }

    private void damageCreepByHero(Creep creep, int damage) {
        creep.lastHitByHero = true;
        creep.lastHitByCreep = false;
        damageEntity(creep, damage);
        if (creep.hp <= 0) {
            audio.onEnemyDown();
        }
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
        int previousHp = structure.hp;
        damageEntity(structure, damage);
        if (previousHp > 0 && structure.hp <= 0) {
            audio.onEnemyDown();
        }
    }

    private void damageHero(Player hero, int damage) {
        if (hero.hp <= 0 || hero.hitCooldown > 0.0) {
            return;
        }
        hero.hitCooldown = 0.35;
        damageEntity(hero, damage);
        if (hero == player) {
            audio.onPlayerHit();
        } else if (hero.hp <= 0) {
            audio.onEnemyDown();
        }
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

    private void updateClickMarkers(double dt) {
        Iterator<ClickMarker> it = clickMarkers.iterator();
        while (it.hasNext()) {
            ClickMarker marker = it.next();
            marker.lifetime -= dt;
            if (marker.lifetime <= 0.0) {
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
            audio.onDefeat();
        } else if (darkThrone.hp <= 0) {
            gameOver = true;
            victoryText = "Силы света победили";
            audio.onVictory();
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

    private CombatEntity findClickedHostileTarget(double worldX, double worldY) {
        CombatEntity target = null;
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

        for (Player hero : heroes) {
            if (!isHostileHero(hero)) {
                continue;
            }

            double dist = distance(worldX, worldY, hero.x, hero.y);
            if (dist <= hero.radius + CLICK_TARGET_PADDING && dist < bestDistance) {
                bestDistance = dist;
                target = hero;
            }
        }

        for (Structure structure : structures) {
            if (!isHostileStructure(structure)) {
                continue;
            }

            double dist = distance(worldX, worldY, structure.x, structure.y);
            if (dist <= structure.radius + CLICK_TARGET_PADDING && dist < bestDistance) {
                bestDistance = dist;
                target = structure;
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
        audio.onWeaponSwitch();
        attackCooldown = Math.min(attackCooldown, 0.12);
        refreshPlayerAttackOrderForCurrentWeapon();
    }

    private void refreshPlayerAttackOrderForCurrentWeapon() {
        playerPathRefreshCooldown = 0.0;
        if (playerAttackTarget == null || !playerAttackTarget.isAlive()) {
            return;
        }

        double attackReach = desiredPlayerAttackReach(playerAttackTarget);
        player.aimAngle = Math.atan2(playerAttackTarget.getY() - player.y, playerAttackTarget.getX() - player.x);
        if (canPlayerAttackTargetFrom(player.x, player.y, playerAttackTarget, attackReach)) {
            clearPlayerPath();
            return;
        }

        rebuildPlayerAttackPath(playerAttackTarget);
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

        if (mouseInsideWindow && !middleMouseDragging && !miniMapDragging
                && !(editorMode && isInsideEditorPanel(mouseX, mouseY))) {
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

    private boolean isMiniMapCameraButton(int button) {
        return button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3;
    }

    private boolean isInsideMiniMap(int screenX, int screenY) {
        HudRenderer.MiniMapBounds bounds = HudRenderer.miniMapBounds(viewportHeight());
        return screenX >= bounds.mapX()
                && screenX <= bounds.mapX() + bounds.mapW()
                && screenY >= bounds.mapY()
                && screenY <= bounds.mapY() + bounds.mapH();
    }

    private void centerCameraOnMiniMapPoint(int screenX, int screenY) {
        HudRenderer.MiniMapBounds bounds = HudRenderer.miniMapBounds(viewportHeight());
        double relX = clamp(screenX - bounds.mapX(), 0.0, bounds.mapW());
        double relY = clamp(screenY - bounds.mapY(), 0.0, bounds.mapH());
        double worldX = relX / bounds.mapW() * map.getPixelWidth();
        double worldY = relY / bounds.mapH() * map.getPixelHeight();
        double visibleWorldW = viewportWidth() / ZOOM;
        double visibleWorldH = viewportHeight() / ZOOM;
        cameraX = clamp(worldX - visibleWorldW / 2.0, 0.0, Math.max(0.0, map.getPixelWidth() - visibleWorldW));
        cameraY = clamp(worldY - visibleWorldH / 2.0, 0.0, Math.max(0.0, map.getPixelHeight() - visibleWorldH));
    }

    private int laneBit(LaneType laneType) {
        return switch (laneType) {
            case TOP -> 1;
            case MID -> 1 << 1;
            case BOT -> 1 << 2;
        };
    }

    private Rectangle editorPanelBounds() {
        int panelHeight = Math.min(viewportHeight() - EDITOR_PANEL_MARGIN * 2, 596);
        int panelX = shouldDockEditorPanelLeft()
                ? EDITOR_PANEL_MARGIN
                : viewportWidth() - EDITOR_PANEL_WIDTH - EDITOR_PANEL_MARGIN;
        return new Rectangle(
                panelX,
                EDITOR_PANEL_MARGIN,
                EDITOR_PANEL_WIDTH,
                panelHeight
        );
    }

    private boolean shouldDockEditorPanelLeft() {
        double visibleWorldW = viewportWidth() / ZOOM;
        double maxCameraX = Math.max(0.0, map.getPixelWidth() - visibleWorldW);
        return maxCameraX > 0.0 && cameraX >= maxCameraX - EDITOR_PANEL_EDGE_SWITCH_DISTANCE;
    }

    private boolean isInsideEditorPanel(int screenX, int screenY) {
        return editorMode && editorPanelBounds().contains(screenX, screenY);
    }

    private EditorUi buildEditorUi() {
        Rectangle panelBounds = editorPanelBounds();
        List<EditorButton> buttons = new ArrayList<>();
        List<EditorLabel> labels = new ArrayList<>();

        int x = panelBounds.x + EDITOR_PANEL_PAD;
        int y = panelBounds.y + EDITOR_PANEL_PAD + 20;
        int contentW = panelBounds.width - EDITOR_PANEL_PAD * 2;

        labels.add(new EditorLabel("Editor", x, panelBounds.y + 18));
        String status = editorStatusTimer > 0.0 ? editorStatusText : (editorDirty ? "Unsaved changes" : "Ctrl/Cmd+S to save");
        labels.add(new EditorLabel(status, x, panelBounds.y + 36));

        int wideW = (contentW - EDITOR_BUTTON_GAP * 2) / 3;
        buttons.add(new EditorButton("tool:paint", new Rectangle(x, y, wideW, EDITOR_BUTTON_HEIGHT), "Paint",
                editorTool == EditorTool.PAINT, new Color(76, 124, 108), Color.WHITE));
        buttons.add(new EditorButton("tool:move", new Rectangle(x + wideW + EDITOR_BUTTON_GAP, y, wideW, EDITOR_BUTTON_HEIGHT), "Move",
                editorTool == EditorTool.MOVE_STRUCTURE, new Color(110, 98, 70), Color.WHITE));
        buttons.add(new EditorButton("save", new Rectangle(x + (wideW + EDITOR_BUTTON_GAP) * 2, y, wideW, EDITOR_BUTTON_HEIGHT), "Save",
                false, new Color(62, 86, 132), Color.WHITE));

        y += EDITOR_BUTTON_HEIGHT + 18;
        labels.add(new EditorLabel("Ground", x, y - 6));
        y = addEditorSwatchGrid(buttons, x, y, contentW, List.of(
                new EditorButton("ground:grass", null, "Grass", editorGround == MapElements.GRASS, new Color(102, 146, 86), Color.BLACK),
                new EditorButton("ground:grass_alt", null, "Grass+", editorGround == MapElements.GRASS_ALT, new Color(88, 131, 74), Color.WHITE),
                new EditorButton("ground:dirt", null, "Dirt", editorGround == MapElements.DIRT, new Color(125, 103, 74), Color.WHITE),
                new EditorButton("ground:high", null, "High", editorGround == MapElements.HIGH_GROUND, new Color(126, 160, 102), Color.BLACK),
                new EditorButton("ground:base", null, "Base", editorGround == MapElements.BASE, new Color(126, 118, 108), Color.WHITE),
                new EditorButton("ground:forest", null, "Forest", editorGround == MapElements.FOREST, new Color(86, 128, 76), Color.WHITE)
        ), 3);

        labels.add(new EditorLabel("Water", x, y - 6));
        y = addEditorSwatchGrid(buttons, x, y, contentW, List.of(
                new EditorButton("water:none", null, "Off", editorWater == null, new Color(58, 58, 58), Color.WHITE),
                new EditorButton("water:river", null, "River", editorWater != null, new Color(74, 120, 160), Color.WHITE)
        ), 2);

        labels.add(new EditorLabel("Props", x, y - 6));
        y = addEditorSwatchGrid(buttons, x, y, contentW, List.of(
                new EditorButton("prop:none", null, "None", editorProp == null, new Color(56, 56, 56), Color.WHITE),
                new EditorButton("prop:boulder", null, "Boulder", editorProp == MapElements.BOULDER, new Color(82, 86, 92), Color.WHITE),
                new EditorButton("prop:rock", null, "Rock", editorProp == MapElements.ROCK, new Color(104, 108, 114), Color.WHITE),
                new EditorButton("prop:bush", null, "Bush", editorProp == MapElements.BUSH, new Color(54, 118, 64), Color.WHITE),
                new EditorButton("prop:stump", null, "Stump", editorProp == MapElements.STUMP, new Color(130, 96, 60), Color.WHITE),
                new EditorButton("prop:pebbles", null, "Pebbles", editorProp == MapElements.PEBBLES, new Color(132, 123, 108), Color.BLACK)
        ), 3);

        labels.add(new EditorLabel("Lane", x, y - 6));
        y = addEditorSwatchGrid(buttons, x, y, contentW, List.of(
                new EditorButton("lane:top", null, "Top", (editorLaneMask & laneBit(LaneType.TOP)) != 0, new Color(94, 128, 184), Color.WHITE),
                new EditorButton("lane:mid", null, "Mid", (editorLaneMask & laneBit(LaneType.MID)) != 0, new Color(154, 112, 168), Color.WHITE),
                new EditorButton("lane:bot", null, "Bot", (editorLaneMask & laneBit(LaneType.BOT)) != 0, new Color(190, 128, 84), Color.WHITE)
        ), 3);

        labels.add(new EditorLabel("Blocked", x, y - 6));
        y = addEditorSwatchGrid(buttons, x, y, contentW, List.of(
                new EditorButton("blocked:off", null, "Off", !editorBlocked, new Color(68, 68, 68), Color.WHITE),
                new EditorButton("blocked:on", null, "On", editorBlocked, new Color(92, 116, 78), Color.WHITE)
        ), 2);

        Rectangle previewBounds = new Rectangle(x, y + 4, contentW, 86);
        return new EditorUi(panelBounds, previewBounds, labels, buttons);
    }

    private int addEditorSwatchGrid(List<EditorButton> buttons, int x, int y, int width, List<EditorButton> template, int columns) {
        int buttonW = (width - EDITOR_BUTTON_GAP * (columns - 1)) / columns;
        int currentY = y;
        for (int i = 0; i < template.size(); i++) {
            EditorButton button = template.get(i);
            int col = i % columns;
            int row = i / columns;
            int buttonX = x + col * (buttonW + EDITOR_BUTTON_GAP);
            int buttonY = y + row * (EDITOR_SWATCH_SIZE + EDITOR_BUTTON_GAP);
            buttons.add(new EditorButton(
                    button.id(),
                    new Rectangle(buttonX, buttonY, buttonW, EDITOR_SWATCH_SIZE),
                    button.label(),
                    button.selected(),
                    button.fill(),
                    button.text()
            ));
            currentY = buttonY + EDITOR_SWATCH_SIZE;
        }
        return currentY + 18;
    }

    private void drawEditorOverlay(Graphics2D g2) {
        EditorUi ui = buildEditorUi();
        Rectangle panel = ui.panelBounds();

        g2.setColor(new Color(0, 0, 0, 188));
        g2.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 16, 16);
        g2.setColor(new Color(232, 232, 232, 150));
        g2.drawRoundRect(panel.x, panel.y, panel.width, panel.height, 16, 16);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        for (EditorLabel label : ui.labels()) {
            g2.setColor(label.text().equals("Editor") ? Color.WHITE : new Color(222, 222, 222));
            g2.drawString(label.text(), label.x(), label.y());
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (EditorButton button : ui.buttons()) {
            drawEditorButton(g2, button);
        }

        Rectangle preview = ui.previewBounds();
        g2.setColor(new Color(16, 16, 16, 180));
        g2.fillRoundRect(preview.x, preview.y, preview.width, preview.height, 12, 12);
        g2.setColor(new Color(220, 220, 220, 110));
        g2.drawRoundRect(preview.x, preview.y, preview.width, preview.height, 12, 12);
        g2.setColor(groundPreviewColor(editorGround));
        g2.fillRoundRect(preview.x + 12, preview.y + 14, 48, 48, 10, 10);
        if (editorWater != null) {
            g2.setColor(new Color(88, 150, 194, 170));
            g2.fillRoundRect(preview.x + 12, preview.y + 14, 48, 48, 10, 10);
        }
        if (editorBlocked) {
            g2.setColor(new Color(40, 92, 48, 170));
            g2.fillOval(preview.x + 20, preview.y + 20, 32, 28);
        }
        if (editorProp != null) {
            g2.setColor(new Color(245, 245, 245, 220));
            g2.drawString(propPreviewLabel(editorProp), preview.x + 22, preview.y + 45);
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Brush", preview.x + 72, preview.y + 24);
        g2.drawString("Lane: " + laneMaskLabel(editorLaneMask), preview.x + 72, preview.y + 42);
        g2.drawString("Blocked: " + (editorBlocked ? "yes" : "no"), preview.x + 72, preview.y + 58);
        g2.drawString("Tab toggle, Ctrl/Cmd+S save", preview.x + 12, preview.y + 76);
    }

    private void drawEditorButton(Graphics2D g2, EditorButton button) {
        Color fill = button.selected() ? brighten(button.fill(), 18) : button.fill();
        g2.setColor(fill);
        g2.fillRoundRect(button.bounds().x, button.bounds().y, button.bounds().width, button.bounds().height, 10, 10);
        g2.setColor(button.selected() ? Color.WHITE : new Color(220, 220, 220, 180));
        g2.drawRoundRect(button.bounds().x, button.bounds().y, button.bounds().width, button.bounds().height, 10, 10);

        g2.setColor(button.text());
        int textY = button.bounds().y + button.bounds().height / 2 + 4;
        int textX = button.bounds().x + 8;
        g2.drawString(button.label(), textX, textY);
    }

    private void drawEditorWorldOverlay(Graphics2D g2) {
        if (isInsideEditorPanel(mouseX, mouseY) || isInsideMiniMap(mouseX, mouseY)) {
            return;
        }

        int tileX = clampTile((int) (screenToWorldX(mouseX) / map.getTileSize()), map.getWidth());
        int tileY = clampTile((int) (screenToWorldY(mouseY) / map.getTileSize()), map.getHeight());
        int sx = worldToScreenX(tileX * map.getTileSize());
        int sy = worldToScreenY(tileY * map.getTileSize());
        int size = (int) Math.round(map.getTileSize() * ZOOM);

        g2.setColor(new Color(255, 240, 160, 190));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRect(sx, sy, size, size);

        if (editorDraggedStructure != null) {
            int px = worldToScreenX(editorDraggedStructure.x);
            int py = worldToScreenY(editorDraggedStructure.y);
            int r = (int) Math.round(editorDraggedStructure.radius * ZOOM);
            g2.setColor(new Color(255, 218, 117, 180));
            g2.drawOval(px - r, py - r, r * 2, r * 2);
        }
    }

    private void toggleEditorMode() {
        editorMode = !editorMode;
        editorPainting = false;
        editorDraggedStructure = null;
        miniMapDragging = false;
        middleMouseDragging = false;
        clearPlayerOrders();
        setEditorStatus(editorMode ? "Editor enabled" : "Editor disabled", 1.4);
    }

    private void saveEditedMap() {
        try {
            EnumMap<LaneType, List<Point>> baseLanePaths = new EnumMap<>(LaneType.class);
            for (LaneType lane : LaneType.values()) {
                baseLanePaths.put(lane, new ArrayList<>(lanePaths.get(Team.LIGHT).get(lane)));
            }
            mapBlueprintWriter.save(MAP_EDITOR_SAVE_PATH, map, mapBlueprint.playerStart(), baseLanePaths, structures);
            editorDirty = false;
            setEditorStatus("Map saved to default.map", 2.5);
        } catch (IOException e) {
            setEditorStatus("Save failed: " + e.getMessage(), 3.5);
        }
    }

    private boolean isEditorSaveShortcut(KeyEvent e) {
        return e.getKeyCode() == KeyEvent.VK_S && (e.isControlDown() || e.isMetaDown());
    }

    private void setEditorStatus(String text, double duration) {
        editorStatusText = text;
        editorStatusTimer = duration;
    }

    private boolean handleEditorPanelClick(int screenX, int screenY) {
        for (EditorButton button : buildEditorUi().buttons()) {
            if (button.bounds().contains(screenX, screenY)) {
                applyEditorAction(button.id());
                return true;
            }
        }
        return isInsideEditorPanel(screenX, screenY);
    }

    private void applyEditorAction(String actionId) {
        if (actionId.equals("save")) {
            saveEditedMap();
            return;
        }
        String[] parts = actionId.split(":", 2);
        if (parts.length != 2) {
            return;
        }

        switch (parts[0]) {
            case "tool" -> editorTool = "move".equals(parts[1]) ? EditorTool.MOVE_STRUCTURE : EditorTool.PAINT;
            case "ground" -> {
                editorGround = switch (parts[1]) {
                    case "grass" -> MapElements.GRASS;
                    case "grass_alt" -> MapElements.GRASS_ALT;
                    case "dirt" -> MapElements.DIRT;
                    case "high" -> MapElements.HIGH_GROUND;
                    case "base" -> MapElements.BASE;
                    case "forest" -> MapElements.FOREST;
                    default -> editorGround;
                };
                if (editorGround == MapElements.FOREST) {
                    editorBlocked = true;
                    editorProp = null;
                    editorWater = null;
                }
            }
            case "water" -> editorWater = "river".equals(parts[1]) ? MapElements.RIVER : null;
            case "prop" -> {
                editorProp = switch (parts[1]) {
                    case "boulder" -> MapElements.BOULDER;
                    case "rock" -> MapElements.ROCK;
                    case "bush" -> MapElements.BUSH;
                    case "stump" -> MapElements.STUMP;
                    case "pebbles" -> MapElements.PEBBLES;
                    default -> null;
                };
                if (isImpassableProp(editorProp)) {
                    editorBlocked = true;
                    editorWater = null;
                }
            }
            case "lane" -> {
                LaneType lane = switch (parts[1]) {
                    case "top" -> LaneType.TOP;
                    case "mid" -> LaneType.MID;
                    case "bot" -> LaneType.BOT;
                    default -> null;
                };
                if (lane != null) {
                    int bit = laneBit(lane);
                    editorLaneMask = (editorLaneMask & bit) != 0 ? editorLaneMask & ~bit : editorLaneMask | bit;
                }
            }
            case "blocked" -> editorBlocked = "on".equals(parts[1]);
            default -> {
            }
        }
    }

    private void applyEditorBrushAtScreen(int screenX, int screenY) {
        int tileX = clampTile((int) (screenToWorldX(screenX) / map.getTileSize()), map.getWidth());
        int tileY = clampTile((int) (screenToWorldY(screenY) / map.getTileSize()), map.getHeight());
        applyEditorBrush(tileX, tileY);
    }

    private void applyEditorBrush(int tileX, int tileY) {
        if (!map.inBounds(tileX, tileY)) {
            return;
        }

        map.setGround(tileX, tileY, editorGround);
        map.setWater(tileX, tileY, editorWater);
        map.setProp(tileX, tileY, editorProp);
        map.setBlocked(tileX, tileY, editorBlocked || editorGround == MapElements.FOREST || isImpassableProp(editorProp));
        map.setLaneMask(tileX, tileY, editorLaneMask);
        if (map.isBlocked(tileX, tileY) && (editorGround == MapElements.GRASS || editorGround == MapElements.GRASS_ALT)) {
            map.setTreeVariant(tileX, tileY, random.nextInt(4));
            map.setTreeTint(tileX, tileY, random.nextInt(5) - 2);
        }
        rebuildMapLayers();
        editorDirty = true;
    }

    private void sampleEditorBrushAtScreen(int screenX, int screenY) {
        int tileX = clampTile((int) (screenToWorldX(screenX) / map.getTileSize()), map.getWidth());
        int tileY = clampTile((int) (screenToWorldY(screenY) / map.getTileSize()), map.getHeight());
        sampleEditorBrush(tileX, tileY);
    }

    private void sampleEditorBrush(int tileX, int tileY) {
        if (!map.inBounds(tileX, tileY)) {
            return;
        }

        editorGround = map.getGround(tileX, tileY);
        editorWater = map.getWater(tileX, tileY);
        editorProp = map.getProp(tileX, tileY);
        editorBlocked = map.isBlocked(tileX, tileY);
        editorLaneMask = laneMaskAt(tileX, tileY);
        editorTool = EditorTool.PAINT;
        setEditorStatus("Tile sampled", 1.2);
    }

    private int laneMaskAt(int tileX, int tileY) {
        int mask = 0;
        if (map.hasLaneType(tileX, tileY, LaneType.TOP)) {
            mask |= laneBit(LaneType.TOP);
        }
        if (map.hasLaneType(tileX, tileY, LaneType.MID)) {
            mask |= laneBit(LaneType.MID);
        }
        if (map.hasLaneType(tileX, tileY, LaneType.BOT)) {
            mask |= laneBit(LaneType.BOT);
        }
        return mask;
    }

    private Structure findStructureForEditor(double worldX, double worldY) {
        Structure best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Structure structure : structures) {
            double dist = distance(worldX, worldY, structure.x, structure.y);
            if (dist <= structure.radius + 8.0 && dist < bestDistance) {
                bestDistance = dist;
                best = structure;
            }
        }
        return best;
    }

    private void dragEditorStructureToScreen(int screenX, int screenY) {
        if (editorDraggedStructure == null) {
            return;
        }
        int tileX = clampTile((int) (screenToWorldX(screenX) / map.getTileSize()), map.getWidth());
        int tileY = clampTile((int) (screenToWorldY(screenY) / map.getTileSize()), map.getHeight());
        editorDraggedStructure.x = map.tileCenter(tileX);
        editorDraggedStructure.y = map.tileCenter(tileY);
        editorDirty = true;
    }

    private Color groundPreviewColor(GroundElement ground) {
        return switch (ground.kind()) {
            case FOREST -> new Color(86, 128, 76);
            case GRASS -> new Color(102, 146, 86);
            case GRASS_ALT -> new Color(88, 131, 74);
            case DIRT -> new Color(125, 103, 74);
            case LANE -> new Color(96, 87, 78);
            case HIGH_GROUND -> new Color(126, 160, 102);
            case BASE -> new Color(126, 118, 108);
        };
    }

    private String laneMaskLabel(int mask) {
        if (mask == 0) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        if ((mask & laneBit(LaneType.TOP)) != 0) {
            parts.add("top");
        }
        if ((mask & laneBit(LaneType.MID)) != 0) {
            parts.add("mid");
        }
        if ((mask & laneBit(LaneType.BOT)) != 0) {
            parts.add("bot");
        }
        return String.join("/", parts);
    }

    private String propPreviewLabel(PropElement prop) {
        return switch (prop.kind()) {
            case BOULDER -> "BL";
            case ROCK -> "R";
            case BUSH -> "B";
            case STUMP -> "S";
            case PEBBLES -> "P";
        };
    }

    private boolean isImpassableProp(PropElement prop) {
        return prop != null && prop.kind() == com.example.demo.game.world.element.PropKind.BOULDER;
    }

    private Color brighten(Color color, int delta) {
        return new Color(
                Math.min(255, color.getRed() + delta),
                Math.min(255, color.getGreen() + delta),
                Math.min(255, color.getBlue() + delta),
                color.getAlpha()
        );
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
        if (showAttackRanges) {
            drawAttackRanges(g2);
        }
        drawStructures(g2);
        drawClickMarkers(g2);
        drawCreeps(g2);
        drawExperienceOrbs(g2);
        drawBullets(g2);
        drawHeroes(g2);
        drawHeroOverheadHud(g2);
        hudRenderer.draw(g2, buildHudModel());
        if (editorMode) {
            drawEditorWorldOverlay(g2);
            drawEditorOverlay(g2);
        }
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

    private void drawAttackRanges(Graphics2D g2) {
        Stroke oldStroke = g2.getStroke();

        for (Structure structure : structures) {
            if (structure.hp <= 0) {
                continue;
            }
            drawAttackRangeCircle(
                    g2,
                    structure.x,
                    structure.y,
                    structure.attackRange,
                    structure.team == Team.LIGHT ? new Color(82, 214, 124, 50) : new Color(255, 90, 90, 52),
                    structure.team == Team.LIGHT ? new Color(126, 238, 156, 155) : new Color(255, 128, 128, 160)
            );
            drawStructureTargetIndicator(g2, structure);
        }

        for (Player hero : heroes) {
            if (hero.hp <= 0) {
                continue;
            }
            double attackRange = heroAttackRange(hero);
            if (attackRange <= 0.0) {
                continue;
            }
            drawAttackRangeCircle(
                    g2,
                    hero.x,
                    hero.y,
                    attackRange,
                    hero.team == Team.LIGHT ? new Color(82, 214, 124, 34) : new Color(255, 90, 90, 38),
                    hero.team == Team.LIGHT ? new Color(126, 238, 156, 135) : new Color(255, 128, 128, 145)
            );
        }

        g2.setStroke(oldStroke);
    }

    private void drawAttackRangeCircle(Graphics2D g2,
                                       double worldX,
                                       double worldY,
                                       double range,
                                       Color fillColor,
                                       Color strokeColor) {
        int sx = worldToScreenX(worldX);
        int sy = worldToScreenY(worldY);
        int radius = (int) Math.round(range * ZOOM);
        g2.setColor(fillColor);
        g2.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g2.setColor(strokeColor);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawOval(sx - radius, sy - radius, radius * 2, radius * 2);
    }

    private void drawStructureTargetIndicator(Graphics2D g2, Structure structure) {
        if (!isValidStructureTarget(structure, structure.attackTarget)) {
            return;
        }

        int sx = worldToScreenX(structure.x);
        int sy = worldToScreenY(structure.y);
        int tx = worldToScreenX(structure.attackTarget.getX());
        int ty = worldToScreenY(structure.attackTarget.getY());

        Color lineColor = structure.team == Team.LIGHT
                ? new Color(132, 245, 164, 190)
                : new Color(255, 124, 124, 190);
        Color targetColor = structure.team == Team.LIGHT
                ? new Color(214, 255, 224, 210)
                : new Color(255, 226, 226, 210);

        g2.setStroke(new BasicStroke(1.8f));
        g2.setColor(lineColor);
        g2.drawLine(sx, sy, tx, ty);

        int markerRadius = (int) Math.round(8 * ZOOM);
        g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 60));
        g2.fillOval(tx - markerRadius, ty - markerRadius, markerRadius * 2, markerRadius * 2);
        g2.setColor(targetColor);
        g2.drawOval(tx - markerRadius, ty - markerRadius, markerRadius * 2, markerRadius * 2);
        g2.drawLine(tx - markerRadius - 4, ty, tx - markerRadius / 2, ty);
        g2.drawLine(tx + markerRadius / 2, ty, tx + markerRadius + 4, ty);
        g2.drawLine(tx, ty - markerRadius - 4, tx, ty - markerRadius / 2);
        g2.drawLine(tx, ty + markerRadius / 2, tx, ty + markerRadius + 4);
    }

    private double heroAttackRange(Player hero) {
        if (hero == player) {
            return playerWeaponReach();
        }
        return 0.0;
    }

    private void drawStructures(Graphics2D g2) {
        Stroke oldStroke = g2.getStroke();
        for (Structure s : structures) {
            if (s.hp <= 0) {
                continue;
            }

            int sx = worldToScreenX(s.x);
            int sy = worldToScreenY(s.y);
            int r = (int) Math.round(s.radius * ZOOM);

            Graphics2D structureGraphics = (Graphics2D) g2.create();
            applyStructureAttackTransform(structureGraphics, s, sx, sy, r);
            if (s.type == StructureType.TOWER) {
                if (s.team == Team.LIGHT) {
                    drawLightTower(structureGraphics, sx, sy, r);
                } else {
                    drawDarkDragonTower(structureGraphics, sx, sy, r);
                }
            } else {
                if (s.team == Team.LIGHT) {
                    drawFountain(structureGraphics, sx, sy, r,
                            new Color(204, 223, 240),
                            new Color(106, 132, 156),
                            new Color(92, 205, 255, 210),
                            new Color(226, 249, 255, 225),
                            new Color(132, 220, 255, 90));
                } else {
                    drawFountain(structureGraphics, sx, sy, r,
                            new Color(74, 50, 54),
                            new Color(36, 24, 28),
                            new Color(178, 24, 32, 218),
                            new Color(255, 118, 110, 228),
                            new Color(196, 42, 46, 90));
                }
            }
            structureGraphics.dispose();
            drawStructureAttackEffect(g2, s, sx, sy, r);

            drawHealthBar(g2, sx, sy - r - 12, 54, 7,
                    (double) s.hp / s.maxHp,
                    s.team == Team.LIGHT ? new Color(88, 168, 255) : new Color(248, 96, 88));
        }
        g2.setStroke(oldStroke);
    }

    private void applyStructureAttackTransform(Graphics2D g2, Structure structure, int sx, int sy, int r) {
        if (structure.attackAnimationTimer <= 0.0) {
            return;
        }

        double progress = structureAttackProgress(structure);
        double wobble = jellyWobble(progress);
        double scaleX = 1.0 + wobble * 0.1;
        double scaleY = 1.0 - wobble * 0.07;
        double anchorY = sy + r * 0.28;

        g2.translate(sx, anchorY);
        g2.scale(scaleX, scaleY);
        g2.translate(-sx, -anchorY);
    }

    private void drawStructureAttackEffect(Graphics2D g2, Structure structure, int sx, int sy, int r) {
        if (structure.attackAnimationTimer <= 0.0) {
            return;
        }

        double progress = structureAttackProgress(structure);
        double dx = structure.attackVisualTargetX - structure.x;
        double dy = structure.attackVisualTargetY - structure.y;
        double len = Math.hypot(dx, dy);
        if (len < 0.001) {
            return;
        }

        double dirX = dx / len;
        double dirY = dy / len;
        double perpX = -dirY;
        double perpY = dirX;
        double wobble = jellyWobble(progress);
        double amplitude = (10.0 + r * 0.12) * wobble * ZOOM;
        double startX = sx + dirX * r * 0.18;
        double startY = sy - r * 0.82;
        double endX = worldToScreenX(structure.attackVisualTargetX);
        double endY = worldToScreenY(structure.attackVisualTargetY);

        Path2D.Double beam = new Path2D.Double();
        beam.moveTo(startX, startY);
        int segments = 6;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double wave = Math.sin(t * Math.PI * 2.0 + progress * Math.PI * 1.7);
            double offset = amplitude * wave * (1.0 - t * 0.35);
            double px = startX + (endX - startX) * t + perpX * offset;
            double py = startY + (endY - startY) * t + perpY * offset;
            beam.lineTo(px, py);
        }

        Color coreColor = structure.team == Team.LIGHT
                ? new Color(168, 236, 255, 220)
                : new Color(255, 140, 132, 220);
        Color glowColor = structure.team == Team.LIGHT
                ? new Color(92, 205, 255, 88)
                : new Color(255, 90, 90, 82);

        Stroke oldStroke = g2.getStroke();
        g2.setColor(glowColor);
        g2.setStroke(new BasicStroke((float) Math.max(4.0, 7.5 * (1.0 - progress)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(beam);
        g2.setColor(coreColor);
        g2.setStroke(new BasicStroke((float) Math.max(2.0, 3.5 * (1.0 - progress)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(beam);
        g2.setStroke(oldStroke);

        int pulseRadius = (int) Math.round((5.0 + (1.0 - progress) * 6.0) * ZOOM);
        g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 110));
        g2.fillOval((int) Math.round(endX) - pulseRadius, (int) Math.round(endY) - pulseRadius, pulseRadius * 2, pulseRadius * 2);
        g2.setColor(coreColor);
        g2.drawOval((int) Math.round(endX) - pulseRadius, (int) Math.round(endY) - pulseRadius, pulseRadius * 2, pulseRadius * 2);
    }

    private void drawLightTower(Graphics2D g2, int sx, int sy, int r) {
        g2.setColor(new Color(42, 64, 86, 48));
        g2.fillOval(sx - (int) (r * 1.05), sy + (int) (r * 0.52), (int) (r * 2.1), (int) (r * 0.62));

        g2.setColor(new Color(166, 194, 222));
        g2.fillRoundRect(sx - (int) (r * 0.88), sy + (int) (r * 0.18), (int) (r * 1.76), (int) (r * 0.42), 12, 12);
        g2.setColor(new Color(96, 128, 160));
        g2.drawRoundRect(sx - (int) (r * 0.88), sy + (int) (r * 0.18), (int) (r * 1.76), (int) (r * 0.42), 12, 12);

        g2.setColor(new Color(214, 232, 246));
        g2.fillRoundRect(sx - (int) (r * 0.54), sy - (int) (r * 0.96), (int) (r * 1.08), (int) (r * 1.44), 14, 14);
        g2.setColor(new Color(104, 136, 168));
        g2.drawRoundRect(sx - (int) (r * 0.54), sy - (int) (r * 0.96), (int) (r * 1.08), (int) (r * 1.44), 14, 14);

        Path2D buttressLeft = new Path2D.Double();
        buttressLeft.moveTo(sx - r * 0.54, sy + r * 0.3);
        buttressLeft.lineTo(sx - r * 0.92, sy + r * 0.54);
        buttressLeft.lineTo(sx - r * 0.78, sy - r * 0.12);
        buttressLeft.lineTo(sx - r * 0.54, sy - r * 0.04);
        buttressLeft.closePath();
        Path2D buttressRight = new Path2D.Double();
        buttressRight.moveTo(sx + r * 0.54, sy + r * 0.3);
        buttressRight.lineTo(sx + r * 0.92, sy + r * 0.54);
        buttressRight.lineTo(sx + r * 0.78, sy - r * 0.12);
        buttressRight.lineTo(sx + r * 0.54, sy - r * 0.04);
        buttressRight.closePath();
        g2.setColor(new Color(178, 204, 226));
        g2.fill(buttressLeft);
        g2.fill(buttressRight);

        g2.setColor(new Color(170, 198, 224));
        g2.fillRoundRect(sx - (int) (r * 0.76), sy - (int) (r * 1.14), (int) (r * 1.52), (int) (r * 0.28), 8, 8);
        g2.setColor(new Color(96, 128, 160));
        g2.drawRoundRect(sx - (int) (r * 0.76), sy - (int) (r * 1.14), (int) (r * 1.52), (int) (r * 0.28), 8, 8);
        for (int i = -1; i <= 1; i++) {
            g2.setColor(new Color(208, 229, 246));
            g2.fillRect(sx + (int) (i * r * 0.28) - (int) (r * 0.1), sy - (int) (r * 1.28), (int) (r * 0.2), (int) (r * 0.18));
            g2.setColor(new Color(96, 128, 160));
            g2.drawRect(sx + (int) (i * r * 0.28) - (int) (r * 0.1), sy - (int) (r * 1.28), (int) (r * 0.2), (int) (r * 0.18));
        }

        g2.setColor(new Color(92, 182, 255, 70));
        g2.fillOval(sx - (int) (r * 0.52), sy - (int) (r * 1.9), (int) (r * 1.04), (int) (r * 1.04));

        Path2D diamond = new Path2D.Double();
        diamond.moveTo(sx, sy - r * 1.66);
        diamond.lineTo(sx + r * 0.28, sy - r * 1.38);
        diamond.lineTo(sx, sy - r * 1.06);
        diamond.lineTo(sx - r * 0.28, sy - r * 1.38);
        diamond.closePath();
        g2.setColor(new Color(238, 251, 255));
        g2.fill(diamond);
        g2.setColor(new Color(82, 172, 238));
        g2.draw(diamond);
        g2.setColor(new Color(148, 224, 255, 180));
        g2.drawLine(sx, sy - (int) (r * 1.58), sx, sy - (int) (r * 1.12));

        g2.setColor(new Color(114, 162, 206, 170));
        g2.fillRoundRect(sx - (int) (r * 0.12), sy - (int) (r * 0.74), (int) (r * 0.24), (int) (r * 0.62), 6, 6);
    }

    private void drawDarkDragonTower(Graphics2D g2, int sx, int sy, int r) {
        g2.setColor(new Color(16, 10, 12, 58));
        g2.fillOval(sx - (int) (r * 1.12), sy + (int) (r * 0.5), (int) (r * 2.24), (int) (r * 0.66));

        g2.setColor(new Color(76, 60, 64));
        g2.fillRoundRect(sx - (int) (r * 0.92), sy + (int) (r * 0.2), (int) (r * 1.84), (int) (r * 0.38), 12, 12);
        g2.setColor(new Color(32, 24, 28));
        g2.drawRoundRect(sx - (int) (r * 0.92), sy + (int) (r * 0.2), (int) (r * 1.84), (int) (r * 0.38), 12, 12);

        g2.setColor(new Color(90, 74, 78));
        g2.fillRoundRect(sx - (int) (r * 0.3), sy - (int) (r * 0.44), (int) (r * 0.6), (int) (r * 0.96), 10, 10);
        g2.setColor(new Color(34, 24, 28));
        g2.drawRoundRect(sx - (int) (r * 0.3), sy - (int) (r * 0.44), (int) (r * 0.6), (int) (r * 0.96), 10, 10);

        Path2D leftWing = new Path2D.Double();
        leftWing.moveTo(sx - r * 0.16, sy - r * 0.16);
        leftWing.lineTo(sx - r * 1.06, sy - r * 0.92);
        leftWing.lineTo(sx - r * 0.92, sy - r * 0.14);
        leftWing.lineTo(sx - r * 1.02, sy + r * 0.22);
        leftWing.lineTo(sx - r * 0.42, sy + r * 0.12);
        leftWing.closePath();
        Path2D rightWing = new Path2D.Double();
        rightWing.moveTo(sx + r * 0.16, sy - r * 0.16);
        rightWing.lineTo(sx + r * 1.06, sy - r * 0.92);
        rightWing.lineTo(sx + r * 0.92, sy - r * 0.14);
        rightWing.lineTo(sx + r * 1.02, sy + r * 0.22);
        rightWing.lineTo(sx + r * 0.42, sy + r * 0.12);
        rightWing.closePath();
        g2.setColor(new Color(64, 52, 56));
        g2.fill(leftWing);
        g2.fill(rightWing);
        g2.setColor(new Color(22, 16, 18));
        g2.draw(leftWing);
        g2.draw(rightWing);

        Path2D neck = new Path2D.Double();
        neck.moveTo(sx - r * 0.14, sy - r * 0.58);
        neck.lineTo(sx + r * 0.14, sy - r * 0.58);
        neck.lineTo(sx + r * 0.22, sy - r * 1.18);
        neck.lineTo(sx - r * 0.22, sy - r * 1.18);
        neck.closePath();
        g2.setColor(new Color(82, 66, 72));
        g2.fill(neck);
        g2.setColor(new Color(24, 18, 20));
        g2.draw(neck);

        Path2D head = new Path2D.Double();
        head.moveTo(sx, sy - r * 1.52);
        head.lineTo(sx + r * 0.34, sy - r * 1.18);
        head.lineTo(sx + r * 0.24, sy - r * 0.84);
        head.lineTo(sx, sy - r * 0.72);
        head.lineTo(sx - r * 0.24, sy - r * 0.84);
        head.lineTo(sx - r * 0.34, sy - r * 1.18);
        head.closePath();
        g2.setColor(new Color(96, 76, 82));
        g2.fill(head);
        g2.setColor(new Color(28, 20, 22));
        g2.draw(head);

        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(28, 20, 22));
        g2.drawLine(sx - (int) (r * 0.12), sy - (int) (r * 1.46), sx - (int) (r * 0.3), sy - (int) (r * 1.68));
        g2.drawLine(sx + (int) (r * 0.12), sy - (int) (r * 1.46), sx + (int) (r * 0.3), sy - (int) (r * 1.68));
        g2.setColor(new Color(255, 110, 96, 220));
        g2.fillOval(sx - (int) (r * 0.16), sy - (int) (r * 1.16), (int) (r * 0.12), (int) (r * 0.12));
        g2.fillOval(sx + (int) (r * 0.04), sy - (int) (r * 1.16), (int) (r * 0.12), (int) (r * 0.12));
    }

    private void drawFountain(Graphics2D g2,
                              int sx,
                              int sy,
                              int r,
                              Color stoneLight,
                              Color stoneDark,
                              Color liquidMain,
                              Color liquidBright,
                              Color glow) {
        g2.setColor(new Color(18, 28, 32, 46));
        g2.fillOval(sx - (int) (r * 1.2), sy + (int) (r * 0.46), (int) (r * 2.4), (int) (r * 0.72));

        g2.setColor(stoneDark);
        g2.fillOval(sx - (int) (r * 1.02), sy + (int) (r * 0.24), (int) (r * 2.04), (int) (r * 0.66));
        g2.setColor(stoneLight);
        g2.fillOval(sx - (int) (r * 0.9), sy + (int) (r * 0.12), (int) (r * 1.8), (int) (r * 0.5));
        g2.setColor(liquidMain);
        g2.fillOval(sx - (int) (r * 0.72), sy + (int) (r * 0.22), (int) (r * 1.44), (int) (r * 0.26));

        g2.setColor(stoneLight);
        g2.fillRoundRect(sx - (int) (r * 0.22), sy - (int) (r * 0.66), (int) (r * 0.44), (int) (r * 0.96), 12, 12);
        g2.setColor(stoneDark);
        g2.drawRoundRect(sx - (int) (r * 0.22), sy - (int) (r * 0.66), (int) (r * 0.44), (int) (r * 0.96), 12, 12);

        g2.setColor(stoneDark);
        g2.fillOval(sx - (int) (r * 0.54), sy - (int) (r * 0.52), (int) (r * 1.08), (int) (r * 0.46));
        g2.setColor(stoneLight);
        g2.fillOval(sx - (int) (r * 0.46), sy - (int) (r * 0.6), (int) (r * 0.92), (int) (r * 0.34));
        g2.setColor(liquidMain);
        g2.fillOval(sx - (int) (r * 0.28), sy - (int) (r * 0.52), (int) (r * 0.56), (int) (r * 0.18));

        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(glow);
        g2.drawLine(sx, sy - (int) (r * 0.92), sx - (int) (r * 0.22), sy - (int) (r * 0.42));
        g2.drawLine(sx, sy - (int) (r * 0.92), sx + (int) (r * 0.22), sy - (int) (r * 0.42));
        g2.drawLine(sx, sy - (int) (r * 0.92), sx, sy - (int) (r * 0.3));

        g2.setColor(liquidBright);
        g2.fillOval(sx - (int) (r * 0.12), sy - (int) (r * 1.04), (int) (r * 0.24), (int) (r * 0.24));
        g2.setColor(glow);
        g2.fillOval(sx - (int) (r * 0.34), sy - (int) (r * 1.24), (int) (r * 0.68), (int) (r * 0.54));

        g2.setColor(stoneDark);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawOval(sx - (int) (r * 1.02), sy + (int) (r * 0.24), (int) (r * 2.04), (int) (r * 0.66));
        g2.drawOval(sx - (int) (r * 0.54), sy - (int) (r * 0.52), (int) (r * 1.08), (int) (r * 0.46));
    }

    private void drawClickMarkers(Graphics2D g2) {
        for (ClickMarker marker : clickMarkers) {
            double progress = 1.0 - marker.lifetime / CLICK_MARKER_LIFETIME;
            double pulse = Math.sin(progress * Math.PI);
            int alpha = (int) Math.round(220 * (1.0 - progress));
            int sx = worldToScreenX(marker.x);
            int sy = worldToScreenY(marker.y);
            if (marker.attack) {
                int outerRadius = (int) Math.round((10.0 + progress * 22.0) * ZOOM);
                int innerRadius = (int) Math.round((4.0 + pulse * 5.0) * ZOOM);
                int spikeGap = (int) Math.round((7.0 + pulse * 3.0) * ZOOM);
                int spikeLen = (int) Math.round((7.0 + (1.0 - progress) * 4.0) * ZOOM);

                g2.setColor(new Color(255, 94, 78, Math.max(0, alpha / 4)));
                g2.fillOval(sx - outerRadius, sy - outerRadius, outerRadius * 2, outerRadius * 2);

                g2.setStroke(new BasicStroke((float) Math.max(2.0, 3.2 * (1.0 - progress))));
                g2.setColor(new Color(255, 98, 82, Math.max(0, alpha)));
                g2.drawOval(sx - outerRadius, sy - outerRadius, outerRadius * 2, outerRadius * 2);

                g2.setColor(new Color(255, 214, 205, Math.max(0, alpha)));
                g2.drawOval(sx - innerRadius, sy - innerRadius, innerRadius * 2, innerRadius * 2);

                g2.drawLine(sx - spikeGap - spikeLen, sy, sx - spikeGap, sy);
                g2.drawLine(sx + spikeGap, sy, sx + spikeGap + spikeLen, sy);
                g2.drawLine(sx, sy - spikeGap - spikeLen, sx, sy - spikeGap);
                g2.drawLine(sx, sy + spikeGap, sx, sy + spikeGap + spikeLen);

                int diag = (int) Math.round((3.0 + pulse * 3.0) * ZOOM);
                g2.drawLine(sx - diag, sy - diag, sx + diag, sy + diag);
                g2.drawLine(sx - diag, sy + diag, sx + diag, sy - diag);
            } else {
                int outerRadius = (int) Math.round((8.0 + progress * 18.0) * ZOOM);
                int innerRadius = (int) Math.round((4.0 + pulse * 4.0) * ZOOM);
                int bracketOffset = (int) Math.round((5.0 + pulse * 2.5) * ZOOM);
                int bracketLen = (int) Math.round((5.0 + (1.0 - progress) * 4.0) * ZOOM);

                g2.setColor(new Color(88, 236, 132, Math.max(0, alpha / 5)));
                g2.fillOval(sx - outerRadius, sy - outerRadius, outerRadius * 2, outerRadius * 2);

                g2.setStroke(new BasicStroke((float) Math.max(2.0, 3.0 * (1.0 - progress))));
                g2.setColor(new Color(74, 230, 118, Math.max(0, alpha)));
                g2.drawOval(sx - outerRadius, sy - outerRadius, outerRadius * 2, outerRadius * 2);
                g2.drawOval(sx - innerRadius, sy - innerRadius, innerRadius * 2, innerRadius * 2);

                g2.setColor(new Color(205, 255, 219, Math.max(0, alpha)));
                g2.drawLine(sx - bracketOffset - bracketLen, sy - bracketOffset, sx - bracketOffset, sy - bracketOffset);
                g2.drawLine(sx - bracketOffset, sy - bracketOffset - bracketLen, sx - bracketOffset, sy - bracketOffset);

                g2.drawLine(sx + bracketOffset, sy - bracketOffset, sx + bracketOffset + bracketLen, sy - bracketOffset);
                g2.drawLine(sx + bracketOffset, sy - bracketOffset - bracketLen, sx + bracketOffset, sy - bracketOffset);

                g2.drawLine(sx - bracketOffset - bracketLen, sy + bracketOffset, sx - bracketOffset, sy + bracketOffset);
                g2.drawLine(sx - bracketOffset, sy + bracketOffset, sx - bracketOffset, sy + bracketOffset + bracketLen);

                g2.drawLine(sx + bracketOffset, sy + bracketOffset, sx + bracketOffset + bracketLen, sy + bracketOffset);
                g2.drawLine(sx + bracketOffset, sy + bracketOffset, sx + bracketOffset, sy + bracketOffset + bracketLen);
            }
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
        int renderSy = sy + (int) Math.round(laneCreepVisualYOffset(creep));
        int r = (int) Math.round(creep.radius * ZOOM);
        if (creep.role == CreepRole.LANE) {
            drawLaneCreepVisual(g2, creep, sx, renderSy, r);
        } else {
            BufferedImage sprite = sprites.getPlayerFrame(creep.state, creep.animPhase);
            drawTintedSpriteWithFacing(g2, sprite, sx, sy, creep.lookAngle, creepRenderScale(creep), creepTint(creep));
        }

        if (creep.state == AnimationState.ATTACK) {
            g2.setColor(creep.team == Team.LIGHT
                    ? new Color(102, 232, 118, 110)
                    : new Color(255, 118, 92, 110));
            g2.fillOval(sx - r - 4, renderSy - r - 4, r * 2 + 8, r * 2 + 8);
        }

        drawLaneCreepAttackEffect(g2, creep, sx, renderSy, r);

        drawHealthBar(g2, sx, renderSy - r - 10, 28, 5,
                (double) creep.hp / creep.maxHp,
                creep.team == Team.LIGHT ? new Color(76, 214, 104) : new Color(241, 94, 85));
    }

    private double creepRenderScale(Creep creep) {
        return switch (creep.laneType) {
            case MELEE -> 1.0;
            case RANGED -> 0.92;
            case CATAPULT -> 1.18;
        };
    }

    private Color creepTint(Creep creep) {
        return switch (creep.team) {
            case LIGHT -> switch (creep.laneType) {
                case MELEE -> new Color(66, 196, 88);
                case RANGED -> new Color(80, 180, 210);
                case CATAPULT -> new Color(142, 170, 84);
            };
            case DARK -> switch (creep.laneType) {
                case MELEE -> new Color(198, 54, 54);
                case RANGED -> new Color(192, 106, 54);
                case CATAPULT -> new Color(146, 122, 58);
            };
            case NEUTRAL -> new Color(160, 86, 70);
        };
    }

    private double laneCreepVisualYOffset(Creep creep) {
        if (creep.role != CreepRole.LANE) {
            return 0.0;
        }
        return switch (creep.laneType) {
            case MELEE -> 0.0;
            case RANGED -> -2.5 - Math.abs(Math.sin(creep.animPhase * 0.55)) * 3.0;
            case CATAPULT -> -1.2 - Math.abs(Math.sin(creep.animPhase * 0.45)) * 1.8;
        };
    }

    private void drawLaneCreepVisual(Graphics2D g2, Creep creep, int sx, int sy, int radius) {
        drawLaneCreepShadow(g2, creep, sx, sy, radius);

        Graphics2D cg = (Graphics2D) g2.create();
        cg.translate(sx, sy);
        if (Math.cos(creep.lookAngle) < 0.0) {
            cg.scale(-1.0, 1.0);
        }
        double scale = radius / 10.5;
        cg.scale(scale, scale);

        switch (creep.team) {
            case LIGHT -> drawLightLaneCreep(cg, creep);
            case DARK -> drawDarkLaneCreep(cg, creep);
            case NEUTRAL -> {
                BufferedImage sprite = sprites.getPlayerFrame(creep.state, creep.animPhase);
                cg.scale(1.0 / scale, 1.0 / scale);
                drawTintedSpriteWithFacing(cg, sprite, 0, 0, 0.0, creepRenderScale(creep), new Color(160, 86, 70));
            }
        }
        cg.dispose();
    }

    private void drawLaneCreepShadow(Graphics2D g2, Creep creep, int sx, int sy, int radius) {
        int shadowW = switch (creep.laneType) {
            case MELEE -> (int) Math.round(radius * 1.45);
            case RANGED -> (int) Math.round(radius * 1.55);
            case CATAPULT -> (int) Math.round(radius * 1.8);
        };
        int shadowH = switch (creep.laneType) {
            case MELEE -> Math.max(5, (int) Math.round(radius * 0.42));
            case RANGED -> Math.max(4, (int) Math.round(radius * 0.34));
            case CATAPULT -> Math.max(4, (int) Math.round(radius * 0.38));
        };
        int shadowY = sy + radius - shadowH / 2 + (creep.laneType == LaneCreepType.MELEE ? 1 : 4);
        g2.setColor(new Color(10, 12, 18, creep.laneType == LaneCreepType.MELEE ? 70 : 52));
        g2.fillOval(sx - shadowW / 2, shadowY, shadowW, shadowH);
    }

    private void drawLightLaneCreep(Graphics2D g2, Creep creep) {
        switch (creep.laneType) {
            case MELEE -> drawLightAngelKnight(g2, creep);
            case RANGED -> drawLightAngelMage(g2, creep);
            case CATAPULT -> drawLightEye(g2, creep);
        }
    }

    private void drawDarkLaneCreep(Graphics2D g2, Creep creep) {
        switch (creep.laneType) {
            case MELEE -> drawDarkImpKnight(g2, creep);
            case RANGED -> drawDarkImpFlier(g2, creep);
            case CATAPULT -> drawDarkEye(g2, creep);
        }
    }

    private void drawLightAngelKnight(Graphics2D g2, Creep creep) {
        double flap = 1.6 + Math.sin(creep.animPhase * 0.55) * 1.3;
        drawFeatherWing(g2, true, -5.0, -1.0, flap, new Color(244, 250, 255), new Color(195, 214, 235));
        drawFeatherWing(g2, false, 5.0, -1.0, flap, new Color(244, 250, 255), new Color(195, 214, 235));
        drawHalo(g2, new Color(255, 228, 150), new Color(255, 250, 228, 190), 0, -14, 12, 4);

        g2.setColor(new Color(226, 232, 242));
        g2.fillRoundRect(-5, -2, 10, 11, 4, 4);
        g2.setColor(new Color(136, 152, 176));
        g2.drawRoundRect(-5, -2, 10, 11, 4, 4);
        g2.setColor(new Color(255, 214, 136));
        g2.fillOval(-4, -10, 8, 8);
        g2.setColor(new Color(216, 183, 128));
        g2.drawOval(-4, -10, 8, 8);
        g2.setColor(new Color(112, 128, 168));
        g2.fillRect(-2, -5, 4, 1);
        g2.setColor(new Color(204, 164, 98));
        g2.fillRoundRect(-7, 0, 3, 8, 2, 2);
        g2.setColor(new Color(142, 150, 164));
        g2.fillRoundRect(-2, 8, 3, 6, 2, 2);
        g2.fillRoundRect(1, 8, 3, 6, 2, 2);
        g2.setColor(new Color(98, 112, 134));
        g2.drawRoundRect(-2, 8, 3, 6, 2, 2);
        g2.drawRoundRect(1, 8, 3, 6, 2, 2);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(126, 190, 255));
        g2.drawLine(4, -1, 11, 7);
        g2.setColor(new Color(228, 244, 255));
        g2.drawLine(5, -2, 12, 6);
        g2.setStroke(oldStroke);
    }

    private void drawLightAngelMage(Graphics2D g2, Creep creep) {
        double flap = 3.2 + Math.sin(creep.animPhase * 0.7) * 2.1;
        drawFeatherWing(g2, true, -4.5, -3.0, flap, new Color(249, 246, 234), new Color(220, 206, 172));
        drawFeatherWing(g2, false, 4.5, -3.0, flap, new Color(249, 246, 234), new Color(220, 206, 172));
        drawHalo(g2, new Color(255, 224, 150), new Color(255, 245, 214, 190), 0, -15, 12, 4);

        g2.setColor(new Color(246, 242, 224));
        Path2D.Double robe = new Path2D.Double();
        robe.moveTo(-4, -3);
        robe.lineTo(4, -3);
        robe.lineTo(7, 10);
        robe.lineTo(-7, 10);
        robe.closePath();
        g2.fill(robe);
        g2.setColor(new Color(190, 170, 118));
        g2.draw(robe);

        g2.setColor(new Color(255, 223, 162));
        g2.fillOval(-4, -10, 8, 8);
        g2.setColor(new Color(214, 186, 134));
        g2.drawOval(-4, -10, 8, 8);
        g2.setColor(new Color(176, 154, 92));
        g2.drawLine(-2, 12, 0, 8);
        g2.drawLine(2, 12, 0, 8);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(236, 224, 178));
        g2.drawLine(2, -1, 8, 5);
        g2.setColor(new Color(214, 188, 104, 185));
        g2.fillOval(8, 2, 7, 7);
        g2.setColor(new Color(255, 247, 214, 220));
        g2.fillOval(10, 4, 3, 3);
        g2.setStroke(oldStroke);
    }

    private void drawLightEye(Graphics2D g2, Creep creep) {
        double pulse = 1.0 + Math.sin(creep.animPhase * 0.55) * 0.08;
        g2.scale(pulse, pulse);
        drawHalo(g2, new Color(255, 228, 156), new Color(255, 246, 222, 175), 0, -12, 16, 5);

        Path2D.Double finLeft = new Path2D.Double();
        finLeft.moveTo(-10, -2);
        finLeft.lineTo(-16, -7);
        finLeft.lineTo(-15, 4);
        finLeft.closePath();
        Path2D.Double finRight = new Path2D.Double();
        finRight.moveTo(10, -2);
        finRight.lineTo(16, -7);
        finRight.lineTo(15, 4);
        finRight.closePath();
        g2.setColor(new Color(244, 244, 252));
        g2.fill(finLeft);
        g2.fill(finRight);
        g2.setColor(new Color(190, 210, 232));
        g2.draw(finLeft);
        g2.draw(finRight);

        g2.setColor(new Color(248, 250, 255));
        g2.fillOval(-11, -7, 22, 14);
        g2.setColor(new Color(200, 176, 92));
        g2.drawOval(-11, -7, 22, 14);
        g2.setColor(new Color(94, 186, 255));
        g2.fillOval(-5, -5, 10, 10);
        g2.setColor(new Color(36, 88, 156));
        g2.fillOval(-2, -2, 4, 4);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(1, -4, 3, 3);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 232, 164, 180));
        g2.drawLine(-6, -10, -2, -14);
        g2.drawLine(0, -11, 0, -16);
        g2.drawLine(6, -10, 2, -14);
        g2.setStroke(oldStroke);
    }

    private void drawDarkImpKnight(Graphics2D g2, Creep creep) {
        g2.setColor(new Color(86, 22, 24));
        g2.fillOval(-4, -10, 8, 8);
        g2.setColor(new Color(48, 12, 12));
        g2.drawOval(-4, -10, 8, 8);
        drawHorn(g2, true, new Color(96, 58, 34));
        drawHorn(g2, false, new Color(96, 58, 34));

        g2.setColor(new Color(88, 82, 88));
        g2.fillRoundRect(-5, -2, 10, 11, 4, 4);
        g2.setColor(new Color(34, 28, 34));
        g2.drawRoundRect(-5, -2, 10, 11, 4, 4);
        g2.setColor(new Color(156, 44, 44));
        g2.fillRoundRect(-2, 1, 4, 6, 2, 2);
        g2.setColor(new Color(70, 12, 12));
        g2.fillRoundRect(-7, 0, 3, 7, 2, 2);
        g2.setColor(new Color(98, 88, 96));
        g2.fillRoundRect(-2, 8, 3, 6, 2, 2);
        g2.fillRoundRect(1, 8, 3, 6, 2, 2);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(190, 190, 196));
        g2.drawLine(4, -1, 11, 6);
        g2.setColor(new Color(82, 64, 48));
        g2.drawLine(2, 1, 5, 3);
        g2.setStroke(oldStroke);

        g2.setColor(new Color(100, 22, 26));
        g2.drawLine(-1, 7, -6, 11);
        g2.drawLine(-6, 11, -4, 13);
        g2.drawLine(-6, 11, -7, 8);
    }

    private void drawDarkImpFlier(Graphics2D g2, Creep creep) {
        double flap = 3.4 + Math.sin(creep.animPhase * 0.8) * 2.4;
        drawBatWing(g2, true, -3.0, -2.0, flap, new Color(72, 38, 46), new Color(34, 18, 24));
        drawBatWing(g2, false, 3.0, -2.0, flap, new Color(72, 38, 46), new Color(34, 18, 24));

        g2.setColor(new Color(120, 28, 34));
        g2.fillOval(-4, -8, 8, 8);
        g2.setColor(new Color(64, 14, 18));
        g2.drawOval(-4, -8, 8, 8);
        drawHorn(g2, true, new Color(110, 84, 48));
        drawHorn(g2, false, new Color(110, 84, 48));
        g2.setColor(new Color(82, 18, 24));
        g2.fillRoundRect(-3, -1, 6, 9, 4, 4);
        g2.setColor(new Color(150, 78, 30));
        g2.fillOval(7, 0, 7, 7);
        g2.setColor(new Color(255, 196, 96, 220));
        g2.fillOval(9, 2, 3, 3);
    }

    private void drawDarkEye(Graphics2D g2, Creep creep) {
        double pulse = 1.0 + Math.sin(creep.animPhase * 0.5) * 0.1;
        g2.scale(pulse, pulse);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 34, 38, 150));
        g2.drawLine(-12, -8, -17, -12);
        g2.drawLine(0, -10, 0, -16);
        g2.drawLine(12, -8, 17, -12);
        g2.drawLine(-12, 8, -17, 12);
        g2.drawLine(12, 8, 17, 12);
        g2.setStroke(oldStroke);

        g2.setColor(new Color(44, 12, 18));
        g2.fillOval(-12, -8, 24, 16);
        g2.setColor(new Color(154, 36, 42));
        g2.drawOval(-12, -8, 24, 16);
        g2.setColor(new Color(196, 44, 54));
        g2.fillOval(-6, -5, 12, 10);
        g2.setColor(new Color(24, 6, 8));
        g2.fillOval(-2, -2, 4, 4);
        g2.setColor(new Color(255, 214, 214, 180));
        g2.fillOval(1, -4, 3, 3);
    }

    private void drawFeatherWing(Graphics2D g2, boolean left, double anchorX, double anchorY, double spread, Color main, Color shade) {
        int dir = left ? -1 : 1;
        Path2D.Double wing = new Path2D.Double();
        wing.moveTo(anchorX, anchorY);
        wing.curveTo(anchorX + dir * (4.0 + spread), anchorY - 7.0, anchorX + dir * (11.0 + spread), anchorY - 8.0, anchorX + dir * 14.0, anchorY - 1.0);
        wing.curveTo(anchorX + dir * 10.0, anchorY + 5.0, anchorX + dir * 5.0, anchorY + 9.0, anchorX + dir * 1.5, anchorY + 7.0);
        wing.closePath();
        g2.setColor(shade);
        g2.fill(wing);

        Path2D.Double innerWing = new Path2D.Double();
        innerWing.moveTo(anchorX, anchorY);
        innerWing.curveTo(anchorX + dir * (3.0 + spread * 0.75), anchorY - 5.0, anchorX + dir * (8.5 + spread * 0.55), anchorY - 5.0, anchorX + dir * 10.0, anchorY - 0.5);
        innerWing.curveTo(anchorX + dir * 7.0, anchorY + 4.0, anchorX + dir * 3.0, anchorY + 6.5, anchorX + dir * 1.0, anchorY + 5.5);
        innerWing.closePath();
        g2.setColor(main);
        g2.fill(innerWing);
    }

    private void drawBatWing(Graphics2D g2, boolean left, double anchorX, double anchorY, double spread, Color main, Color edge) {
        int dir = left ? -1 : 1;
        Path2D.Double wing = new Path2D.Double();
        wing.moveTo(anchorX, anchorY);
        wing.lineTo(anchorX + dir * (6.0 + spread), anchorY - 5.0);
        wing.lineTo(anchorX + dir * (13.0 + spread), anchorY - 1.0);
        wing.lineTo(anchorX + dir * 10.0, anchorY + 4.0);
        wing.lineTo(anchorX + dir * 6.0, anchorY + 2.0);
        wing.lineTo(anchorX + dir * 2.5, anchorY + 6.0);
        wing.closePath();
        g2.setColor(main);
        g2.fill(wing);
        g2.setColor(edge);
        g2.draw(wing);
    }

    private void drawHalo(Graphics2D g2, Color ring, Color glow, int cx, int cy, int width, int height) {
        g2.setColor(glow);
        g2.drawOval(cx - width / 2 - 1, cy - height / 2 - 1, width + 2, height + 2);
        g2.setColor(ring);
        g2.drawOval(cx - width / 2, cy - height / 2, width, height);
    }

    private void drawHorn(Graphics2D g2, boolean left, Color color) {
        int dir = left ? -1 : 1;
        Polygon horn = new Polygon(
                new int[]{dir * 1, dir * 6, dir * 3},
                new int[]{-8, -13, -8},
                3
        );
        g2.setColor(color);
        g2.fillPolygon(horn);
    }

    private void drawLaneCreepAttackEffect(Graphics2D g2, Creep creep, int sx, int sy, int radius) {
        if (creep.role != CreepRole.LANE || creep.attackAnimationTimer <= 0.0) {
            return;
        }

        double progress = 1.0 - clamp(creep.attackAnimationTimer / UNIT_ATTACK_ANIMATION_DURATION, 0.0, 1.0);
        double dirX = Math.cos(creep.lookAngle);
        double dirY = Math.sin(creep.lookAngle);

        switch (creep.laneType) {
            case MELEE -> drawMeleeCreepAttackEffect(g2, creep, sx, sy, radius, progress);
            case RANGED -> {
                int startX = (int) Math.round(sx + dirX * radius * 0.9);
                int startY = (int) Math.round(sy + dirY * radius * 0.9);
                int endX = (int) Math.round(startX + dirX * (10.0 + progress * 10.0) * ZOOM);
                int endY = (int) Math.round(startY + dirY * (10.0 + progress * 10.0) * ZOOM);
                Stroke oldStroke = g2.getStroke();
                g2.setColor(new Color(246, 222, 150, 185));
                g2.setStroke(new BasicStroke((float) Math.max(1.4, 2.6 * (1.0 - progress)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(startX, startY, endX, endY);
                g2.setColor(new Color(255, 246, 210, 220));
                g2.fillOval(endX - 3, endY - 3, 6, 6);
                g2.setStroke(oldStroke);
            }
            case CATAPULT -> {
                double launchDistance = (8.0 + progress * 18.0) * ZOOM;
                int stoneX = (int) Math.round(sx + dirX * (radius * 0.8 + launchDistance));
                int stoneY = (int) Math.round(sy + dirY * (radius * 0.8 + launchDistance) - Math.sin(progress * Math.PI) * 8.0 * ZOOM);
                g2.setColor(new Color(96, 84, 70, 210));
                g2.fillOval(stoneX - 5, stoneY - 5, 10, 10);
                g2.setColor(new Color(188, 160, 118, 180));
                g2.drawLine(
                        (int) Math.round(sx + dirX * radius * 0.6),
                        (int) Math.round(sy + dirY * radius * 0.6),
                        stoneX,
                        stoneY
                );
            }
        }
    }

    private void drawMeleeCreepAttackEffect(Graphics2D g2, Creep creep, int sx, int sy, int radius, double progress) {
        int arcRadius = (int) Math.round(radius + (7.0 + progress * 5.0) * ZOOM);
        int startAngle = (int) Math.round(Math.toDegrees(-creep.lookAngle) - 34.0);
        Stroke oldStroke = g2.getStroke();
        g2.setColor(creep.team == Team.LIGHT
                ? new Color(214, 255, 226, 195)
                : new Color(255, 214, 206, 195));
        g2.setStroke(new BasicStroke((float) Math.max(1.6, 2.8 * (1.0 - progress)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(sx - arcRadius, sy - arcRadius, arcRadius * 2, arcRadius * 2, startAngle, 68);
        g2.setStroke(oldStroke);
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
        double outerR = currentWeapon.meleeRange() * ATTACK_RANGE_BALANCE_SCALE * ZOOM;
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

    private static final class ClickMarker {
        private double x;
        private double y;
        private double lifetime;
        private boolean attack;
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
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            centerCameraOnPlayer();
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            toggleEditorMode();
            return;
        }
        if (editorMode && isEditorSaveShortcut(e)) {
            saveEditedMap();
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ALT -> {
                if (!editorMode) {
                    showAttackRanges = true;
                }
            }
            case KeyEvent.VK_UP -> cameraUp = true;
            case KeyEvent.VK_DOWN -> cameraDown = true;
            case KeyEvent.VK_LEFT -> cameraLeft = true;
            case KeyEvent.VK_RIGHT -> cameraRight = true;
            case KeyEvent.VK_1 -> {
                if (!editorMode) {
                    switchWeapon(WeaponType.STONE);
                }
            }
            case KeyEvent.VK_2 -> {
                if (!editorMode) {
                    switchWeapon(WeaponType.BOW);
                }
            }
            case KeyEvent.VK_3 -> {
                if (!editorMode) {
                    switchWeapon(WeaponType.SWORD);
                }
            }
            case KeyEvent.VK_Q -> {
                if (!editorMode) {
                    triggerAbility(AbilitySlot.PRIMARY);
                }
            }
            case KeyEvent.VK_E -> {
                if (!editorMode) {
                    triggerAbility(AbilitySlot.SECONDARY);
                }
            }
            case KeyEvent.VK_R -> {
                if (editorMode) {
                    return;
                }
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
            case KeyEvent.VK_ALT -> showAttackRanges = false;
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
        if (miniMapDragging) {
            centerCameraOnMiniMapPoint(e.getX(), e.getY());
        } else if (editorMode && editorDraggedStructure != null) {
            dragEditorStructureToScreen(e.getX(), e.getY());
        } else if (editorMode && editorPainting && !isInsideEditorPanel(e.getX(), e.getY()) && !isInsideMiniMap(e.getX(), e.getY())) {
            applyEditorBrushAtScreen(e.getX(), e.getY());
        } else if (middleMouseDragging) {
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

        if (editorMode) {
            if (isInsideEditorPanel(e.getX(), e.getY())) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleEditorPanelClick(e.getX(), e.getY());
                }
                return;
            }
            if (isMiniMapCameraButton(e.getButton()) && isInsideMiniMap(e.getX(), e.getY())) {
                miniMapDragging = true;
                centerCameraOnMiniMapPoint(e.getX(), e.getY());
                return;
            }
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (editorTool == EditorTool.MOVE_STRUCTURE) {
                    editorDraggedStructure = findStructureForEditor(screenToWorldX(e.getX()), screenToWorldY(e.getY()));
                    if (editorDraggedStructure != null) {
                        dragEditorStructureToScreen(e.getX(), e.getY());
                    }
                } else {
                    editorPainting = true;
                    applyEditorBrushAtScreen(e.getX(), e.getY());
                }
                return;
            }
            if (e.getButton() == MouseEvent.BUTTON3) {
                sampleEditorBrushAtScreen(e.getX(), e.getY());
                return;
            }
            if (e.getButton() == MouseEvent.BUTTON2) {
                middleMouseDragging = true;
                dragLastMouseX = e.getX();
                dragLastMouseY = e.getY();
                return;
            }
            return;
        }

        if (isMiniMapCameraButton(e.getButton()) && isInsideMiniMap(e.getX(), e.getY())) {
            miniMapDragging = true;
            centerCameraOnMiniMapPoint(e.getX(), e.getY());
            return;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());
            CombatEntity clickedTarget = findClickedHostileTarget(worldX, worldY);
            addClickMarker(worldX, worldY, clickedTarget != null);
            if (clickedTarget != null) {
                issueAttackOrder(clickedTarget);
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
        if (isMiniMapCameraButton(e.getButton())) {
            miniMapDragging = false;
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            editorPainting = false;
            editorDraggedStructure = null;
        }
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
        miniMapDragging = false;
        editorPainting = false;
        editorDraggedStructure = null;
        middleMouseDragging = false;
    }

    @Override
    public void removeNotify() {
        gameTimer.stop();
        audio.close();
        super.removeNotify();
    }

    private interface TileGoal {
        boolean isGoal(int tileX, int tileY);

        double heuristic(int tileX, int tileY);
    }

    private interface TileWalkability {
        boolean isWalkable(int tileX, int tileY);
    }

    private record LaneSample(double progress, double totalLength) {
    }

    private record LaneAnchor(double x, double y, double dirX, double dirY) {
    }

    private record LaneGuidance(double targetX, double targetY, double dirX, double dirY) {
    }

    private record CreepLaneProgress(Creep creep, LaneSample sample) {
    }

    private enum EditorTool {
        PAINT,
        MOVE_STRUCTURE
    }

    private record EditorLabel(String text, int x, int y) {
    }

    private record EditorButton(String id,
                                Rectangle bounds,
                                String label,
                                boolean selected,
                                Color fill,
                                Color text) {
    }

    private record EditorUi(Rectangle panelBounds,
                            Rectangle previewBounds,
                            List<EditorLabel> labels,
                            List<EditorButton> buttons) {
    }

    private record PathNode(int tileX, int tileY, double priority) {
    }

    private record PathSearchResult(List<Point> tiles, Point goalTile) {
    }
}
