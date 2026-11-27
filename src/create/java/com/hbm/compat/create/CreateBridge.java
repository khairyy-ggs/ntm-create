package com.hbm.compat.create;

import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

/**
 * Entry point of the Create soft integration. Only this package imports Create, and it is touched only when
 * Create is loaded (the registration call in PATCHES.md is guarded by the mod-list check), so a world without
 * Create never loads a Create class.
 */
public final class CreateBridge {

    /** Set by the registration step; see PATCHES.md, ModBlockEntities. */
    public static Supplier<BlockEntityType<ShaftOutputBlockEntity>> SHAFT_OUTPUT_TYPE;

    private CreateBridge() {}
}
