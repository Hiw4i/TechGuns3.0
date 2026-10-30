package com.techguns.techguns3.resource;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.MobRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Datapack loader for {@code data/<namespace>/mobs/*.json}.
 *
 * <p>Same contract as {@link GunDataLoader}: runs on every {@code /reload},
 * one broken profile logs an error and is skipped while the rest stays live,
 * and the previous good map is kept when the whole pack is empty.</p>
 */
public final class MobDataLoader extends SimpleJsonResourceReloadListener<MobProfile> {
    public static final FileToIdConverter LISTER = FileToIdConverter.json("mobs");

    public MobDataLoader() {
        super(MobProfile.CODEC, LISTER);
    }

    @Override
    protected Map<Identifier, MobProfile> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, MobProfile> out = new HashMap<>();
        var files = LISTER.listMatchingResources(manager);
        for (var entry : files.entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier mobId = LISTER.fileToId(fileId);
            try (var reader = entry.getValue().openAsReader()) {
                JsonElement json = StrictJsonParser.parse(reader);
                var result = MobProfile.CODEC.parse(JsonOps.INSTANCE, json);
                if (result.isSuccess()) {
                    MobProfile profile = result.getOrThrow();
                    if (profile.schemaVersion() != MobProfile.CURRENT_SCHEMA) {
                        TechGuns3.LOGGER.warn("[TechGuns3] mob {} schema_version {} != {}, loading anyway but fields may mismatch",
                                mobId, profile.schemaVersion(), MobProfile.CURRENT_SCHEMA);
                    }
                    String err = profile.validate();
                    if (err != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] mob {} invalid: {}", mobId, err);
                        continue;
                    }
                    if (out.putIfAbsent(mobId, profile) != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] duplicate mob profile id {}", mobId);
                    }
                } else {
                    TechGuns3.LOGGER.error("[TechGuns3] couldn't parse mob {}: {}",
                            mobId, result.error().map(Object::toString).orElse("unknown"));
                }
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TechGuns3] couldn't read mob {} from {}", mobId, fileId, e);
            }
        }
        TechGuns3.LOGGER.info("[TechGuns3] mob prepare: {} files from {}", out.size(), files.size());
        return out;
    }

    @Override
    protected void apply(Map<Identifier, MobProfile> preparations, ResourceManager manager, ProfilerFiller profiler) {
        MobRegistry.apply(preparations);
    }
}
