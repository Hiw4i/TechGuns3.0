package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.registry.TGEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-side renderer wiring. 1.12 registered one renderer per projectile type;
 * wave 1 reuses vanilla {@code ThrownItemRenderer} for the single bullet entity.
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class TGClientSetup {
    private TGClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TGEntities.BULLET.get(), ThrownItemRenderer::new);
    }
}
