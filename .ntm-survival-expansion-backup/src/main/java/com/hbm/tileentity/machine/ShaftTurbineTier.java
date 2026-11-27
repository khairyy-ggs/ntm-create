package com.hbm.tileentity.machine;

/** Balance profiles for the planned three-stage Create shaft turbine family. */
public enum ShaftTurbineTier {
    COMPACT(200_000, 5_000_000L, 1_000_000L, 25_000L),
    INDUSTRIAL(2_000_000, 35_000_000L, 10_000_000L, 500_000L),
    LARGE(10_000_000, 175_000_000L, 50_000_000L, 2_500_000L);

    public static final int MAX_RPM = 256;
    /** HE consumed per SU of sustained output, per game tick. */
    public static final double HE_PER_SU_PER_TICK = 0.005D;

    private final int stressCapacity;
    private final long bufferCapacity;
    private final long fullOutputPower;
    private final long startupPower;

    ShaftTurbineTier(int stressCapacity, long bufferCapacity, long fullOutputPower, long startupPower) {
        this.stressCapacity = stressCapacity;
        this.bufferCapacity = bufferCapacity;
        this.fullOutputPower = fullOutputPower;
        this.startupPower = startupPower;
    }

    public int stressCapacity() { return stressCapacity; }
    public long bufferCapacity() { return bufferCapacity; }
    public long fullOutputPower() { return fullOutputPower; }
    public long startupPower() { return startupPower; }
    public long fullLoadHePerTick() { return Math.round(stressCapacity * HE_PER_SU_PER_TICK); }
}
