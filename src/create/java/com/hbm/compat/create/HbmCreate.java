package com.hbm.compat.create;

import com.hbm.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge's entry to the Create Energy Shaft's Create side. Does nothing unless Create is loaded, and keeps every
 * Create-typed line in {@link CreateRegistration}, so a world without Create never loads a Create class.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class HbmCreate {

    private HbmCreate() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (ModList.get().isLoaded("create")) CreateRegistration.register(event);
    }
}
