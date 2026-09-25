package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.turret.TurretMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TGMenus {
    private TGMenus() {}

    public static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(Registries.MENU, TechGuns3.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TurretMenu>> TURRET =
            TYPES.register("turret", () -> new MenuType<>(TurretMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
