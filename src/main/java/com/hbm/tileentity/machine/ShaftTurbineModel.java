package com.hbm.tileentity.machine;

/**
 * Control model of the Create Energy Shaft: stored power in, stress units and RPM out.
 * Pure Java on purpose (no Minecraft types) so it can be unit tested without a game.
 * All balance numbers are placeholders chosen for this design; tune them in play.
 *
 * The player's job is to tune the minimum water temperature until the turbine reads stable.
 * Steam (fed up to a limit the player sets) raises the output for the same stored power, but it also
 * destabilizes the turbine; an unstable turbine fed steam runs a critical countdown and explodes.
 */
public final class ShaftTurbineModel {

    /** Hard ceiling of the stress this turbine may offer to a Create network. */
    public static final long MAX_SU = 200_000L;
    public static final int MAX_RPM = 256;
    public static final int RPM_STEP = 8;
    public static final long MAX_POWER = 5_000_000L;
    /** Stored HE at which the output reaches its ceiling (output is linear in stored power below it). */
    public static final long FULL_OUTPUT_POWER = 1_000_000L;
    /** HE drawn per tick for every SU offered without steam; 5M SU costs 5,000 HE/t. */
    public static final double HE_PER_SU = 0.005D;
    public static final long START_POWER = 25_000L;
    public static final int SU_STEP = 2_000;
    public static final int PUBLISH_COOLDOWN_TICKS = 20;
    /** Share of the output that flows from a perfectly steady supply; the rest depends on flow stability. */
    public static final double STABLE_FLOOR = 0.6D;

    public static final double RAMP_BASE = 1.0D / 600D;
    public static final double RAMP_STEAM = 1.0D / 200D;

    // steam
    public static final int STEAM_MB_MAX = 400;
    /** Extra output at full steam: 1.8x the stress for the same stored power (and the same factor less power drawn). */
    public static final double STEAM_GAIN = 0.8D;
    public static final double STEAM_DESTAB = 0.8D;

    // water
    public static final int WATER_MB_AT_FULL = 400;
    public static final double REF_DELTA_T = 60D;
    public static final int MIN_TEMP_LOW = 40;
    public static final int MIN_TEMP_HIGH = 95;
    /** The minimum water temperature that balances the turbine at a given effective load. */
    public static final int IDEAL_LOW = 45;
    public static final int IDEAL_HIGH = 90;
    public static final double TEMP_TOLERANCE = 5D;
    public static final double TEMP_SPAN = 25D;

    // stability and the critical countdown
    public static final double STAB_RATE = 0.05D;
    public static final double STABLE_AT = 0.75D;
    public static final double WARN_BELOW = 0.5D;
    public static final double CRIT_BELOW = 0.25D;
    public static final double CRIT_RECOVER_AT = 0.4D;
    public static final int EXPLODE_TICKS = 200;
    /** Without steam the turbine only derates when badly tuned; it never goes critical. */
    public static final double NO_STEAM_FLOOR = 0.35D;

    // state
    public double load;
    public double flowStability = 1D;
    public double stability = 1D;
    public int critTicks;
    public boolean running;
    public int publishedSu;
    public int publishedRpm;
    private int sinceAck = PUBLISH_COOLDOWN_TICKS;
    private boolean lastLimited;

    // controls
    public int throttlePercent = 100;
    public int steamLimitPercent;
    public int minWaterTemp = 70;

    public record Inputs(long power, long inflowLastTick, int steamMb, int coldWaterMb, int inletTemp, int hotRoomMb) {}

    public record Outputs(long heUsed, int steamUsed, int waterUsed, int hotWaterMade, int hotTemp,
                          int su, int rpm, boolean publish, double steamShare, boolean coolingLimited,
                          int alarm, boolean explode) {}

    public static int clampMinTemp(int t) {
        return Math.max(MIN_TEMP_LOW, Math.min(MIN_TEMP_HIGH, t));
    }

    public static int clampPercent(int p) {
        return Math.max(0, Math.min(100, p));
    }

    public static double boost(double steamShare) {
        return 1D + STEAM_GAIN * steamShare;
    }

    /** The minimum water temperature that balances the turbine for this load and steam share. */
    public static double idealTemp(double load, double steamShare) {
        double eff = Math.min(1D, load * boost(steamShare));
        return IDEAL_LOW + (IDEAL_HIGH - IDEAL_LOW) * eff;
    }

    public static int rpmFor(double load) {
        if (load < 0.01D) return 0;
        int steps = (int) Math.ceil(MAX_RPM * load / RPM_STEP);
        return Math.min(MAX_RPM, Math.max(RPM_STEP, steps * RPM_STEP));
    }

    public static int quantizeSu(double load) {
        long raw = Math.round(load * MAX_SU);
        long q = (raw / SU_STEP) * SU_STEP;
        return (int) Math.min(MAX_SU, Math.max(0L, q));
    }

    /** Cold water wanted this tick at a given load: hotter setpoints need less water. */
    public static int waterWanted(double load, int inletTemp, int setpoint) {
        double delta = Math.max(5D, setpoint - inletTemp);
        return (int) Math.ceil(load * WATER_MB_AT_FULL * (REF_DELTA_T / delta));
    }

    private static double clamp01(double v) {
        return Math.max(0D, Math.min(1D, v));
    }

    public Outputs tick(Inputs in) {
        if (sinceAck < PUBLISH_COOLDOWN_TICKS) sinceAck++;
        throttlePercent = clampPercent(throttlePercent);
        steamLimitPercent = clampPercent(steamLimitPercent);
        minWaterTemp = clampMinTemp(minWaterTemp);

        if (!running && in.power >= START_POWER) running = true;
        if (running && in.power <= 0L) running = false;

        // power flow steadiness (how well the supply covers what the shaft is drawing)
        double draw = publishedSu * HE_PER_SU / boost(0D);
        double ratio = draw > 0D ? Math.min(1D, in.inflowLastTick / draw) : 1D;
        flowStability += (ratio - flowStability) * 0.02D;

        // steam admitted this tick, limited by the player's setting and by what the tank holds
        double admitted = running ? Math.min(in.steamMb, STEAM_MB_MAX * steamLimitPercent / 100D) : 0D;
        double steamShare = admitted / STEAM_MB_MAX;
        double boost = boost(steamShare);

        // stability: how well the water setting matches the load, minus what steam destabilizes
        double ideal = idealTemp(load, steamShare);
        double thermal = clamp01(1D - Math.max(0D, Math.abs(minWaterTemp - ideal) - TEMP_TOLERANCE) / TEMP_SPAN);
        if (lastLimited) thermal *= 0.7D;
        double flowScore = 0.5D + 0.5D * flowStability;
        double stableTarget = clamp01(thermal * flowScore - steamShare * steamShare * STEAM_DESTAB * (1D - 0.5D * thermal));
        stableTarget = Math.max(stableTarget, NO_STEAM_FLOOR * (1D - steamShare));
        stability += (stableTarget - stability) * STAB_RATE;

        boolean steamed = steamShare > 0D;
        if (steamed && stability < CRIT_BELOW) critTicks++;
        else if (stability >= CRIT_RECOVER_AT || !steamed) critTicks = Math.max(0, critTicks - 2);
        boolean explode = steamed && critTicks >= EXPLODE_TICKS;
        int alarm = stability < CRIT_BELOW ? 2 : stability < WARN_BELOW ? 1 : 0;

        // output target: stored power, throttle, supply flow, stability derate and the steam boost
        double fill = Math.min(1D, in.power / (double) FULL_OUTPUT_POWER);
        double stabFactor = Math.min(1D, 0.5D + stability);
        double driveFlow = STABLE_FLOOR + (1D - STABLE_FLOOR) * flowStability;
        double target = running ? clamp01(fill * (throttlePercent / 100D) * driveFlow * stabFactor * boost) : 0D;

        // the load the available water can sustain: water wanted scales linearly with load
        double fullLoadWater = waterWanted(1D, in.inletTemp, minWaterTemp);
        double sustainable = Math.min(in.coldWaterMb, in.hotRoomMb) / fullLoadWater;
        boolean limited = sustainable < target;
        if (limited) target = sustainable;
        lastLimited = limited;

        double ramp = RAMP_BASE + (RAMP_STEAM - RAMP_BASE) * steamShare;
        if (target > load) load = Math.min(target, load + ramp);
        else load = Math.max(target, load - RAMP_STEAM);

        int su = quantizeSu(load);
        int rpm = rpmFor(load);
        // steam does part of the work: the same stress costs 1/boost of the stored power
        long heUsed = Math.min(in.power, Math.round(su * HE_PER_SU / boost));
        int water = Math.min(Math.min(in.coldWaterMb, in.hotRoomMb), waterWanted(load, in.inletTemp, minWaterTemp));
        int steamUsed = load > 0D ? (int) Math.round(admitted) : 0;

        boolean changed = su != publishedSu || rpm != publishedRpm;
        boolean toZero = (su == 0 || rpm == 0) && (publishedSu != 0 || publishedRpm != 0);
        boolean publish = changed && (toZero || sinceAck >= PUBLISH_COOLDOWN_TICKS);
        if (publish) {
            publishedSu = su;
            publishedRpm = rpm;
            sinceAck = 0;
        }
        int hotTemp = water > 0 ? minWaterTemp : in.inletTemp;
        return new Outputs(heUsed, steamUsed, water, water, hotTemp, publishedSu, publishedRpm, publish,
                steamShare, limited, alarm, explode);
    }
}
