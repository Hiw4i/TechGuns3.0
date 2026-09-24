package com.tuchguns.tuchguns3;

import com.tuchguns.tuchguns3.registry.TGBlocks;
import com.tuchguns.tuchguns3.registry.TGItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Single MVP creative tab. 1.12 used the vanilla combat tab + Galacticraft tabs;
 * on 26.3 we get our own tab so guns/ammo/ores stay together.
 */
public final class TGTabs {
    private TGTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TuchGuns3.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tuchguns3"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> TGItems.PISTOL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Wave-1 guns
                        output.accept(TGItems.PISTOL.get());
                        output.accept(TGItems.REVOLVER.get());
                        output.accept(TGItems.AK47.get());
                        output.accept(TGItems.M4.get());
                        output.accept(TGItems.THOMPSON.get());
                        output.accept(TGItems.COMBAT_SHOTGUN.get());
                        output.accept(TGItems.BOLT_ACTION.get());
                        // Wave-1 ammo
                        output.accept(TGItems.PISTOL_ROUNDS.get());
                        output.accept(TGItems.RIFLE_ROUNDS.get());
                        output.accept(TGItems.SHOTGUN_ROUNDS.get());
                        output.accept(TGItems.PISTOL_MAGAZINE.get());
                        output.accept(TGItems.ASSAULT_RIFLE_MAGAZINE.get());
                        output.accept(TGItems.SMG_MAGAZINE.get());
                        // Wave-1 ingots
                        output.accept(TGItems.TIN_INGOT.get());
                        output.accept(TGItems.LEAD_INGOT.get());
                        output.accept(TGItems.TITANIUM_INGOT.get());
                        output.accept(TGItems.URANIUM_INGOT.get());
                        // Wave-1 blocks
                        output.accept(TGBlocks.COPPER_ORE_ITEM.get());
                        output.accept(TGBlocks.TIN_ORE_ITEM.get());
                        output.accept(TGBlocks.LEAD_ORE_ITEM.get());
                        output.accept(TGBlocks.URANIUM_ORE_ITEM.get());
                        output.accept(TGBlocks.TITANIUM_ORE_ITEM.get());
                        output.accept(TGBlocks.METAL_PANEL_ITEM.get());
                    })
                    .build());
}
