package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import com.fordmc.fmutils.client.cinematic.CinematicMath;
import net.minecraft.world.phys.Vec3;

public class ZoomPreset extends BasePreset {
    private final Vec3 fixedPos;
    private final float startFov;
    private final float endFov;

    public ZoomPreset(long seed, double baseRadius, double baseHeight) {
        super(seed);
        double angle = random.nextDouble() * Math.PI * 2;
        this.fixedPos = new Vec3(
            Math.cos(angle) * (baseRadius * 1.5),
            baseHeight + 1.0,
            Math.sin(angle) * (baseRadius * 1.5)
        );
        this.startFov = 0f;
        this.endFov = -30f - random.nextFloat() * 20f;
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        // Very slow movement towards player
        double progress = t / duration;
        return fixedPos.lerp(fixedPos.scale(0.8), progress);
    }

    @Override
    public float getFovModifier(double t, double duration) {
        double progress = CinematicMath.smoothstep(t / duration);
        return (float) (startFov + (endFov - startFov) * progress);
    }
}
