package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineVerticalHeatExchanger;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Vertical Heat Exchanger: one static 3x3x3 model, centred on the core cell, turned with the machine's facing. */
public final class VerticalHeatExchangerRenderer
        extends GemBER<TileEntityMachineVerticalHeatExchanger, VerticalHeatExchangerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/vertical_heat_exchanger.obj"));
    private final Surface texture = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/vertical_heat_exchanger.png");

    @Override
    protected void setup(Gem.Model<VerticalHeatExchangerRenderState> declaration) {
        Gem.Part<VerticalHeatExchangerRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        machine.child(model.parts(Set.of("Body"), true), texture);
    }

    @Override
    public VerticalHeatExchangerRenderState createRenderState() {
        return new VerticalHeatExchangerRenderState();
    }

    @Override
    public void extract(TileEntityMachineVerticalHeatExchanger be, VerticalHeatExchangerRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = 180F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
