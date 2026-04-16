package com.fordmc.fmutils.client.cinematic.presets;

import com.fordmc.fmutils.client.cinematic.BasePreset;
import com.fordmc.fmutils.client.cinematic.CinematicMath;
import net.minecraft.world.phys.Vec3;

/**
 * A cinematic sweep that starts close and pulls back significantly.
 */
public class EstablishingShotPreset extends BasePreset {
    private final Vec3 direction;
    private final double startRadius = 3.5;
    private final double endRadius = 18.0;
    private final double height;

    public EstablishingShotPreset(long seed, double baseDistance, double baseHeight) {
        super(seed);
        // Random direction for pull-back
        double angle = random.nextDouble() * Math.PI * 2;
        this.direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        this.height = baseHeight + 1.5;
    }

    @Override
    public Vec3 getOffset(double t, double duration) {
        double progress = CinematicMath.cubicBezier(t / duration);
        double currentRadius = startRadius + (endRadius - startRadius) * progress;
        
        // Add subtle arc
        double arc = Math.sin(progress * Math.PI) * 2.0;
        
        return direction.scale(currentRadius).add(0, height + arc, 0);
    }

    @Override
    public float getFovModifier(double t, double duration) {
        // Dramatic dolly zoom: as we pull back, we zoom in slightly to keep player size relevant
        double progress = t / duration;
        return (float) (progress * -25.0f);
    }
}
