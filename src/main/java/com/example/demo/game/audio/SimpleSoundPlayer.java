package com.example.demo.game.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class SimpleSoundPlayer implements AutoCloseable {
    private static final float SAMPLE_RATE = 22_050.0f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<SoundEffect, byte[]> sounds = new EnumMap<>(SoundEffect.class);
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "game-audio");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean closed;

    public SimpleSoundPlayer() {
        sounds.put(SoundEffect.FOOTSTEP_LEFT, sequence(
                tone(1800.0, 980.0, 0.024, 0.040, Wave.NOISE),
                tone(1320.0, 760.0, 0.032, 0.030, Wave.NOISE),
                tone(520.0, 680.0, 0.018, 0.010, Wave.SINE)
        ));
        sounds.put(SoundEffect.FOOTSTEP_RIGHT, sequence(
                tone(1960.0, 1120.0, 0.022, 0.038, Wave.NOISE),
                tone(1440.0, 820.0, 0.030, 0.028, Wave.NOISE),
                tone(610.0, 760.0, 0.016, 0.010, Wave.SINE)
        ));
        sounds.put(SoundEffect.WEAPON_SWITCH, sequence(
                tone(760.0, 1180.0, 0.070, 0.030, Wave.NOISE),
                tone(980.0, 420.0, 0.110, 0.026, Wave.NOISE),
                tone(620.0, 860.0, 0.055, 0.018, Wave.SINE)
        ));
        sounds.put(SoundEffect.PROJECTILE_SHOT, sequence(
                tone(1280.0, 620.0, 0.050, 0.050, Wave.NOISE),
                tone(940.0, 520.0, 0.070, 0.040, Wave.TRIANGLE),
                tone(460.0, 360.0, 0.028, 0.020, Wave.SINE)
        ));
        sounds.put(SoundEffect.PROJECTILE_IMPACT, sequence(
                tone(2200.0, 780.0, 0.020, 0.040, Wave.NOISE),
                tone(980.0, 280.0, 0.050, 0.045, Wave.TRIANGLE),
                tone(240.0, 180.0, 0.050, 0.028, Wave.SINE)
        ));
        sounds.put(SoundEffect.MELEE_SWING, sequence(
                tone(520.0, 180.0, 0.070, 0.038, Wave.NOISE),
                tone(280.0, 140.0, 0.085, 0.060, Wave.TRIANGLE)
        ));
        sounds.put(SoundEffect.MELEE_HIT, sequence(
                tone(820.0, 240.0, 0.034, 0.050, Wave.NOISE),
                tone(220.0, 120.0, 0.070, 0.055, Wave.SQUARE)
        ));
        sounds.put(SoundEffect.STRUCTURE_SHOT, sequence(
                tone(720.0, 1120.0, 0.050, 0.034, Wave.NOISE),
                tone(640.0, 420.0, 0.080, 0.032, Wave.SINE),
                tone(330.0, 250.0, 0.060, 0.020, Wave.TRIANGLE)
        ));
        sounds.put(SoundEffect.PLAYER_HIT, sequence(
                tone(240.0, 160.0, 0.080, 0.16, Wave.SQUARE),
                tone(180.0, 120.0, 0.070, 0.12, Wave.SINE)
        ));
        sounds.put(SoundEffect.ENEMY_DOWN, sequence(
                tone(410.0, 240.0, 0.090, 0.15, Wave.SINE),
                tone(220.0, 130.0, 0.090, 0.11, Wave.TRIANGLE)
        ));
        sounds.put(SoundEffect.VICTORY, sequence(
                tone(523.25, 523.25, 0.080, 0.14, Wave.SINE),
                silence(0.020),
                tone(659.25, 659.25, 0.090, 0.15, Wave.SINE),
                silence(0.020),
                tone(783.99, 783.99, 0.120, 0.16, Wave.SINE)
        ));
        sounds.put(SoundEffect.DEFEAT, sequence(
                tone(329.63, 220.00, 0.120, 0.16, Wave.TRIANGLE),
                silence(0.015),
                tone(220.00, 146.83, 0.180, 0.14, Wave.SINE)
        ));
    }

    public void play(SoundEffect effect) {
        if (closed || effect == null) {
            return;
        }
        byte[] pcm = sounds.get(effect);
        if (pcm == null || pcm.length == 0) {
            return;
        }
        try {
            executor.execute(() -> playPcm(pcm));
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void playPcm(byte[] pcm) {
        if (closed) {
            return;
        }

        SourceDataLine line = null;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(FORMAT, Math.max(2048, pcm.length));
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception ignored) {
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    private byte[] sequence(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private byte[] silence(double durationSeconds) {
        return new byte[Math.max(1, frames(durationSeconds) * 2)];
    }

    private byte[] tone(double startHz, double endHz, double durationSeconds, double amplitude, Wave wave) {
        int frames = frames(durationSeconds);
        byte[] pcm = new byte[frames * 2];
        double phase = 0.0;
        int noise = 0x13579bdf;

        for (int i = 0; i < frames; i++) {
            double progress = frames == 1 ? 1.0 : i / (double) (frames - 1);
            double frequency = startHz + (endHz - startHz) * progress;
            phase += Math.PI * 2.0 * frequency / SAMPLE_RATE;

            double envelope = envelope(progress);
            noise = noise * 1664525 + 1013904223;
            double sample = sampleWave(phase, wave, noise) * amplitude * envelope;
            short value = (short) Math.round(sample * Short.MAX_VALUE);
            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }

        return pcm;
    }

    private int frames(double durationSeconds) {
        return Math.max(1, (int) Math.round(durationSeconds * SAMPLE_RATE));
    }

    private double envelope(double progress) {
        if (progress < 0.12) {
            return progress / 0.12;
        }
        if (progress > 0.72) {
            return Math.max(0.0, 1.0 - (progress - 0.72) / 0.28);
        }
        return 1.0;
    }

    private double sampleWave(double phase, Wave wave, int noise) {
        return switch (wave) {
            case SINE -> Math.sin(phase);
            case TRIANGLE -> 2.0 * Math.abs(2.0 * (phase / (Math.PI * 2.0) - Math.floor(phase / (Math.PI * 2.0) + 0.5))) - 1.0;
            case SQUARE -> Math.sin(phase) >= 0.0 ? 1.0 : -1.0;
            case NOISE -> ((noise >>> 8) & 0xffff) / 32767.5 - 1.0;
        };
    }

    private enum Wave {
        SINE,
        TRIANGLE,
        SQUARE,
        NOISE
    }
}
