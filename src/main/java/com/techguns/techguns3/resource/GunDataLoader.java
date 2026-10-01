package com.techguns.techguns3.resource;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GunDefinition;
import com.techguns.techguns3.registry.GunRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Datapack loader for {@code data/<namespace>/guns/*.json}.
 *
 * <p>Vanilla-style reload listener: runs on every {@code /reload}, datapack
 * stacks merge (later packs override), broken files are skipped with an error
 * log while the last good map stays live. Never throws into world load.</p>
 */
public final class GunDataLoader extends SimpleJsonResourceReloadListener<GunDefinition> {
    public static final FileToIdConverter LISTER = FileToIdConverter.json("guns");

    public GunDataLoader() {
        // Codec is required by the parent constructor but prepare() below is
        // fully overridden, so parsing always goes through the per-file loop.
        super(GunDefinition.CODEC, LISTER);
    }

    @Override
    protected Map<Identifier, GunDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, GunDefinition> out = new HashMap<>();
        var files = LISTER.listMatchingResources(manager);
        for (var entry : files.entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier gunId = LISTER.fileToId(fileId);
            try (var reader = entry.getValue().openAsReader()) {
                // Per-file isolation: one broken gun logs an error and is skipped,
                // the rest of the map still loads (vanilla scanDirectory would
                // fail the whole batch on a duplicate id instead).
                JsonElement json = net.minecraft.util.StrictJsonParser.parse(reader);
                var result = GunDefinition.CODEC.parse(JsonOps.INSTANCE, json);
                if (result.isSuccess()) {
                    GunDefinition def = result.getOrThrow();
                    if (def.schemaVersion() != GunDefinition.CURRENT_SCHEMA) {
                        TechGuns3.LOGGER.warn("[TechGuns3] gun {} schema_version {} != {}, " +
                                "loading anyway but fields may mismatch",
                                gunId, def.schemaVersion(), GunDefinition.CURRENT_SCHEMA);
                    }
                    String err = def.validate();
                    if (err != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] gun {} invalid: {}", gunId, err);
                        continue;
                    }
                    if (out.putIfAbsent(gunId, def) != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] duplicate gun id {}", gunId);
                    }
                } else {
                    TechGuns3.LOGGER.error("[TechGuns3] couldn't parse gun {}: {}",
                            gunId, result.error().map(Object::toString).orElse("unknown"));
                }
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TechGuns3] couldn't read gun {} from {}", gunId, fileId, e);
            }
        }
        TechGuns3.LOGGER.info("[TechGuns3] gun prepare: {} files from {}", out.size(), files.size());
        return out;
    }

    @Override
    protected void apply(Map<Identifier, GunDefinition> preparations, ResourceManager manager, ProfilerFiller profiler) {
        GunRegistry.apply(preparations);
    }
}
