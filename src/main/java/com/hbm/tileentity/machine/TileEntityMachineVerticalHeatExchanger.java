package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidDirectionalMK2;
import com.hbm.api.fluidmk2.IFluidMultiUserMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.inventory.container.ContainerMachineVerticalHeatExchanger;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Vertical Heat Exchanger, a three-high tower with two circuits. The lower level is the turbine loop: hot water
 * in, cold water out. The upper level is the heated side: pumped water in, pre-heated water out. Each level is
 * its own fluid user (see {@link IFluidMultiUserMK2}) so a pipe or a pump on the lower side can only reach the
 * loop tanks and one on the upper side only the heated tanks. On each level the side that faces {@code rot} takes
 * fluid in and the opposite side gives it out. Create pumps and pipes reach the tanks through the mod's
 * machine fluid exposure, which covers every HBM fluid machine.
 */
public class TileEntityMachineVerticalHeatExchanger extends TileEntityMachineBase
        implements IFluidStandardTransceiverMK2, IFluidMultiUserMK2, MenuProvider {

    public static final String DISPLAY_KEY = "container.machineVerticalHeatExchanger";

    public final FluidTankNTM hotIn = new FluidTankNTM(Fluids.HOTWATER, 64_000);
    public final FluidTankNTM coldOut = new FluidTankNTM(Fluids.WATER, 64_000);
    public final FluidTankNTM sourceIn = new FluidTankNTM(Fluids.WATER, 64_000);
    public final FluidTankNTM preheatOut = new FluidTankNTM(Fluids.WATER, 64_000);
    /** mB exchanged last tick, for the screen. */
    public int exchanged;

    private final Circuit loop = new Circuit(hotIn, coldOut);
    private final Circuit heated = new Circuit(sourceIn, preheatOut);

    public TileEntityMachineVerticalHeatExchanger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_VERTICAL_HEAT_EXCHANGER.get(), pos, state, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineVerticalHeatExchanger t) {
        t.setupAllPorts(t.getPorts());
        t.updatePortPOFIFO();

        int ops = HeatExchangerModel.ops(t.hotIn.getFill(), t.sourceIn.getFill(),
                t.coldOut.getMaxFill() - t.coldOut.getFill(), t.preheatOut.getMaxFill() - t.preheatOut.getFill());
        t.hotIn.setFill(t.hotIn.getFill() - ops);
        t.coldOut.setFill(t.coldOut.getFill() + ops);
        t.sourceIn.setFill(t.sourceIn.getFill() - ops);
        t.preheatOut.setFill(t.preheatOut.getFill() + ops);
        t.exchanged = ops;
        t.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineVerticalHeatExchanger t) {
    }

    public ForgeDirection getFacing() { return MachineCore.facing(getBlockState()); }

    private ForgeDirection inletSide() { return getFacing().getRotation(ForgeDirection.UP); }

    protected PortDef[] cachedPorts;

    /** Per level: the inlet on the rot side, the outlet on the opposite side. Levels at core height and two above. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection rot = inletSide();
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, rot),
                    PortDef.make(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                    PortDef.make(x + rot.offsetX, y + 2, z + rot.offsetZ, rot),
                    PortDef.make(x - rot.offsetX, y + 2, z - rot.offsetZ, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    /** Lower level reaches the loop circuit, upper level the heated circuit, the middle level nothing. */
    @Override
    public @Nullable IFluidUserMK2 fluidUserAt(BlockPos pos, @Nullable Direction face) {
        int dy = pos.getY() - worldPosition.getY();
        return dy == 0 ? loop : dy == 2 ? heated : null;
    }

    private final class Circuit implements IFluidStandardTransceiverMK2, IFluidDirectionalMK2 {
        private final FluidTankNTM in, out;

        Circuit(FluidTankNTM in, FluidTankNTM out) {
            this.in = in;
            this.out = out;
        }

        @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{in, out}; }
        @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{in}; }
        @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{out}; }
        @Override public boolean isLoaded() { return TileEntityMachineVerticalHeatExchanger.this.isLoaded(); }
        @Override public boolean receivesOn(ForgeDirection side) { return side == inletSide(); }
        @Override public boolean providesOn(ForgeDirection side) { return side == inletSide().getOpposite(); }
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{hotIn, coldOut, sourceIn, preheatOut}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{hotIn, sourceIn}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{coldOut, preheatOut}; }

    @Override public Component getDisplayName() { return Component.translatable(DISPLAY_KEY); }
    @Override public String getDefaultName() { return DISPLAY_KEY; }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineVerticalHeatExchanger(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(exchanged);
        for (FluidTankNTM tank : getAllTanks()) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        exchanged = buf.readInt();
        for (FluidTankNTM tank : getAllTanks()) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        hotIn.writeToNBT(output, "hotIn");
        coldOut.writeToNBT(output, "coldOut");
        sourceIn.writeToNBT(output, "sourceIn");
        preheatOut.writeToNBT(output, "preheatOut");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        hotIn.readFromNBT(input, "hotIn");
        coldOut.readFromNBT(input, "coldOut");
        sourceIn.readFromNBT(input, "sourceIn");
        preheatOut.readFromNBT(input, "preheatOut");
    }
}
