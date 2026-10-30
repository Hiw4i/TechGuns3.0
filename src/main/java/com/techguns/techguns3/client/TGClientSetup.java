package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.particle.BulletHoleParticle;
import com.techguns.techguns3.client.particle.CurveParticle;
import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.registry.TGBlockEntities;
import com.techguns.techguns3.registry.TGParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
        event.registerEntityRenderer(TGEntities.BEAM.get(), BeamRenderer::new);
        event.registerEntityRenderer(TGEntities.BLAST.get(), BlastRenderer::new);
        event.registerEntityRenderer(TGEntities.CHARGE_ORB.get(), ChargeOrbRenderer::new);
        event.registerEntityRenderer(TGEntities.MUZZLE_FLASH.get(), MuzzleFlashRenderer::new);
        event.registerEntityRenderer(TGEntities.SHELL_CASING.get(), ShellCasingRenderer::new);
        event.registerEntityRenderer(TGEntities.SLIME_BLOB.get(), SlimeBlobRenderer::new);
        event.registerEntityRenderer(TGEntities.FLYING_GIB.get(), FlyingGibRenderer::new);
        event.registerEntityRenderer(TGEntities.ZOMBIE_SOLDIER.get(), GunSoldierRenderer::new);
        event.registerEntityRenderer(TGEntities.BANDIT.get(), GunSoldierRenderer::new);
        for (var type : java.util.List.of(
                TGEntities.ARMY_SOLDIER.get(), TGEntities.COMMANDO.get(), TGEntities.OUTCAST.get(),
                TGEntities.ZOMBIE_POLICEMAN.get(), TGEntities.ZOMBIE_MINER.get(), TGEntities.PSYCHO_STEVE.get(),
                TGEntities.SKELETON_SOLDIER.get(), TGEntities.ZOMBIE_PIGMAN_SOLDIER.get(),
                TGEntities.SUPER_MUTANT.get(), TGEntities.ZOMBIE_FARMER.get())) {
            event.registerEntityRenderer(type, GunSoldierRenderer::new);
        }
        event.registerEntityRenderer(TGEntities.TURRET_HEAD.get(), GeoTurretHeadRenderer::new);
        event.registerBlockEntityRenderer(TGBlockEntities.TURRET_BASE.get(), GeoTurretBaseRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TGMenus.TURRET.get(), TurretScreen::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GunHudOverlay.LAYER_ID, GunHudOverlay::render);
        event.registerAboveAll(GunCrosshairOverlay.LAYER_ID, GunCrosshairOverlay::render);
        event.registerAboveAll(ScopeOverlay.LAYER_ID, ScopeOverlay::render);
        event.registerAboveAll(BlastFlashOverlay.LAYER_ID, BlastFlashOverlay::render);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TGParticles.GLOW.get(), sprites -> (options, level, x, y, z, dx, dy, dz, rand) ->
                new CurveParticle(level, x, y, z, dx, dy, dz, sprites.first(),
                        options.color(), options.endColor(), options.size0(), options.size1(),
                        options.life(), options.gravity(), options.drag(), true, 0.0f));
        event.registerSpriteSet(TGParticles.PUFF.get(), sprites -> (options, level, x, y, z, dx, dy, dz, rand) ->
                new CurveParticle(level, x, y, z, dx, dy, dz, sprites.first(),
                        options.color(), options.endColor(), options.size0(), options.size1(),
                        options.life(), options.gravity(), options.drag(), false, 0.6f));
        event.registerSpriteSet(TGParticles.FLAME.get(), sprites -> (options, level, x, y, z, dx, dy, dz, rand) ->
                new CurveParticle(level, x, y, z, dx, dy, dz, sprites.first(),
                        options.color(), options.endColor(), options.size0(), options.size1(),
                        options.life(), options.gravity(), options.drag(), true, 0.8f));
        // Gore: shaded droplets (never fullbright) so blood reads as liquid, not sparks.
        event.registerSpriteSet(TGParticles.BLOOD.get(), sprites -> (options, level, x, y, z, dx, dy, dz, rand) ->
                new CurveParticle(level, x, y, z, dx, dy, dz, sprites.first(),
                        options.color(), options.endColor(), options.size0(), options.size1(),
                        options.life(), options.gravity(), options.drag(), false, 0.0f));
        // Bullet holes: a flat quad on the face that was hit.
        event.registerSpriteSet(TGParticles.BULLET_HOLE.get(), sprites -> (options, level, x, y, z, dx, dy, dz, rand) -> {
            var faces = net.minecraft.core.Direction.values();
            var face = faces[Math.floorMod(options.face(), faces.length)];
            return new BulletHoleParticle(level, x, y, z, sprites.first(), face, options.pos());
        });
    }
}
