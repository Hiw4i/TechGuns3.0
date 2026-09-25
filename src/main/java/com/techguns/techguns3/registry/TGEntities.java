package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.entity.BanditEntity;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.entity.ZombieSoldierEntity;
import com.techguns.techguns3.turret.TurretHeadEntity;
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

    /** Short-lived tesla arc link. Static geometry, synced once, never moves. */
    public static final DeferredHolder<EntityType<?>, EntityType<TeslaArcEntity>> TESLA_ARC =
            ENTITY_TYPES.registerEntityType("tesla_arc",
                    TeslaArcEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(Integer.MAX_VALUE));

    /** 3-tick muzzle/impact star. Static, synced once. */
    public static final DeferredHolder<EntityType<?>, EntityType<MuzzleFlashEntity>> MUZZLE_FLASH =
            ENTITY_TYPES.registerEntityType("muzzle_flash",
                    MuzzleFlashEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(Integer.MAX_VALUE));

    public static final DeferredHolder<EntityType<?>, EntityType<ZombieSoldierEntity>> ZOMBIE_SOLDIER =
            ENTITY_TYPES.registerEntityType("zombie_soldier", ZombieSoldierEntity::new,
                    MobCategory.MONSTER, builder -> builder.sized(0.6f, 1.95f)
                            .clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<BanditEntity>> BANDIT =
            ENTITY_TYPES.registerEntityType("bandit", BanditEntity::new,
                    MobCategory.MONSTER, builder -> builder.sized(0.6f, 1.95f)
                            .clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<TurretHeadEntity>> TURRET_HEAD =
            ENTITY_TYPES.registerEntityType("turret_head", TurretHeadEntity::new,
                    MobCategory.MISC, builder -> builder.sized(0.9f, 0.9f)
                            .clientTrackingRange(8).updateInterval(1));
}
