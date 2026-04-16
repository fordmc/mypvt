package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import com.fordmc.fmutils.client.cinematic.CinematicMath;
import net.minecraft.world.phys.Vec3;

public class SideShotPreset extends BasePreset {
    private final Vec3 baseOffset;
    private final Vec3 slideDirection;
    private final double speed;

    public SideShotPreset(long seed, double baseRadius, double baseHeight) {
        super(seed);
        // Positioned to the side
        this.baseOffset = new Vec3(baseRadius, baseHeight, 0);
        this.slideDirection = new Vec3(0, 0, 1);
        this.speed = (random.nextBoolean() ? 1 : -1) * (0.5 + random.nextDouble() * 1.5);
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        double offset = (t - duration / 2.0) * speed;
        return baseOffset.add(slideDirection.scale(offset));
    }

    @Override
    public boolean isFollowMode() {
        return true; 
    }
}
