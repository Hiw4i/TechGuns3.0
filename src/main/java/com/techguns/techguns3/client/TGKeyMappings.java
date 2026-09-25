package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Dedicated gun keys. Fire stays on LMB (vanilla attack key, intercepted for guns);
 * zoom stays on RMB (vanilla use key); reload gets its own bind, default {@code R}.
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class TGKeyMappings {
    private TGKeyMappings() {}

    public static KeyMapping reload() {
        return Holder.RELOAD;
    }

    private static final class Holder {
        static final KeyMapping.Category CATEGORY =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TechGuns3.MODID, "guns"));
        static final KeyMapping RELOAD = new KeyMapping("key.techguns3.reload",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(Holder.CATEGORY);
        event.register(Holder.RELOAD);
    }
}
