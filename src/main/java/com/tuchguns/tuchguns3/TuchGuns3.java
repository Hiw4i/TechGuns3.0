package com.tuchguns.tuchguns3;

import com.mojang.logging.LogUtils;
import com.tuchguns.tuchguns3.registry.TGBlocks;
import com.tuchguns.tuchguns3.registry.TGDataComponents;
import com.tuchguns.tuchguns3.registry.TGEntities;
import com.tuchguns.tuchguns3.registry.TGItems;
import com.tuchguns.tuchguns3.registry.TGSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * TuchGuns 3.0 — NeoForge 26.3 port of Techguns2-CE.
 *
 * <p>Phase 0/1 scope (MVP wave 1): framework + registries + config + creative tab.
 * No machines, NPCs, dungeons or radiation yet. Shooting logic lands in phase 2.
 * Original 1.12 sources: techguns.TGuns / TGBlocks / TGItems / TGConfig.</p>
 */
@Mod(TuchGuns3.MODID)
public class TuchGuns3 {
    public static final String MODID = "tuchguns3";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TuchGuns3(IEventBus modEventBus, ModContainer modContainer) {
        TGBlocks.BLOCKS.register(modEventBus);
        TGItems.ITEMS.register(modEventBus);
        TGSounds.SOUND_EVENTS.register(modEventBus);
        TGEntities.ENTITY_TYPES.register(modEventBus);
        TGDataComponents.DATA_COMPONENTS.register(modEventBus);
        TGTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, TGConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TGConfig.CLIENT_SPEC);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[TuchGuns3] server starting, gun damage multiplier x{}", TGConfig.gunDamageMultiplier());
    }
}
