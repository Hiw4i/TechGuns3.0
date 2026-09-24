package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components. Replaces the 1.12 NBT tags ({@code "ammo"}, {@code "ammovariant"},
 * {@code "camo"}) on {@code GenericGun} stacks.
 *
 * <p>Wave 1 only tracks the loaded round count; camo and ammo-variant tags return in later waves.</p>
 */
public final class TGDataComponents {
    private TGDataComponents() {}

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TechGuns3.MODID);

    /** Rounds currently loaded in the gun. Absent = full magazine (freshly crafted / creative). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO =
            DATA_COMPONENTS.registerComponentType("ammo",
                    builder -> builder.persistent(com.mojang.serialization.Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT));
}
