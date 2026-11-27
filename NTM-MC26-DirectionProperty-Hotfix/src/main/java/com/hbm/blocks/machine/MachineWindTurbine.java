package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineWindTurbine;
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

/** Single-block wind turbine. The rotor faces outward; the power port is on the rear. */
public final class MachineWindTurbine extends CompatBlock implements TickingBlock, IEnergyConnectorBlock {

    public MachineWindTurbine(Properties properties) {
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
        return new TileEntityMachineWindTurbine(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        VoxelShape base = Shapes.or(
                net.minecraft.world.level.block.Block.box(4, 0, 4, 12, 2, 12),
                net.minecraft.world.level.block.Block.box(7, 2, 7, 9, 13, 9),
                net.minecraft.world.level.block.Block.box(5, 11, 5, 11, 14, 11));
        VoxelShape rotor = switch (facing) {
            case NORTH -> net.minecraft.world.level.block.Block.box(0, 2, 0, 16, 14, 2);
            case SOUTH -> net.minecraft.world.level.block.Block.box(0, 2, 14, 16, 14, 16);
            case EAST -> net.minecraft.world.level.block.Block.box(14, 2, 0, 16, 14, 16);
            case WEST -> net.minecraft.world.level.block.Block.box(0, 2, 0, 2, 14, 16);
            default -> net.minecraft.world.level.block.Block.box(0, 2, 0, 16, 14, 2);
        };
        return Shapes.or(base, rotor);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return world.getBlockEntity(pos) instanceof TileEntityMachineWindTurbine tile && tile.canConnect(dir);
    }
}
