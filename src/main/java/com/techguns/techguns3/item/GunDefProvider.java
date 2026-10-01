package com.techguns.techguns3.item;

import com.techguns.techguns3.registry.GunRegistry;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Side-aware gun definition lookup without pulling client classes onto the
 * dedicated server. Common code calls {@link #get}; on the client this prefers
 * the synced cache so {@code /reload} updates HUD/FOV/recoil instantly.
 */
public final class GunDefProvider {
    private GunDefProvider() {}

    public static GunDefinition get(Identifier gunId) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            GunDefinition client = ClientHook.get(gunId);
            if (client != null) return client;
        }
        return GunRegistry.get(gunId);
    }

    /**
     * Client-only indirection, never touched on a dedicated server (the caller
     * guards on dist). Catches only {@link LinkageError}: a genuinely missing
     * client class must degrade to registry builtins, but ordinary bugs
     * (NPE and friends) must still crash loudly instead of hiding behind
     * stale data.
     */
    private static final class ClientHook {
        static GunDefinition get(Identifier gunId) {
            try {
                if (com.techguns.techguns3.client.GunClientCache.has(gunId)) {
                    return com.techguns.techguns3.client.GunClientCache.get(gunId);
                }
                return null;
            } catch (LinkageError e) {
                com.techguns.techguns3.TechGuns3.LOGGER.debug(
                        "[TechGuns3] client gun cache unavailable, using registry", e);
                return null;
            }
        }
    }
}
