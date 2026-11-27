package com.hbm.tileentity.machine;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Starter wind source. Open-sky exposure, altitude and weather affect its output. */
public final class TileEntityMachineWindTurbine extends TileEntityRenewableGenerator {
    public static final long BUFFER_CAPACITY = 400_000L;

    public TileEntityMachineWindTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_WIND_TURBINE.get(), pos, state,
                BUFFER_CAPACITY, RenewablePowerModel.WIND_RATED_HE_PER_TICK);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineWindTurbine tile) {
        if (!level.isClientSide()) tile.tickProvider(RenewablePowerModel.windOutput(level, pos));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_wind_turbine"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }
}
