package com.fordmc.fmutils.client.cinematic;

import net.minecraft.util.Mth;

public class CinematicMath {
    /**
     * Standard smoothstep interpolation.
     */
    public static double smoothstep(double x) {
        x = Mth.clamp(x, 0.0, 1.0);
        return x * x * (3 - 2 * x);
    }

    /**
     * Cubic easing for ultra-smooth transitions.
     */
    public static double cubicBezier(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * A simple coherent noise function using overlapping sine waves with prime-related frequencies.
     * Use this for organic handheld shake or subtle variations.
     */
    public static double noise(double t, long seed) {
        double s = seed % 1000 / 100.0;
        return (Math.sin(t * 1.0 + s) * 0.5 +
                Math.sin(t * 2.31 + s * 1.5) * 0.25 +
                Math.sin(t * 5.11 + s * 0.7) * 0.125 +
                Math.sin(t * 0.47 + s * 2.1) * 0.125);
    }

    /**
     * Wraps degrees to -180...180 range.
     */
    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) f -= 360.0F;
        if (f < -180.0F) f += 360.0F;
        return f;
    }
}
