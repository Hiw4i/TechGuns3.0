package com.techguns.techguns3.resource;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.techguns.techguns3.TechGuns3;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Shared load-validate-log loop for the mod's datapack JSON loaders
 * ({@code guns/*.json}, {@code mobs/*.json}).
 *
 * <p>Contract: runs on every {@code /reload}; per-file isolation (one broken
 * file logs an error and is skipped, the rest still loads); duplicate ids are
 * resolved last-wins with a warning (datapack stack order decides); the caller
 * keeps the previous good map when the fresh one comes back empty.</p>
 */
final class DataLoaders {
    private DataLoaders() {}

    /** Per-type rules: schema version pair and the semantic validation pass. */
    record Rules<T>(ToIntFunction<T> schemaVersion, int currentSchema, Function<T, String> validate) {}

    static <T> Map<Identifier, T> loadValidated(ResourceManager manager, FileToIdConverter lister,
                                                Codec<T> codec, String kind, Rules<T> rules) {
        Map<Identifier, T> out = new HashMap<>();
        var files = lister.listMatchingResources(manager);
        for (var entry : files.entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier id = lister.fileToId(fileId);
            try (var reader = entry.getValue().openAsReader()) {
                JsonElement json = StrictJsonParser.parse(reader);
                var result = codec.parse(JsonOps.INSTANCE, json);
                if (result.isSuccess()) {
                    T value = result.getOrThrow();
                    if (rules.schemaVersion().applyAsInt(value) != rules.currentSchema()) {
                        TechGuns3.LOGGER.warn("[TechGuns3] {} {} schema_version {} != {}, " +
                                        "loading anyway but fields may mismatch",
                                kind, id, rules.schemaVersion().applyAsInt(value), rules.currentSchema());
                    }
                    String err = rules.validate().apply(value);
                    if (err != null) {
                        TechGuns3.LOGGER.error("[TechGuns3] {} {} invalid: {}", kind, id, err);
                        continue;
                    }
                    T previous = out.put(id, value);
                    if (previous != null) {
                        // Last-wins, matching the vanilla datapack merge order.
                        TechGuns3.LOGGER.warn("[TechGuns3] duplicate {} id {}: later pack overrides",
                                kind, id);
                    }
                } else {
                    TechGuns3.LOGGER.error("[TechGuns3] couldn't parse {} {}: {}",
                            kind, id, result.error().map(Object::toString).orElse("unknown"));
                }
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TechGuns3] couldn't read {} {} from {}", kind, id, fileId, e);
            }
        }
        TechGuns3.LOGGER.info("[TechGuns3] {} prepare: {} files from {}", kind, out.size(), files.size());
        return out;
    }
}
