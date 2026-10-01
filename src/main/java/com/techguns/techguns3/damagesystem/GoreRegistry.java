package com.techguns.techguns3.damagesystem;

import net.minecraft.world.entity.LivingEntity;

/**
 * Who bleeds what color and who may burst into chunks. Port of the 1.12
 * {@code goreMap} + {@code goreStats} ({@code EntityDeathUtils} /
 * {@code DeathEffect.GoreData}): blood RGB per victim kind, generic chunk
 * fallback for everything unlisted.
 *
 * <p>Matching is done on the registry path so modded mobs automatically get
 * the default arterial red instead of nothing.</p>
 */
public final class GoreRegistry {
    private GoreRegistry() {}

    /** Fresh arterial red. */
    public static final int BLOOD_RED = 0xC81818;
    /** Dried dark red, spray end color. */
    public static final int BLOOD_DARK = 0x4A0808;
    /** Bio/acid ichor (biogun bleed override). */
    public static final int ICHOR_GREEN = 0x4AE83A;
    /** Victim has no blood: robots, iron golems — sparks instead. */
    public static final int NO_BLOOD = -1;

    /**
     * Blood color for a victim, or {@link #NO_BLOOD} for bloodless machines.
     */
    public static int bloodColor(LivingEntity victim) {
        String path = entityPath(victim);
        if (path.contains("turret") || path.contains("robot") || path.contains("mech")
                || path.equals("iron_golem") || path.equals("copper_golem")) {
            return NO_BLOOD;
        }
        if (path.contains("skeleton") || path.equals("bogged")) return 0xE8E4D8;
        if (path.contains("slime") || path.contains("creeper") || path.contains("frog")) return 0x32C83C;
        if (path.contains("magma") || path.contains("blaze") || path.contains("strider")) return 0xFF8A1A;
        if (path.contains("enderman") || path.contains("shulker") || path.contains("chorus")) return 0xA024A7;
        if (path.contains("spider") || path.contains("silverfish") || path.contains("bee")) return 0x5A7A1F;
        if (path.contains("squid") || path.contains("tadpole") || path.contains("axolotl")) return 0x1A3A6A;
        if (path.contains("warden")) return 0x1A8A7A;
        if (path.contains("snow_golem") || path.equals("snowman")) return 0xF0F0F0;
        if (path.contains("ghast")) return 0xD8D0C0;
        return BLOOD_RED;
    }

    /** How many chunks a gibbed victim of this size throws (before the config cap). */
    public static int gibCount(LivingEntity victim) {
        float height = victim.getBbHeight();
        if (height < 0.7f) return 5;
        if (height < 1.4f) return 7;
        if (height < 2.2f) return 8;
        return 10;
    }

    /** Chunk scale from victim volume, clamped so chickens don't throw logs. */
    public static float gibScale(LivingEntity victim) {
        float volume = (victim.getBbWidth() + victim.getBbHeight()) / 2.0f;
        return Math.max(0.7f, Math.min(2.5f, volume / 1.05f));
    }

    private static String entityPath(LivingEntity victim) {
        try {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
            return id == null ? "" : id.getPath();
        } catch (Exception e) {
            return "";
        }
    }
}
