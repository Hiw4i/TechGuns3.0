package com.techguns.techguns3.entity;

import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class ZombieSoldierEntity extends GunSoldierEntity {
    public ZombieSoldierEntity(EntityType<? extends ZombieSoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected GenericGunItem defaultGun() {
        return TGItems.PISTOL.get();
    }
}
