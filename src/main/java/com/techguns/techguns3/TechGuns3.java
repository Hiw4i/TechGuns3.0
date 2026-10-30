package com.techguns.techguns3;

import com.mojang.logging.LogUtils;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGBlocks;
import com.techguns.techguns3.registry.TGParticles;
import com.techguns.techguns3.registry.TGBlockEntities;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGEffects;
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
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

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

    /**
     * The profile-driven NPC roster: one list for attribute defaults and spawn
     * placements, so adding an NPC cannot update one and forget the other.
     */
    private static final List<Supplier<EntityType<TGArmedMob>>> PROFILED_NPCS = List.of(
            TGEntities.ARMY_SOLDIER, TGEntities.COMMANDO, TGEntities.OUTCAST,
            TGEntities.ZOMBIE_POLICEMAN, TGEntities.ZOMBIE_MINER, TGEntities.PSYCHO_STEVE,
            TGEntities.SKELETON_SOLDIER, TGEntities.ZOMBIE_PIGMAN_SOLDIER,
            TGEntities.SUPER_MUTANT, TGEntities.ZOMBIE_FARMER);

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
        TGEffects.EFFECTS.register(modEventBus);
        TGParticles.TYPES.register(modEventBus);
        TGTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // SYNCED, not LOCAL: the common options (damage, gore, fx density) are
        // read on the client as well, so the server has to push them.
        modContainer.registerConfig(ModConfig.Type.SYNCED, TGConfig.COMMON_SPEC);
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
        // Registration-time defaults for the profile-driven NPCs; the datapack
        // profile re-applies them (and more) on spawn — see TGArmedMob.applyProfile.
        for (var type : PROFILED_NPCS) {
            event.put(type.get(), Monster.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, 25.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.28)
                    .add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.FOLLOW_RANGE, 48.0).build());
        }
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TGEntities.ZOMBIE_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TGEntities.BANDIT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var type : PROFILED_NPCS) {
            event.register(type.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[TechGuns3] server starting, gun damage multiplier x{}", TGConfig.gunDamageMultiplier());
        com.techguns.techguns3.test.TGSelfTests.maybeRun(event.getServer());
    }

    @SubscribeEvent
    public void onAddReloadListeners(net.neoforged.neoforge.event.AddServerReloadListenersEvent event) {
        event.addListener(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "guns"),
                new com.techguns.techguns3.resource.GunDataLoader());
        event.addListener(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "mobs"),
                new com.techguns.techguns3.resource.MobDataLoader());
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var payload = com.techguns.techguns3.network.GunServerSync.encodeCurrent();
        event.getRelevantPlayers().forEach(p -> {
            try {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
            } catch (Exception e) {
                LOGGER.error("[TechGuns3] gun sync failed", e);
            }
        });
    }
}
