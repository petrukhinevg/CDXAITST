package com.example.demo.game.world.blueprint;

import com.example.demo.game.model.LaneType;
import com.example.demo.game.model.Structure;
import com.example.demo.game.model.StructureType;
import com.example.demo.game.model.Team;
import com.example.demo.game.world.GameMap;
import com.example.demo.game.world.element.MapElements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapBlueprintRoundTripTests {

    @TempDir
    Path tempDir;

    @Test
    void saveLoadPreservesTreeStateAndTileLayers() throws Exception {
        GameMap source = new GameMap(3, 3, 24);
        source.reset(new Random(7));

        source.setGround(1, 1, MapElements.GRASS);
        source.setBlocked(1, 1, true);
        source.setTreeVariant(1, 1, 3);
        source.setTreeTint(1, 1, -1);

        source.setGround(2, 1, MapElements.FOREST);
        source.setBlocked(2, 1, true);
        source.setTreeVariant(2, 1, 1);
        source.setTreeTint(2, 1, 2);

        source.setGround(0, 2, MapElements.DIRT);
        source.setWater(0, 2, MapElements.RIVER);
        source.setLaneMask(0, 2, laneBit(LaneType.TOP) | laneBit(LaneType.MID));

        source.setProp(0, 0, MapElements.BUSH);
        source.setProp(2, 2, MapElements.BOULDER);
        source.setBlocked(2, 2, true);

        EnumMap<LaneType, List<Point>> lanePaths = new EnumMap<>(LaneType.class);
        for (LaneType laneType : LaneType.values()) {
            lanePaths.put(laneType, new ArrayList<>());
        }
        lanePaths.get(LaneType.TOP).add(new Point(0, 2));
        lanePaths.get(LaneType.TOP).add(new Point(1, 2));

        List<Structure> structures = List.of(
                structure(Team.LIGHT, StructureType.THRONE, null, source, 0, 1, 999),
                structure(Team.DARK, StructureType.THRONE, null, source, 2, 0, 999),
                structure(Team.LIGHT, StructureType.TOWER, LaneType.TOP, source, 0, 2, 0)
        );

        Path file = tempDir.resolve("roundtrip.map");
        new MapBlueprintWriter().save(file, source, new Point(1, 2), lanePaths, structures);

        String text = Files.readString(file);
        assertTrue(text.contains("tv3"));
        assertTrue(text.contains("tt1"));
        assertTrue(text.contains("tv1"));
        assertTrue(text.contains("tt4"));

        MapBlueprint blueprint = new MapBlueprintLoader().load(file);
        GameMap restored = new GameMap(3, 3, 24);
        blueprint.applyTo(restored, new Random(99));

        assertEquals(MapElements.GRASS, restored.getGround(1, 1));
        assertTrue(restored.isBlocked(1, 1));
        assertEquals(3, restored.getTreeVariant(1, 1));
        assertEquals(-1, restored.getTreeTint(1, 1));

        assertEquals(MapElements.FOREST, restored.getGround(2, 1));
        assertTrue(restored.isBlocked(2, 1));
        assertEquals(1, restored.getTreeVariant(2, 1));
        assertEquals(2, restored.getTreeTint(2, 1));

        assertEquals(MapElements.RIVER, restored.getWater(0, 2));
        assertTrue(restored.hasLaneType(0, 2, LaneType.TOP));
        assertTrue(restored.hasLaneType(0, 2, LaneType.MID));
        assertEquals(MapElements.BUSH, restored.getProp(0, 0));
        assertEquals(MapElements.BOULDER, restored.getProp(2, 2));
        assertTrue(restored.isBlocked(2, 2));
    }

    private Structure structure(Team team,
                                StructureType type,
                                LaneType laneType,
                                GameMap map,
                                int tileX,
                                int tileY,
                                int laneOrder) {
        Structure structure = new Structure();
        structure.team = team;
        structure.type = type;
        structure.lane = laneType;
        structure.laneOrder = laneOrder;
        structure.x = map.tileCenter(tileX);
        structure.y = map.tileCenter(tileY);
        structure.hp = 100;
        structure.maxHp = 100;
        return structure;
    }

    private int laneBit(LaneType laneType) {
        return switch (laneType) {
            case TOP -> 1;
            case MID -> 1 << 1;
            case BOT -> 1 << 2;
        };
    }
}
