package com.fordmc.fmutils.client.cinematic;

import net.minecraft.world.phys.Vec3;

/**
 * Defines the behavior of a cinematic camera shot.
 */
public interface ICinematicPreset {
    /**
     * Calculates the offset from the target (player) head position.
     * @param t Current time in seconds within the shot.
     * @param duration Total duration of the shot.
     * @return Offset vector.
     */
    Vec3 getOffset(double t, double duration);

    /**
     * Calculates the FOV modifier for this point in time.
     * @param t Current time in seconds within the shot.
     * @param duration Total duration of the shot.
     * @return FOV offset (e.g., -20 for zoom).
     */
    float getFovModifier(double t, double duration);

    /**
     * Whether this preset should follow the player's rotation or use its own logic.
     */
    boolean isFollowMode();

    /**
     * Called every tick to update internal state (e.g., inertia).
     */
    default void tick() {}
}
