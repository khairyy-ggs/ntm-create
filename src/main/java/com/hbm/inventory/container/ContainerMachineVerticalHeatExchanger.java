package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineVerticalHeatExchanger;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the Vertical Heat Exchanger: no machine slots, the screen only reads the four tanks. */
public class ContainerMachineVerticalHeatExchanger extends AbstractContainerMenu {

    private final @Nullable TileEntityMachineVerticalHeatExchanger exchanger;

    public ContainerMachineVerticalHeatExchanger(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineVerticalHeatExchanger(int containerId, Inventory playerInventory,
                                                 @Nullable TileEntityMachineVerticalHeatExchanger exchanger) {
        super(ModMenus.MACHINE_VERTICAL_HEAT_EXCHANGER.get(), containerId);
        this.exchanger = exchanger;
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineVerticalHeatExchanger resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineVerticalHeatExchanger t ? t : null;
    }

    public @Nullable TileEntityMachineVerticalHeatExchanger getExchanger() { return exchanger; }

    @Override
    public boolean stillValid(Player player) {
        return exchanger != null && !exchanger.isRemoved()
                && player.distanceToSqr(exchanger.getBlockPos().getX() + 0.5, exchanger.getBlockPos().getY() + 0.5,
                exchanger.getBlockPos().getZ() + 0.5) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
