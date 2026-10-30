package com.techguns.techguns3.entity;

import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.mob.TGArmedMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Legacy shell of the wave-1 shared gun AI: all behaviour now lives in the
 * profile-driven {@link TGArmedMob}. Kept so {@code bandit}/{@code zombie_soldier}
 * keep their ids/loot/textures and provide the fallback weapon used when a
 * datapack profile is missing or unarmed.
 */
public abstract class GunSoldierEntity extends TGArmedMob {
    protected GunSoldierEntity(EntityType<? extends GunSoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    @Nullable
    protected abstract GenericGunItem defaultGun();
}
