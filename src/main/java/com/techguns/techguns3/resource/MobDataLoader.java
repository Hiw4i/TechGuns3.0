package com.techguns.techguns3.resource;

import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.MobRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

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
        return DataLoaders.loadValidated(manager, LISTER, MobProfile.CODEC, "mob",
                new DataLoaders.Rules<>(MobProfile::schemaVersion,
                        MobProfile.CURRENT_SCHEMA, MobProfile::validate));
    }

    @Override
    protected void apply(Map<Identifier, MobProfile> preparations, ResourceManager manager, ProfilerFiller profiler) {
        MobRegistry.apply(preparations);
    }
}
