package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Create Energy Shaft: a static housing and the brass flywheel at the front, which turns with the published RPM.
 * The OBJ is centred on its own midpoint like NTM's turbine.obj (z -2.5..2.5) and shifted back by one block here,
 * so the housing fills the 3 wide, 4 long footprint exactly; its +z end is the machine's front.
 */
public final class ShaftTurbineRenderer
        extends GemBER<TileEntityMachineShaftTurbine, ShaftTurbineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/shaft_turbine.obj"));
    private final Surface texture = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/shaft_turbine.png");

    @Override
    protected void setup(Gem.Model<ShaftTurbineRenderState> declaration) {
        Gem.Part<ShaftTurbineRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(0F, 0F, -1F);

        machine.child(model.parts(Set.of("Body"), true), texture);
        // the flywheel axis is 0.5 up, level with the Shaft Output block on the front face
        machine.child(model.parts(Set.of("Rotor"), true), texture)
                .spin(Gem.Axes.Z, 0F, 0.5F, 0F, state -> state.rotor);
    }

    @Override
    public ShaftTurbineRenderState createRenderState() {
        return new ShaftTurbineRenderState();
    }

    @Override
    public void extract(TileEntityMachineShaftTurbine be, ShaftTurbineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = 180F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.rotor = be.getRotor(partialTicks);
    }
}
