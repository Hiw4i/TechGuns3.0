package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.entity.BulletProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wave-1 entities. 1.12 ({@code techguns.TGEntities}) registered ~28 projectile types with
 * tracking range 128 and update interval 1; wave 1 collapses every plain
 * {@code GenericProjectile} shooter onto this single bullet entity. Exotic projectiles
 * (rockets, lasers, tesla arcs, bio blobs, grenades) return as subclasses in later waves.
 */
public final class TGEntities {
    private TGEntities() {}

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(TechGuns3.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletProjectile>> BULLET =
            ENTITY_TYPES.registerEntityType("bullet",
                    BulletProjectile::new, MobCategory.MISC,
                    builder -> builder.sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune());
}
