package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.registry.TGBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.techguns.techguns3.registry.TGMenus;

/**
 * Client-side renderer + HUD wiring. Input handling lives in
 * {@link GunClientHandler}, keybinds in {@link TGKeyMappings} (both
 * self-register via {@code EventBusSubscriber}).
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class TGClientSetup {
    private TGClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TGEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(TGEntities.TESLA_ARC.get(), TeslaArcRenderer::new);
        event.registerEntityRenderer(TGEntities.MUZZLE_FLASH.get(), MuzzleFlashRenderer::new);
        event.registerEntityRenderer(TGEntities.ZOMBIE_SOLDIER.get(), GunSoldierRenderer::new);
        event.registerEntityRenderer(TGEntities.BANDIT.get(), GunSoldierRenderer::new);
        event.registerEntityRenderer(TGEntities.TURRET_HEAD.get(), GeoTurretHeadRenderer::new);
        event.registerBlockEntityRenderer(TGBlockEntities.TURRET_BASE.get(), GeoTurretBaseRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TGMenus.TURRET.get(), TurretScreen::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AmmoHudOverlay.LAYER_ID, AmmoHudOverlay::render);
    }
}
