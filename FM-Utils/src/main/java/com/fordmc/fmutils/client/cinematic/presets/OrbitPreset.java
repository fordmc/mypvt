package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import com.fordmc.fmutils.client.cinematic.CinematicMath;
import net.minecraft.world.phys.Vec3;

public class OrbitPreset extends BasePreset {
    private final double radius;
    private final double speed;
    private final double height;
    private final double verticalSpeed;
    private final double verticalAmp;
    private final double radiusOscSpeed;
    private final double radiusOscAmp;

    public OrbitPreset(long seed, double baseRadius, double baseHeight, double baseSpeed) {
        super(seed);
        // Wider range for radius to support "Far" views
        this.radius = baseRadius * (0.8 + random.nextDouble() * 1.5);
        this.speed = baseSpeed * (random.nextBoolean() ? 1 : -1) * (0.6 + random.nextDouble() * 0.4);
        this.height = baseHeight + random.nextDouble() * 3.0;
        
        // Randomize orbit complexity
        this.verticalSpeed = 0.15 + random.nextDouble() * 0.25;
        this.verticalAmp = 0.8 + random.nextDouble() * 2.0;
        this.radiusOscSpeed = 0.05 + random.nextDouble() * 0.15;
        this.radiusOscAmp = 1.0 + random.nextDouble() * 3.0;
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        double angle = t * speed;
        
        // Breathing radius
        double currentR = radius + Math.sin(t * radiusOscSpeed) * radiusOscAmp;
        
        // Vertical bobbing (Lissajous)
        double currentY = height + Math.sin(t * verticalSpeed) * verticalAmp;
        
        // Add subtle organic noise to position
        double noiseX = getNoise(t, 0) * 0.2;
        double noiseY = getNoise(t, 1) * 0.1;
        double noiseZ = getNoise(t, 2) * 0.2;

        return new Vec3(
            Math.cos(angle) * currentR + noiseX,
            currentY + noiseY,
            Math.sin(angle) * currentR + noiseZ
        );
    }

    @Override
    public float getFovModifier(double t, double duration) {
        // Distance-based zoom: keep player size relatively consistent
        return (float) (Math.sin(t * 0.1) * 3.0f - (radius > 8 ? (radius - 8) * 2.5f : 0));
    }
}
