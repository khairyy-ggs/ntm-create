package com.hbm.tileentity.machine;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Starter photovoltaic source. It generates only under open sky during the daytime. */
public final class TileEntityMachineSolarPanel extends TileEntityRenewableGenerator {
    public static final long BUFFER_CAPACITY = 250_000L;

    public TileEntityMachineSolarPanel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SOLAR_PANEL.get(), pos, state,
                BUFFER_CAPACITY, RenewablePowerModel.SOLAR_RATED_HE_PER_TICK);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSolarPanel tile) {
        if (!level.isClientSide()) tile.tickProvider(RenewablePowerModel.solarOutput(level, pos));
    }

    @Override public String getDefaultName() { return "block.hbm.machine_solar_panel"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }
}
