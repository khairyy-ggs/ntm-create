package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineVerticalHeatExchanger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Vertical Heat Exchanger: three high, three wide, three deep, core at the bottom centre (the same
 * footprint family as the assembly machine). Fluid connections sit on the two side cells of the bottom level
 * (turbine loop) and of the top level (heated side).
 */
public class MachineVerticalHeatExchanger extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, ITooltipProvider {

    public MachineVerticalHeatExchanger(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineVerticalHeatExchanger(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    /** Both side cells on the loop level and on the heated level carry a connection. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        for (int up : new int[]{0, 2}) {
            BlockPos level = core.above(up);
            visitor.cell(level.relative(rot), MASK_WEST);
            visitor.cell(level.relative(rot, -1), MASK_EAST);
        }
    }

    /** Receiving needs the passive plane too (as on the large turbine). */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        for (int up : new int[]{0, 2}) {
            BlockPos level = core.above(up);
            visitor.passiveCell(level.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }

    /** Hot water from the turbine and the water that cools it; nothing else reaches the tanks. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return type == Fluids.WATER || type == Fluids.HOTWATER;
    }
}
