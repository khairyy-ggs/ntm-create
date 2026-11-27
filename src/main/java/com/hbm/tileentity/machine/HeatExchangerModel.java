package com.hbm.tileentity.machine;

/**
 * Counter-flow exchange of the Vertical Heat Exchanger. Hot water from the turbine gives its heat to
 * pumped water: every mB of hot water that comes in leaves as a mB of cold water, and every mB of
 * pumped water that comes in leaves as a mB of pre-heated water. Without pumped water nothing moves,
 * so a turbine whose hot water has no heat sink stalls once its hot tank fills (on purpose).
 */
public final class HeatExchangerModel {

    /** mB of each side that can be exchanged per tick. */
    public static final int RATE = 2000;

    private HeatExchangerModel() {}

    public static int ops(int hotFill, int sourceFill, int coldRoom, int preheatRoom) {
        int n = Math.min(Math.min(hotFill, sourceFill), Math.min(coldRoom, preheatRoom));
        return Math.max(0, Math.min(RATE, n));
    }
}
