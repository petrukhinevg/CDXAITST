package com.example.demo.game.collision;

import com.example.demo.game.model.CombatEntity;
import org.dyn4j.collision.narrowphase.Gjk;
import org.dyn4j.collision.narrowphase.Penetration;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

import java.util.List;

public final class UnitCollisionResolver {
    private final Gjk detector = new Gjk();

    @FunctionalInterface
    public interface MovementAdapter {
        boolean tryMove(CombatEntity entity, double targetX, double targetY);
    }

    public void resolve(List<? extends CombatEntity> units, MovementAdapter movementAdapter) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < units.size(); i++) {
                for (int j = i + 1; j < units.size(); j++) {
                    resolvePair(units.get(i), units.get(j), movementAdapter);
                }
            }
        }
    }

    private void resolvePair(CombatEntity first, CombatEntity second, MovementAdapter movementAdapter) {
        Circle firstShape = Geometry.createCircle(first.getRadius());
        Circle secondShape = Geometry.createCircle(second.getRadius());

        Transform firstTransform = new Transform();
        firstTransform.translate(first.getX(), first.getY());

        Transform secondTransform = new Transform();
        secondTransform.translate(second.getX(), second.getY());

        Penetration penetration = new Penetration();
        if (!detector.detect(firstShape, firstTransform, secondShape, secondTransform, penetration)) {
            return;
        }

        double depth = penetration.getDepth();
        if (depth <= 0.0) {
            return;
        }

        Vector2 normal = penetration.getNormal();
        double nx = normal.x;
        double ny = normal.y;
        if (Math.abs(nx) < 1.0e-6 && Math.abs(ny) < 1.0e-6) {
            double dx = second.getX() - first.getX();
            double dy = second.getY() - first.getY();
            double len = Math.hypot(dx, dy);
            nx = len < 1.0e-6 ? 1.0 : dx / len;
            ny = len < 1.0e-6 ? 0.0 : dy / len;
        }

        boolean movedFirst = movementAdapter.tryMove(first,
                first.getX() - nx * depth * 0.5,
                first.getY() - ny * depth * 0.5);
        boolean movedSecond = movementAdapter.tryMove(second,
                second.getX() + nx * depth * 0.5,
                second.getY() + ny * depth * 0.5);

        if (!movedFirst) {
            movementAdapter.tryMove(second, second.getX() + nx * depth, second.getY() + ny * depth);
        }
        if (!movedSecond) {
            movementAdapter.tryMove(first, first.getX() - nx * depth, first.getY() - ny * depth);
        }
    }
}
