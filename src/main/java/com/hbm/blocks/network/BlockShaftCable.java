package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IShaftNetOnly;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.network.TileEntityShaftCable;
import org.jspecify.annotations.Nullable;

/**
 * The dedicated cable of the Create Energy Shaft. Same shape and behaviour as the red cable; the
 * difference is what it joins: other shaft cables, the shaft turbine, and anything that can put power
 * out (generators, batteries). It never joins an ordinary cable, and the net it forms feeds the shaft
 * turbine only (see the shaft filter in PowerNetwork).
 */
public class BlockShaftCable extends BlockCable {

    public BlockShaftCable(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityShaftCable(pos, state);
    }

    @Override
    protected boolean connectsTo(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        ForgeDirection side = ForgeDirection.getOrientation(dir.getOpposite());
        var block = level.getBlockState(neighbour).getBlock();
        if (block instanceof BlockShaftCable || block instanceof IShaftNetOnly) {
            return !(block instanceof IEnergyConnectorBlock c) || c.canConnect(level, neighbour, side);
        }
        if (block instanceof IEnergyConnectorBlock c && c.canConnect(level, neighbour, side)) return true;
        BlockEntity be = level.getBlockEntity(neighbour);
        return be instanceof IEnergyProviderMK2 p && p.canConnect(side);
    }
}
