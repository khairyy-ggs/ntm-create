package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.IShaftNetOnly;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineShaftTurbine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create Energy Shaft: takes power from shaft cables, cold water (and optional steam), and offers
 * stress to a Create network through the shaft output block on its front. All control logic lives in
 * {@link ShaftTurbineModel}; this class is the world side: tanks, power buffer, ports, menu, saving.
 */
public class TileEntityMachineShaftTurbine extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IShaftNetOnly, IFluidStandardTransceiverMK2, MenuProvider, IControlReceiver {

    /** Fed back to the player in the screen: how strained the turbine is, 0 to 100. */
    public int getStabilityPercent() { return (int) Math.round(model.stability * 100D); }

    public static final String DISPLAY_KEY = "container.machineShaftTurbine";

    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 64_000);
    public final FluidTankNTM hot = new FluidTankNTM(Fluids.HOTWATER, 64_000);
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.STEAM, 128_000);
    public final ShaftTurbineModel model = new ShaftTurbineModel();

    public long power;
    private long powerAfterTick;
    public long inflow;
    public double steamShare;
    public boolean coolingLimited;
    /** 0 stable, 1 warning, 2 critical (the countdown to the blast is running). */
    public int alarm;
    /** Bumped when the model publishes new stress/RPM; the Create output block follows it. */
    private int shaftSerial;

    public TileEntityMachineShaftTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SHAFT_TURBINE.get(), pos, state, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineShaftTurbine t) {
        t.setupAllPorts(t.getPorts());
        t.updatePortPIFIFO();

        t.inflow = Math.max(0L, t.power - t.powerAfterTick);
        var in = new ShaftTurbineModel.Inputs(t.power, t.inflow, t.steam.getFill(), t.water.getFill(),
                t.water.getTankType().getTemperature(), t.hot.getMaxFill() - t.hot.getFill());
        var out = t.model.tick(in);

        t.power -= out.heUsed();
        t.steam.setFill(t.steam.getFill() - out.steamUsed());
        t.water.setFill(t.water.getFill() - out.waterUsed());
        t.hot.setFill(t.hot.getFill() + out.hotWaterMade());
        t.steamShare = out.steamShare();
        t.coolingLimited = out.coolingLimited();
        t.alarm = out.alarm();
        t.powerAfterTick = t.power;
        if (out.publish()) t.shaftSerial++;
        if (out.explode()) {
            t.blowUp(level, pos);
            return;
        }
        t.networkPackNT();
    }

    /** Steam fed into a turbine that never stabilized: the machine goes, and so does what is around it. */
    private void blowUp(Level level, BlockPos pos) {
        float radius = (float) (4D + 6D * model.load);
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, radius, Level.ExplosionInteraction.BLOCK);
    }

    /** Flywheel angle for the renderer: eased towards a speed that follows the published RPM, capped so it never strobes. */
    public float rotor;
    public float lastRotor;
    private float spinSpeed;

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineShaftTurbine t) {
        float target = Math.min(24F, t.model.publishedRpm * 0.09F);
        t.spinSpeed += (target - t.spinSpeed) * 0.1F;
        t.lastRotor = t.rotor;
        t.rotor += t.spinSpeed;
        if (t.rotor >= 360F) {
            t.rotor -= 360F;
            t.lastRotor -= 360F;
        }
    }

    public float getRotor(float partialTicks) {
        return lastRotor + (rotor - lastRotor) * partialTicks;
    }

    // ---- what the Create output block reads ----
    public int getPublishedSu() { return model.publishedSu; }
    public int getPublishedRpm() { return model.publishedRpm; }
    public int getShaftSerial() { return shaftSerial; }

    // ---- menu controls: 0/1 throttle -/+10, 2/3 minimum water temperature -/+5, 4/5 steam limit -/+10 ----
    public void handleButton(int id) {
        switch (id) {
            case 0 -> model.throttlePercent = ShaftTurbineModel.clampPercent(model.throttlePercent - 10);
            case 1 -> model.throttlePercent = ShaftTurbineModel.clampPercent(model.throttlePercent + 10);
            case 2 -> model.minWaterTemp = ShaftTurbineModel.clampMinTemp(model.minWaterTemp - 5);
            case 3 -> model.minWaterTemp = ShaftTurbineModel.clampMinTemp(model.minWaterTemp + 5);
            case 4 -> model.steamLimitPercent = ShaftTurbineModel.clampPercent(model.steamLimitPercent - 10);
            case 5 -> model.steamLimitPercent = ShaftTurbineModel.clampPercent(model.steamLimitPercent + 10);
            default -> { return; }
        }
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerMachineShaftTurbine menu
                && menu.getTurbine() == this && menu.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // reserved for a throttle slider; the buttons go through handleButton
    }

    // ---- ports: water/steam in and power in on the two sides, hot water out at the back ----
    protected PortDef[] cachedPorts;

    public ForgeDirection getFacing() { return MachineCore.facing(getBlockState()); }

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, rot),
                    PortDef.make(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 3, y, z - dir.offsetZ * 3, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }
    public DirPos[] getPowerPos() { return getConPos(); }

    // ---- energy: receiver only, and only from shaft cables ----
    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = Math.max(0L, Math.min(ShaftTurbineModel.MAX_POWER, value)); }
    @Override public long getMaxPower() { return ShaftTurbineModel.MAX_POWER; }
    @Override public long getReceiverSpeed() { return ShaftTurbineModel.MAX_POWER / 20L; }

    // ---- fluids ----
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{hot}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{water, steam}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water, hot, steam}; }

    // ---- menu ----
    @Override public Component getDisplayName() { return Component.translatable(DISPLAY_KEY); }
    @Override public String getDefaultName() { return DISPLAY_KEY; }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineShaftTurbine(id, inventory, this);
    }

    // ---- sync (client menu reads these) ----
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(inflow);
        buf.writeDouble(model.load);
        buf.writeInt(model.publishedSu);
        buf.writeInt(model.publishedRpm);
        buf.writeInt(model.throttlePercent);
        buf.writeInt(model.minWaterTemp);
        buf.writeInt(model.steamLimitPercent);
        buf.writeDouble(model.stability);
        buf.writeDouble(steamShare);
        buf.writeInt(alarm);
        buf.writeInt(model.critTicks);
        buf.writeBoolean(coolingLimited);
        water.serialize(buf);
        hot.serialize(buf);
        steam.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        inflow = buf.readLong();
        model.load = buf.readDouble();
        model.publishedSu = buf.readInt();
        model.publishedRpm = buf.readInt();
        model.throttlePercent = buf.readInt();
        model.minWaterTemp = buf.readInt();
        model.steamLimitPercent = buf.readInt();
        model.stability = buf.readDouble();
        steamShare = buf.readDouble();
        alarm = buf.readInt();
        model.critTicks = buf.readInt();
        coolingLimited = buf.readBoolean();
        water.deserialize(buf);
        hot.deserialize(buf);
        steam.deserialize(buf);
    }

    // ---- saving: longs only, so no assumption about the DataOut int/boolean helpers ----
    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        water.writeToNBT(output, "water");
        hot.writeToNBT(output, "hot");
        steam.writeToNBT(output, "steam");
        output.putLong("power", power);
        output.putLong("load", Math.round(model.load * 1_000_000D));
        output.putLong("throttle", model.throttlePercent);
        output.putLong("minTemp", model.minWaterTemp);
        output.putLong("steamLimit", model.steamLimitPercent);
        output.putLong("stability", Math.round(model.stability * 1_000_000D));
        output.putLong("crit", model.critTicks);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        water.readFromNBT(input, "water");
        hot.readFromNBT(input, "hot");
        steam.readFromNBT(input, "steam");
        power = input.getLongOr("power", 0L);
        model.load = input.getLongOr("load", 0L) / 1_000_000D;
        model.throttlePercent = (int) input.getLongOr("throttle", 100L);
        model.minWaterTemp = ShaftTurbineModel.clampMinTemp((int) input.getLongOr("minTemp", 70L));
        model.steamLimitPercent = ShaftTurbineModel.clampPercent((int) input.getLongOr("steamLimit", 0L));
        model.stability = input.getLongOr("stability", 1_000_000L) / 1_000_000D;
        model.critTicks = (int) input.getLongOr("crit", 0L);
        powerAfterTick = power;
    }

    public long getPowerScaled(int size) { return power * size / ShaftTurbineModel.MAX_POWER; }
}
