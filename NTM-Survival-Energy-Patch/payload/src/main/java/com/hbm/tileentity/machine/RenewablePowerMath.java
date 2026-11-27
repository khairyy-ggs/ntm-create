package com.hbm.tileentity.machine;

/**
 * Deterministic arithmetic shared by the survival renewable generators.
 * This class has no Minecraft dependencies so it can be unit-tested on its own.
 */
public final class RenewablePowerMath {
    private RenewablePowerMath() {}

    public static long solarOutput(long ratedOutput, int skyLight, boolean daytime,
                                   boolean raining, boolean thundering) {
        if (!daytime || skyLight <= 0 || ratedOutput <= 0L) return 0L;
        double lightFactor = clamp01(skyLight / 15.0D);
        double weatherFactor = thundering ? 0.42D : raining ? 0.68D : 1.0D;
        return Math.max(0L, Math.round(ratedOutput * lightFactor * weatherFactor));
    }

    public static long windOutput(long ratedOutput, double altitudeFactor, double gustFactor,
                                  boolean raining, boolean thundering) {
        if (ratedOutput <= 0L) return 0L;
        double altitude = clamp01(altitudeFactor);
        double gust = Math.max(0.70D, Math.min(1.0D, gustFactor));
        double terrainFactor = 0.78D + altitude * 0.42D;
        double weatherFactor = thundering ? 1.18D : raining ? 1.08D : 1.0D;
        return Math.max(0L, Math.round(ratedOutput * terrainFactor * gust * weatherFactor));
    }

    public static long addToBuffer(long stored, long generated, long capacity) {
        long safeCapacity = Math.max(0L, capacity);
        long safeStored = Math.max(0L, Math.min(safeCapacity, stored));
        long safeGenerated = Math.max(0L, generated);
        return safeGenerated > safeCapacity - safeStored ? safeCapacity : safeStored + safeGenerated;
    }

    public static long clampPower(long power, long capacity) {
        return Math.max(0L, Math.min(Math.max(0L, capacity), power));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
