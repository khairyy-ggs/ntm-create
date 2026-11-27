package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the Create Energy Shaft: no machine slots, only the player's inventory and six buttons. */
public class ContainerMachineShaftTurbine extends AbstractContainerMenu {

    private final @Nullable TileEntityMachineShaftTurbine turbine;

    public ContainerMachineShaftTurbine(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineShaftTurbine(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityMachineShaftTurbine turbine) {
        super(ModMenus.MACHINE_SHAFT_TURBINE.get(), containerId);
        this.turbine = turbine;
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);
    }

    private static @Nullable TileEntityMachineShaftTurbine resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineShaftTurbine t ? t : null;
    }

    public @Nullable TileEntityMachineShaftTurbine getTurbine() { return turbine; }

    /** 0/1 throttle -/+, 2/3 minimum water temperature -/+, 4/5 steam limit -/+. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (turbine == null || id < 0 || id > 5) return false;
        turbine.handleButton(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return turbine != null && !turbine.isRemoved()
                && player.distanceToSqr(turbine.getBlockPos().getX() + 0.5, turbine.getBlockPos().getY() + 0.5,
                turbine.getBlockPos().getZ() + 0.5) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
