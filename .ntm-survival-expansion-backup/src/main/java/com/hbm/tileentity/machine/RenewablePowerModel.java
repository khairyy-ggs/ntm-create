package com.hbm.tileentity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/** World-sampling adapter for the photovoltaic panel and wind generator. */
public final class RenewablePowerModel {
    public static final long SOLAR_RATED_HE_PER_TICK = 1_500L;
    public static final long WIND_RATED_HE_PER_TICK = 2_000L;

    private RenewablePowerModel() {}

    public static long solarOutput(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || !level.dimensionType().hasSkyLight()) return 0L;
        BlockPos sample = pos.above();
        if (!level.canSeeSky(sample)) return 0L;
        long dayTime = Math.floorMod(level.getDayTime(), 24_000L);
        boolean daytime = dayTime < 12_000L;
        int skyLight = level.getBrightness(LightLayer.SKY, sample);
        return RenewablePowerMath.solarOutput(SOLAR_RATED_HE_PER_TICK, skyLight, daytime,
                level.isRainingAt(sample), level.isThundering());
    }

    public static long windOutput(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || !level.dimensionType().hasSkyLight()) return 0L;
        BlockPos sample = pos.above();
        if (!level.canSeeSky(sample)) return 0L;

        double altitude = Math.max(0.0D, Math.min(1.0D,
                (pos.getY() - level.getSeaLevel()) / 128.0D));
        long positionSeed = (pos.asLong() ^ (pos.asLong() >>> 17)) & 0xFFFFL;
        double phase = (level.getGameTime() + positionSeed) / 1_200.0D;
        double gust = 0.85D + 0.15D * Math.sin(phase);
        return RenewablePowerMath.windOutput(WIND_RATED_HE_PER_TICK, altitude, gust,
                level.isRainingAt(sample), level.isThundering());
    }
}
