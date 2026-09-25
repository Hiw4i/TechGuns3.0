package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.turret.TurretBaseBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MVP wave-1 blocks.
 *
 * <p>1.12 mapping notes (from {@code techguns.TGBlocks} + {@code EnumOreType}):
 * <ul>
 *   <li>{@code basicore} with meta {@code ORE_COPPER/TIN/LEAD/TITANIUM/URANIUM}
 *       → five standalone blocks below (1.13 flattening: no meta blocks on 26.3).</li>
 *   <li>Hardness/mining level/light copied from {@code EnumOreType}.</li>
 *   <li>{@code metalpanel} had N variants via {@code GenericBlockMetaEnumCamoChangeable};
 *       MVP keeps a single {@code metal_panel} placeholder, variants return later.</li>
 * </ul>
 * Ore generation and loot tables are supplied as data pack resources.</p>
 */
public final class TGBlocks {
    private TGBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TechGuns3.MODID);

    private static DeferredBlock<Block> ore(String id, float hardness, int light) {
        return BLOCKS.registerSimpleBlock(id, () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(hardness, 3.0f)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> light));
    }

    private static DeferredBlock<Block> xpOre(String id, float hardness, int light) {
        return BLOCKS.registerBlock(id,
                props -> new DropExperienceBlock(UniformInt.of(1, 3), props),
                () -> BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .sound(SoundType.STONE)
                        .strength(hardness, 3.0f)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> light));
    }

    public static final DeferredBlock<Block> COPPER_ORE = ore("copper_ore", 4.0f, 0);
    public static final DeferredBlock<Block> TIN_ORE = ore("tin_ore", 4.0f, 0);
    public static final DeferredBlock<Block> LEAD_ORE = ore("lead_ore", 6.0f, 0);
    /** Uranium glowed (light 4) in 1.12 {@code EnumOreType}. */
    public static final DeferredBlock<Block> URANIUM_ORE = xpOre("uranium_ore", 7.0f, 4);
    public static final DeferredBlock<Block> TITANIUM_ORE = ore("titanium_ore", 8.0f, 0);

    public static final DeferredBlock<Block> METAL_PANEL = BLOCKS.registerSimpleBlock("metal_panel",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(8.0f, 6.0f)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<TurretBaseBlock> TURRET_BASE = BLOCKS.registerBlock("turret_base",
            TurretBaseBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).sound(SoundType.METAL)
                    .strength(5.0f, 12.0f).requiresCorrectToolForDrops());

    private static <T extends Block> DeferredItem<BlockItem> blockItem(DeferredBlock<T> block) {
        return TGItems.ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
    }

    // Kept as explicit fields (instead of wiring through the tab via blocks) so datagen
    // and recipes can reference stable holders.
    public static final DeferredItem<BlockItem> COPPER_ORE_ITEM = blockItem(COPPER_ORE);
    public static final DeferredItem<BlockItem> TIN_ORE_ITEM = blockItem(TIN_ORE);
    public static final DeferredItem<BlockItem> LEAD_ORE_ITEM = blockItem(LEAD_ORE);
    public static final DeferredItem<BlockItem> URANIUM_ORE_ITEM = blockItem(URANIUM_ORE);
    public static final DeferredItem<BlockItem> TITANIUM_ORE_ITEM = blockItem(TITANIUM_ORE);
    public static final DeferredItem<BlockItem> METAL_PANEL_ITEM = blockItem(METAL_PANEL);
    public static final DeferredItem<BlockItem> TURRET_BASE_ITEM = blockItem(TURRET_BASE);
}
