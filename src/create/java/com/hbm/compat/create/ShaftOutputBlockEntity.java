package com.hbm.compat.create;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's kinetic source for the HBM turbine. It mirrors the turbine's published RPM and SU;
 * it does not calculate or consume energy itself.
 */
public class ShaftOutputBlockEntity extends GeneratingKineticBlockEntity {

    private int appliedSerial = Integer.MIN_VALUE;
    private int rpm;
    private float capacityPerRpm;

    public ShaftOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        TileEntityMachineShaftTurbine turbine = findTurbine();
        int serial = turbine == null ? -1 : turbine.getShaftSerial();
        if (serial == appliedSerial) return;

        appliedSerial = serial;
        int su = turbine == null ? 0 : turbine.getPublishedSu();
        rpm = turbine == null ? 0 : turbine.getPublishedRpm();

        // Create multiplies base capacity by absolute generated speed to obtain total SU.
        capacityPerRpm = rpm == 0 ? 0F : su / (float) rpm;
        updateGeneratedRotation();
    }

    private TileEntityMachineShaftTurbine findTurbine() {
        if (level == null) return null;
        Direction outputFacing = getBlockState().getValue(DirectionalKineticBlock.FACING);
        BlockPos turbineSide = worldPosition.relative(outputFacing.getOpposite());
        return MultiblockSurface.coreEntity(level, turbineSide) instanceof TileEntityMachineShaftTurbine turbine
                ? turbine : null;
    }

    @Override
    public float getGeneratedSpeed() {
        // Use Create's own direction conversion, matching its Creative Motor implementation.
        return convertToDirection(rpm, getBlockState().getValue(DirectionalKineticBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        this.lastCapacityProvided = capacityPerRpm;
        return capacityPerRpm;
    }
}
