package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineVerticalHeatExchanger;
import com.hbm.tileentity.machine.TileEntityMachineVerticalHeatExchanger;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the Vertical Heat Exchanger (176x166). Four 16x52 tanks, bottom y=69, at x=26 hot water in,
 * x=62 cold water out (the turbine loop) and x=98 water in, x=134 pre-heated water out (the heated side).
 */
public class GUIMachineVerticalHeatExchanger extends CompatContainerScreen<ContainerMachineVerticalHeatExchanger> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_vertical_heat_exchanger.png");
    private static final int[] TANK_X = {26, 62, 98, 134};
    private static final String[] LABEL = {"Hot in", "Cold out", "Water in", "Warm out"};

    public GUIMachineVerticalHeatExchanger(ContainerMachineVerticalHeatExchanger menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        for (int i = 0; i < 4; i++) {
            int w = this.font.width(LABEL[i]);
            GuiDraw.text(graphics, this.font, Component.literal(LABEL[i]), TANK_X[i] + 8 - w / 2, 72, 4210752, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        TileEntityMachineVerticalHeatExchanger te = this.menu.getExchanger();
        if (te == null) return;
        var tanks = te.getAllTanks();
        GuiUtil.tank(graphics, tanks[0], this.leftPos + TANK_X[0], this.topPos + 69, 16, 52);
        GuiUtil.tank(graphics, tanks[1], this.leftPos + TANK_X[1], this.topPos + 69, 16, 52);
        GuiUtil.tank(graphics, tanks[2], this.leftPos + TANK_X[2], this.topPos + 69, 16, 52);
        GuiUtil.tank(graphics, tanks[3], this.leftPos + TANK_X[3], this.topPos + 69, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineVerticalHeatExchanger te = this.menu.getExchanger();
        if (te == null) return;
        var tanks = te.getAllTanks();
        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, this.leftPos + TANK_X[i], this.topPos + 17, 16, 52)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(tanks[i]), mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 76, 160, 8)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Exchanging " + te.exchanged + " mB/t",
                    "No pumped water: no cooling"), mouseX, mouseY);
        }
    }
}
