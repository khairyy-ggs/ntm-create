package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineShaftTurbine;
import com.hbm.tileentity.machine.ShaftTurbineModel;
import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the Create Energy Shaft (176x222). Tanks 16x52 at x=8 steam (optional), x=30 cold water and
 * x=152 hot water; 12x52 power bar at x=56; readout window (74,17) with the stability bar at (77,58);
 * three [-] [value] [+] rows at y=76 throttle, y=94 minimum water temperature and y=112 steam limit; status
 * window (132,76). Buttons: 0/1 throttle, 2/3 min water temperature, 4/5 steam limit.
 */
public class GUIMachineShaftTurbine extends CompatContainerScreen<ContainerMachineShaftTurbine> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/generators/gui_shaft_turbine.png");
    private static final int WHITE = 0xFFFFFF;
    private static final int[] ALARM_COLORS = {0x55FF55, 0xFFDD44, 0xFF5544};
    private static final String[] ALARM_TEXT = {"STABLE", "WARNING", "CRITICAL"};

    public GUIMachineShaftTurbine(ContainerMachineShaftTurbine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, 129, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.literal("Throttle"), 8, 79, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.literal("Min water"), 8, 97, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.literal("Steam"), 8, 115, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        TileEntityMachineShaftTurbine te = this.menu.getTurbine();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        ShaftTurbineModel m = te.model;

        GuiUtil.tank(graphics, te.steam, gl + 8, gt + 69, 16, 52);
        GuiUtil.tank(graphics, te.water, gl + 30, gt + 69, 16, 52);
        GuiUtil.tank(graphics, te.hot, gl + 152, gt + 69, 16, 52);

        int i = (int) te.getPowerScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, gl + 56, gt + 69 - i, 176.0F, (float) (52 - i), 12, i, 256, 256);

        int alarm = Math.max(0, Math.min(2, te.alarm));
        int bar = (int) Math.round(Math.max(0D, Math.min(1D, m.stability)) * 64D);
        if (bar > 0) GuiDraw.blit(graphics, TEXTURE, gl + 78, gt + 59, 176.0F, (float) (60 + alarm * 6), bar, 5, 256, 256);

        if (te.coolingLimited) GuiDraw.blit(graphics, TEXTURE, gl + 136, gt + 110, 176.0F, 80.0F, 14, 14, 256, 256);
        if (alarm == 2) GuiDraw.blit(graphics, TEXTURE, gl + 152, gt + 110, 198.0F, 80.0F, 14, 14, 256, 256);

        GuiDraw.text(graphics, this.font, Component.literal("Power in " + fmt(te.inflow) + " HE/t"), gl + 78, gt + 21, WHITE, false);
        GuiDraw.text(graphics, this.font, Component.literal("Stress " + fmt(m.publishedSu) + " SU"), gl + 78, gt + 33, WHITE, false);
        GuiDraw.text(graphics, this.font, Component.literal("Speed " + m.publishedRpm + " RPM"), gl + 78, gt + 45, WHITE, false);

        GuiDraw.text(graphics, this.font, Component.literal(m.throttlePercent + "%"), gl + 82, gt + 79, WHITE, false);
        GuiDraw.text(graphics, this.font, Component.literal(m.minWaterTemp + " C"), gl + 82, gt + 97, WHITE, false);
        GuiDraw.text(graphics, this.font, Component.literal(m.steamLimitPercent + "%"), gl + 82, gt + 115, WHITE, false);

        GuiDraw.text(graphics, this.font, Component.literal(ALARM_TEXT[alarm]), gl + 135, gt + 80, ALARM_COLORS[alarm], false);
        GuiDraw.text(graphics, this.font, Component.literal(te.getStabilityPercent() + "%"), gl + 135, gt + 92, ALARM_COLORS[alarm], false);
        if (alarm == 2) {
            int left = Math.max(0, (ShaftTurbineModel.EXPLODE_TICKS - m.critTicks) / 20);
            GuiDraw.text(graphics, this.font, Component.literal(left + " s"), gl + 135, gt + 104, ALARM_COLORS[2], false);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineShaftTurbine te = this.menu.getTurbine();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 8, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.steam), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 30, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.water), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.hot), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 56, gt + 17, 12, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, ShaftTurbineModel.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 62, gt + 94, 64, 14)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Minimum water temperature",
                    "Adjust until the turbine reads STABLE", "The right value moves with the load"), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 62, gt + 112, 64, 14)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Steam limit: how much steam may be fed in",
                    "More steam: more stress for the same power", "But less stable. Unstable plus steam explodes"), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 62, gt + 76, 64, 14)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Throttle: share of the output offered to Create"), mouseX, mouseY);
        } else if (te.coolingLimited && GuiUtil.hover(mouseX, mouseY, gl + 136, gt + 110, 14, 14)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Not enough cold water: output is limited"), mouseX, mouseY);
        } else if (te.alarm == 2 && GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 110, 14, 14)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Critical! Cut the steam or retune the water"), mouseX, mouseY);
        }
    }

    private static String fmt(long v) {
        if (v >= 1_000_000L) return String.format("%.2fM", v / 1_000_000D);
        if (v >= 1_000L) return String.format("%.1fk", v / 1_000D);
        return Long.toString(v);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (this.menu.getTurbine() == null || mc.gameMode == null) return false;
        double x = event.x() - leftPos, y = event.y() - topPos;
        int id = -1;
        for (int row = 0; row < 3 && id < 0; row++) {
            int by = 76 + row * 18;
            if (in(x, y, 62, by, 14, 14)) id = row * 2;
            else if (in(x, y, 112, by, 14, 14)) id = row * 2 + 1;
        }
        if (id < 0) return false;
        GuiUtil.playClickSound();
        mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private static boolean in(double x, double y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
