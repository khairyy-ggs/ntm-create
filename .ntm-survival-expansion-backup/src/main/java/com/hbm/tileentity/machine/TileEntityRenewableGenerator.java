package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Shared HBM MK2 provider for renewable survival machines.
 * One output face keeps a generator attached to exactly one cable graph at a time, while the
 * existing PowerNetwork remains responsible for all distribution and receiver balancing.
 */
public abstract class TileEntityRenewableGenerator extends TileEntityMachineBase implements IEnergyProviderMK2 {
    public long power;
    private final long bufferCapacity;
    private final long outputPerTick;
    protected PortDef[] cachedPorts;

    protected TileEntityRenewableGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           long bufferCapacity, long outputPerTick) {
        super(type, pos, state, 0);
        this.bufferCapacity = Math.max(1L, bufferCapacity);
        this.outputPerTick = Math.max(1L, outputPerTick);
    }

    /** The front is visual; the cable/output port sits on the rear face of the machine. */
    public ForgeDirection getOutputSide() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return ForgeDirection.getOrientation(facing.getOpposite());
    }

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = new PortDef[]{PortDef.make(worldPosition, getOutputSide())};
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }
    public DirPos[] getPowerPos() { return getConPos(); }

    @Override
    public boolean canConnect(ForgeDirection side) {
        return side != null && side == getOutputSide();
    }

    protected final void tickProvider(long generatedThisTick) {
        if (level == null || level.isClientSide) return;
        setupAllPorts(getPorts());
        updatePortPOFIFO();
        long before = power;
        power = RenewablePowerMath.addToBuffer(power, generatedThisTick, bufferCapacity);
        if (power != before) setChanged();
        if (level.getGameTime() % 20L == 0L) networkPackNT();
    }

    @Override public long getPower() { return power; }

    @Override
    public void setPower(long value) {
        long next = RenewablePowerMath.clampPower(value, bufferCapacity);
        if (power != next) {
            power = next;
            setChanged();
        }
    }

    @Override public long getMaxPower() { return bufferCapacity; }
    @Override public long getProviderSpeed() { return outputPerTick; }

    public long getBufferCapacity() { return bufferCapacity; }
    public long getOutputPerTick() { return outputPerTick; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = RenewablePowerMath.clampPower(input.getLongOr("power", 0L), bufferCapacity);
    }
}
