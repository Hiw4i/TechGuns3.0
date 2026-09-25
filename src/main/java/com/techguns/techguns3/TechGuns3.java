package com.techguns.techguns3;

import com.mojang.logging.LogUtils;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGBlocks;
import com.techguns.techguns3.registry.TGBlockEntities;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.registry.TGItems;
import com.techguns.techguns3.registry.TGMenus;
import com.techguns.techguns3.registry.TGSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

/**
 * TechGuns 3.0 — NeoForge 26.3 port of Techguns2-CE.
 *
 * <p>First playable slice: three firearms, two armed NPCs and a block mounted turret.
 * Original 1.12 sources: techguns.TGuns / TGBlocks / TGItems / TGConfig.</p>
 */
@Mod(TechGuns3.MODID)
public class TechGuns3 {
    public static final String MODID = "techguns3";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TechGuns3(IEventBus modEventBus, ModContainer modContainer) {
        TGBlocks.BLOCKS.register(modEventBus);
        TGBlockEntities.TYPES.register(modEventBus);
        TGItems.ITEMS.register(modEventBus);
        TGMenus.TYPES.register(modEventBus);
        TGSounds.SOUND_EVENTS.register(modEventBus);
        TGEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(TechGuns3::registerEntityAttributes);
        modEventBus.addListener(TechGuns3::registerSpawnPlacements);
        TGDataComponents.DATA_COMPONENTS.register(modEventBus);
        TGTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, TGConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TGConfig.CLIENT_SPEC);

        modEventBus.addListener(GunPackets::register);

        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(TGEntities.ZOMBIE_SOLDIER.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 50.0).build());
        event.put(TGEntities.BANDIT.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 40.0).build());
        event.put(TGEntities.TURRET_HEAD.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0).build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TGEntities.ZOMBIE_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TGEntities.BANDIT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[TechGuns3] server starting, gun damage multiplier x{}", TGConfig.gunDamageMultiplier());
        if ("autofire".equals(System.getenv("TECHGUNS3_SELFTEST"))) {
            LOGGER.info("[TechGuns3] TECHGUNS3_SELFTEST=autofire, running headless gun self-test");
            com.techguns.techguns3.test.GunSelfTestCommand.runHeadless(event.getServer());
        }
    }
}
