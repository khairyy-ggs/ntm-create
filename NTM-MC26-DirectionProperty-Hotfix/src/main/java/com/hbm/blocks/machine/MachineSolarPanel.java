package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineSolarPanel;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;

/** HBM-style starter PV panel; its rear face is the single power connection. */
public final class MachineSolarPanel extends CompatBlock implements TickingBlock, IEnergyConnectorBlock {

    public MachineSolarPanel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSolarPanel(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.or(
                net.minecraft.world.level.block.Block.box(2, 0, 2, 14, 3, 14),
                net.minecraft.world.level.block.Block.box(3, 3, 3, 4, 9, 4),
                net.minecraft.world.level.block.Block.box(12, 3, 3, 13, 9, 4),
                net.minecraft.world.level.block.Block.box(3, 3, 12, 4, 9, 13),
                net.minecraft.world.level.block.Block.box(12, 3, 12, 13, 9, 13),
                net.minecraft.world.level.block.Block.box(12, 3, 12, 13, 9, 13),
                net.minecraft.world.level.block.Block.box(1, 9, 1, 15, 11, 15));
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return world.getBlockEntity(pos) instanceof TileEntityMachineSolarPanel tile && tile.canConnect(dir);
    }
}
