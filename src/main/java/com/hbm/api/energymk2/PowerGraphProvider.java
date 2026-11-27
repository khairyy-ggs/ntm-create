package com.hbm.api.energymk2;

import com.hbm.blocks.network.BlockShaftCable;
import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/** Ordinary cables interconnect; shaft cables interconnect only with each other. */
public final class PowerGraphProvider implements IGraphProvider<CableData> {

    public static final PowerGraphProvider INSTANCE = new PowerGraphProvider();

    private PowerGraphProvider() {}

    @Override
    public CableData createData(BlockState state) {
        return state.getBlock() instanceof BlockShaftCable ? CableData.SHAFT : CableData.INSTANCE;
    }

    @Override
    public boolean dataCompatible(CableData a, CableData b) {
        return a.shaft() == b.shaft();
    }

    @Override
    public Codec<CableData> dataCodec() {
        return CableData.CODEC;
    }

    @Override
    public void onNetsMerged(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        PowerNetwork.mergeBridges(keep, drain);
    }
}
