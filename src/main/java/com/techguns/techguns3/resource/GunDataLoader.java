package com.techguns.techguns3.resource;

import com.techguns.techguns3.item.GunDefinition;
import com.techguns.techguns3.registry.GunRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

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
        return DataLoaders.loadValidated(manager, LISTER, GunDefinition.CODEC, "gun",
                new DataLoaders.Rules<>(GunDefinition::schemaVersion,
                        GunDefinition.CURRENT_SCHEMA, GunDefinition::validate));
    }

    @Override
    protected void apply(Map<Identifier, GunDefinition> preparations, ResourceManager manager, ProfilerFiller profiler) {
        GunRegistry.apply(preparations);
    }
}
