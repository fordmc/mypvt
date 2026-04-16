package com.fordmc.fmutils.client.cinematic;

import java.util.Random;

public abstract class BasePreset implements ICinematicPreset {
    protected final long seed;
    protected final Random random;

    public BasePreset(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    protected double getNoise(double t) {
        return CinematicMath.noise(t, seed);
    }

    protected double getNoise(double t, int offset) {
        return CinematicMath.noise(t, seed + offset);
    }

    @Override
    public float getFovModifier(double t, double duration) {
        return 0f; // Default No FOV change
    }

    @Override
    public boolean isFollowMode() {
        return true;
    }
}
