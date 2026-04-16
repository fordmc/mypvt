package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import com.fordmc.fmutils.client.cinematic.CinematicMath;
import net.minecraft.world.phys.Vec3;

public class DriftPreset extends BasePreset {
    private final Vec3 startOffset;
    private final Vec3 driftVector;
    private final double speed;

    public DriftPreset(long seed, double baseRadius, double baseHeight) {
        super(seed);
        this.speed = 0.05 + random.nextDouble() * 0.1;
        
        // Pick a random starting point on a sphere/cylinder segment
        double angle = random.nextDouble() * Math.PI * 2;
        this.startOffset = new Vec3(
            Math.cos(angle) * baseRadius,
            baseHeight + (random.nextDouble() - 0.5) * 2.0,
            Math.sin(angle) * baseRadius
        );

        // Drift direction (wider range for sweeps)
        this.driftVector = new Vec3(
            (random.nextDouble() - 0.5) * 8.0,
            (random.nextDouble() - 0.5) * 2.0,
            (random.nextDouble() - 0.5) * 8.0
        );
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        // Use smoothstep for the drift progress to avoid sudden starts
        double progress = CinematicMath.smoothstep(t / duration);
        Vec3 pos = startOffset.add(driftVector.scale(progress * speed * duration));
        
        // Add handheld noise
        double noiseScale = 0.15;
        return pos.add(
            getNoise(t, 0) * noiseScale,
            getNoise(t, 1) * noiseScale,
            getNoise(t, 2) * noiseScale
        );
    }

    @Override
    public float getFovModifier(double t, double duration) {
        // Slow FOV creep for tension
        return (float) (t * 0.5); 
    }
}
