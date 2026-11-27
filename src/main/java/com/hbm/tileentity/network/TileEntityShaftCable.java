package com.hbm.tileentity.network;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The shaft cable's own block entity type, so the red cable's type keeps accepting only the red cable block. */
public class TileEntityShaftCable extends TileEntityCableBaseNT {

    public TileEntityShaftCable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_CABLE.get(), pos, state);
    }
}
