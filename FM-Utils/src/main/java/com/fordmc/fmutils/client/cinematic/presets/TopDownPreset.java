package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import net.minecraft.world.phys.Vec3;

public class TopDownPreset extends BasePreset {
    private final double height;
    private final double rotationSpeed;

    public TopDownPreset(long seed, double baseDistance) {
        super(seed);
        this.height = baseDistance * 2.5 + random.nextDouble() * 5.0;
        this.rotationSpeed = (random.nextBoolean() ? 0.05 : -0.05) + (random.nextDouble() - 0.5) * 0.1;
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        double angle = t * rotationSpeed;
        double r = 1.5; // Slight circular drift even at top down
        return new Vec3(
            Math.cos(angle) * r,
            height,
            Math.sin(angle) * r
        );
    }

    @Override
    public float getFovModifier(double t, double duration) {
        return -10f; // Bit of zoom to keep player relevant
    }
}
