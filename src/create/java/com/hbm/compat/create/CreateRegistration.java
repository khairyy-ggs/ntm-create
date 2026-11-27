package com.hbm.compat.create;

import com.hbm.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers the Shaft Output block, its item and its block entity type; touched only when Create is present. */
final class CreateRegistration {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "shaft_output");
    private static ShaftOutputBlock block;
    private static BlockEntityType<ShaftOutputBlockEntity> type;

    private CreateRegistration() {}

    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            block = new ShaftOutputBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion());
            helper.register(ID, block);
        });
        event.register(Registries.ITEM, helper -> helper.register(ID, new BlockItem(block, new Item.Properties())));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            type = BlockEntityType.Builder.of((pos, state) -> new ShaftOutputBlockEntity(type, pos, state), block).build(null);
            CreateBridge.SHAFT_OUTPUT_TYPE = () -> type;
            helper.register(ID, type);
        });
    }
}
