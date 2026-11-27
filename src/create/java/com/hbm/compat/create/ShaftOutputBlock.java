package com.hbm.compat.create;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kinetic output for the Create Energy Shaft.
 * The exposed shaft runs along FACING's axis, like Create's ordinary shaft blocks.
 */
public class ShaftOutputBlock extends DirectionalKineticBlock implements IBE<ShaftOutputBlockEntity> {

    public ShaftOutputBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // A shaft has two ends. Checking only face == FACING disconnects a shaft on the opposite end.
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<ShaftOutputBlockEntity> getBlockEntityClass() {
        return ShaftOutputBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShaftOutputBlockEntity> getBlockEntityType() {
        return CreateBridge.SHAFT_OUTPUT_TYPE.get();
    }

    /**
     * IBE only supplies an automatic ticker for SmartBlockEntity instances.
     * GeneratingKineticBlockEntity is not a SmartBlockEntity, so this block must provide its own ticker.
     */
    @Override
    public <S extends BlockEntity> BlockEntityTicker<S> getTicker(
            Level level, BlockState state, BlockEntityType<S> type) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof ShaftOutputBlockEntity shaftOutput) {
                shaftOutput.tick();
            }
        };
    }
}
