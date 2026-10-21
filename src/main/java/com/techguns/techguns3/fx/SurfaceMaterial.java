package com.techguns.techguns3.fx;

import net.minecraft.world.level.block.SoundType;

import java.util.Set;

/**
 * The material of a surface, derived from the sound type of the block.
 *
 * <p>It decides how a bullet sounds when it hits and what the impact looks
 * like: metal throws bright sparks, wood throws splinters, loose material
 * throws dust.</p>
 */
public enum SurfaceMaterial {
    ROCK,
    METAL,
    WOOD,
    DIRT,
    GLASS;

    private static final Set<SoundType> WOOD_SOUNDS = Set.of(
            SoundType.WOOD, SoundType.LADDER, SoundType.BAMBOO_WOOD,
            SoundType.NETHER_WOOD, SoundType.CHERRY_WOOD, SoundType.SCAFFOLDING);
    private static final Set<SoundType> GLASS_SOUNDS = Set.of(
            SoundType.GLASS, SoundType.AMETHYST, SoundType.AMETHYST_CLUSTER,
            SoundType.SMALL_AMETHYST_BUD, SoundType.MEDIUM_AMETHYST_BUD, SoundType.LARGE_AMETHYST_BUD);
    private static final Set<SoundType> METAL_SOUNDS = Set.of(
            SoundType.METAL, SoundType.ANVIL, SoundType.COPPER, SoundType.CHAIN,
            SoundType.NETHERITE_BLOCK, SoundType.LODESTONE);
    private static final Set<SoundType> DIRT_SOUNDS = Set.of(
            SoundType.SAND, SoundType.GRAVEL, SoundType.SNOW, SoundType.SOUL_SAND,
            SoundType.SOUL_SOIL, SoundType.MUD, SoundType.GRASS, SoundType.ROOTED_DIRT);

    /** The material of the block that makes this sound. */
    public static SurfaceMaterial of(SoundType sound) {
        if (WOOD_SOUNDS.contains(sound)) return WOOD;
        if (GLASS_SOUNDS.contains(sound)) return GLASS;
        if (METAL_SOUNDS.contains(sound)) return METAL;
        if (DIRT_SOUNDS.contains(sound)) return DIRT;
        return ROCK;
    }
}
