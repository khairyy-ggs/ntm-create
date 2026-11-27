package com.hbm.api.energymk2;

import com.mojang.serialization.Codec;

/**
 * The conductor datum for the power graph. Ordinary cables carry nothing. A shaft cable (the dedicated
 * cable of the Create Energy Shaft) carries one flag, and the provider treats two cells as compatible
 * only if the flags match, so shaft cells never merge into the ordinary grid.
 */
public record CableData(boolean shaft) {

    public static final CableData INSTANCE = new CableData(false);
    public static final CableData SHAFT = new CableData(true);

    /**
     * Not 1: pre-v3 saves wrote a tier int here and every one of them must keep decoding to the
     * ordinary cable. 0x53 never was a tier.
     */
    private static final byte SHAFT_BYTE = 0x53;

    public static final Codec<CableData> CODEC =
            Codec.BYTE.xmap(b -> b == SHAFT_BYTE ? SHAFT : INSTANCE, d -> d.shaft() ? SHAFT_BYTE : (byte) 0);
}
