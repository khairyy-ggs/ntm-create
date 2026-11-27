package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IShaftNetOnly;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Create Energy Shaft: same footprint as the legacy large turbine (four long, three wide).
 * Power and water/steam enter on the sides, hot water leaves at the back, and the front face is left
 * free for the shaft output block that hands stress to Create.
 */
public class MachineShaftTurbine extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, IShaftNetOnly, ITooltipProvider {

    public MachineShaftTurbine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineShaftTurbine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 3, 1, 1, 1};
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

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineShaftTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getPowerPos(), pos, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot), MASK_WEST);
        visitor.cell(core.relative(rot, -1), MASK_EAST);
        visitor.cell(core.relative(facing, -3), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3), MASK_ALL, domains);
    }

    /** Cold water and steam in, hot water out. Nothing else reaches the tanks. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return type == Fluids.WATER || type == Fluids.STEAM || type == Fluids.HOTWATER;
    }
}
