package com.example.demo.game.audio;

import com.example.demo.game.model.WeaponType;

public final class GameAudio implements AutoCloseable {
    private static final double PLAYER_FOOTSTEP_INTERVAL = 0.24;

    private final SimpleSoundPlayer soundPlayer = new SimpleSoundPlayer();
    private double playerFootstepTimer;
    private boolean nextFootstepLeft = true;

    public void reset() {
        resetPlayerMovementLoop();
    }

    public void resetPlayerMovementLoop() {
        playerFootstepTimer = 0.0;
        nextFootstepLeft = true;
    }

    public void updatePlayerMovement(double dt, boolean moved, boolean canPlayFootsteps) {
        if (!moved || !canPlayFootsteps) {
            resetPlayerMovementLoop();
            return;
        }

        playerFootstepTimer -= dt;
        if (playerFootstepTimer > 0.0) {
            return;
        }

        soundPlayer.play(nextFootstepLeft ? SoundEffect.FOOTSTEP_LEFT : SoundEffect.FOOTSTEP_RIGHT);
        nextFootstepLeft = !nextFootstepLeft;
        playerFootstepTimer += PLAYER_FOOTSTEP_INTERVAL;
    }

    public void onWeaponSwitch() {
        soundPlayer.play(SoundEffect.WEAPON_SWITCH);
    }

    public void onPlayerAttack(WeaponType weapon) {
        soundPlayer.play(weapon.projectile() ? SoundEffect.PROJECTILE_SHOT : SoundEffect.MELEE_SWING);
    }

    public void onProjectileImpact() {
        soundPlayer.play(SoundEffect.PROJECTILE_IMPACT);
    }

    public void onMeleeImpact() {
        soundPlayer.play(SoundEffect.MELEE_HIT);
    }

    public void onStructureAttack() {
        soundPlayer.play(SoundEffect.STRUCTURE_SHOT);
    }

    public void onPlayerHit() {
        soundPlayer.play(SoundEffect.PLAYER_HIT);
    }

    public void onEnemyDown() {
        soundPlayer.play(SoundEffect.ENEMY_DOWN);
    }

    public void onVictory() {
        soundPlayer.play(SoundEffect.VICTORY);
    }

    public void onDefeat() {
        soundPlayer.play(SoundEffect.DEFEAT);
    }

    @Override
    public void close() {
        soundPlayer.close();
    }
}
