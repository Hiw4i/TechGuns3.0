package com.techguns.techguns3.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven description of one Techguns mob.
 *
 * <p>Loaded from {@code data/<namespace>/mobs/*.json} through
 * {@link com.techguns.techguns3.resource.MobDataLoader}. Every tunable of a
 * mob lives here: stats, weapon pool, combat discipline and the abilities the
 * AI may use (cover, flanking, block breaking, building, dynamite, pickup).
 * No code changes are needed to rebalance a mob: edit the JSON and run
 * {@code /reload}.</p>
 *
 * <p>Split into nested blocks because {@code RecordCodecBuilder.group} caps at
 * 16 fields. All fields have sane defaults, so a minimal profile is
 * {@code {"combat": {"weapons": ["techguns3:pistol"]}}}.</p>
 */
public record MobProfile(
        int schemaVersion,
        Stats stats,
        Combat combat,
        Abilities abilities,
        SpawnInfo spawn
) {
    public static final int CURRENT_SCHEMA = 1;

    /** Attribute block. Mirrors vanilla attributes one-to-one. */
    public record Stats(
            double maxHealth,
            double movementSpeed,
            double attackDamage,
            double followRange,
            double armor,
            double knockbackResistance,
            double scale,
            double stepHeight,
            double blockBreakSpeed
    ) {
        public static final Stats DEFAULT = new Stats(
                20.0, 0.25, 3.0, 32.0, 0.0, 0.0, 1.0, 0.6, 1.0);

        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.DOUBLE.optionalFieldOf("max_health", 20.0).forGetter(Stats::maxHealth),
                Codec.DOUBLE.optionalFieldOf("movement_speed", 0.25).forGetter(Stats::movementSpeed),
                Codec.DOUBLE.optionalFieldOf("attack_damage", 3.0).forGetter(Stats::attackDamage),
                Codec.DOUBLE.optionalFieldOf("follow_range", 32.0).forGetter(Stats::followRange),
                Codec.DOUBLE.optionalFieldOf("armor", 0.0).forGetter(Stats::armor),
                Codec.DOUBLE.optionalFieldOf("knockback_resistance", 0.0).forGetter(Stats::knockbackResistance),
                Codec.DOUBLE.optionalFieldOf("scale", 1.0).forGetter(Stats::scale),
                Codec.DOUBLE.optionalFieldOf("step_height", 0.6).forGetter(Stats::stepHeight),
                Codec.DOUBLE.optionalFieldOf("block_break_speed", 1.0).forGetter(Stats::blockBreakSpeed)
        ).apply(inst, Stats::new));
    }

    /**
     * Gun handling. {@code accuracy} is 0..1: 1 = aimbot-class grouping,
     * 0 = sprays everywhere. {@code ammo_magazines} is the number of spare
     * magazines; -1 means unlimited (the CE NPC behaviour).
     */
    public record Combat(
            List<Identifier> weapons,
            double accuracy,
            int reactionTicks,
            int burst,
            int burstPauseTicks,
            double preferredRange,
            double maxRange,
            double damageMultiplier,
            int ammoMagazines
    ) {
        public static final Combat DEFAULT = new Combat(
                List.of(), 0.5, 10, 3, 20, 12.0, 24.0, 0.5, -1);

        public static final Codec<Combat> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Identifier.CODEC.listOf().optionalFieldOf("weapons", List.of()).forGetter(Combat::weapons),
                Codec.DOUBLE.optionalFieldOf("accuracy", 0.5).forGetter(Combat::accuracy),
                Codec.INT.optionalFieldOf("reaction_ticks", 10).forGetter(Combat::reactionTicks),
                Codec.INT.optionalFieldOf("burst", 3).forGetter(Combat::burst),
                Codec.INT.optionalFieldOf("burst_pause_ticks", 20).forGetter(Combat::burstPauseTicks),
                Codec.DOUBLE.optionalFieldOf("preferred_range", 12.0).forGetter(Combat::preferredRange),
                Codec.DOUBLE.optionalFieldOf("max_range", 24.0).forGetter(Combat::maxRange),
                Codec.DOUBLE.optionalFieldOf("damage_multiplier", 0.5).forGetter(Combat::damageMultiplier),
                Codec.INT.optionalFieldOf("ammo_magazines", -1).forGetter(Combat::ammoMagazines)
        ).apply(inst, Combat::new));
    }

    /**
     * The "Improved Mobs style" feature switches. Each ability is free to turn
     * on per mob; the global config still has a master switch for griefing.
     *
     * <p>{@code build_blocks} is how many blocks of the material a builder
     * carries. {@code mine_idle} makes miners dig when they have no target.</p>
     */
    public record Abilities(
            boolean cover,
            boolean flank,
            double fleeHealth,
            boolean pickupWeapons,
            double pickupRadius,
            boolean mineBlocks,
            double digRange,
            boolean mineIdle,
            boolean build,
            int buildBlocks,
            Identifier buildMaterial,
            int dynamite,
            Identifier dynamiteItem,
            double dynamiteRange,
            boolean dynamiteSuicide,
            boolean hearPlayers
    ) {
        public static final Abilities DEFAULT = new Abilities(
                true, true, 0.2, true, 8.0,
                false, 16.0, false,
                false, 0, Identifier.withDefaultNamespace("cobblestone"),
                0, Identifier.withDefaultNamespace("tnt"), 16.0, false,
                true);

        public static final Codec<Abilities> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("cover", true).forGetter(Abilities::cover),
                Codec.BOOL.optionalFieldOf("flank", true).forGetter(Abilities::flank),
                Codec.DOUBLE.optionalFieldOf("flee_health", 0.2).forGetter(Abilities::fleeHealth),
                Codec.BOOL.optionalFieldOf("pickup_weapons", true).forGetter(Abilities::pickupWeapons),
                Codec.DOUBLE.optionalFieldOf("pickup_radius", 8.0).forGetter(Abilities::pickupRadius),
                Codec.BOOL.optionalFieldOf("mine_blocks", false).forGetter(Abilities::mineBlocks),
                Codec.DOUBLE.optionalFieldOf("dig_range", 16.0).forGetter(Abilities::digRange),
                Codec.BOOL.optionalFieldOf("mine_idle", false).forGetter(Abilities::mineIdle),
                Codec.BOOL.optionalFieldOf("build", false).forGetter(Abilities::build),
                Codec.INT.optionalFieldOf("build_blocks", 0).forGetter(Abilities::buildBlocks),
                Identifier.CODEC.optionalFieldOf("build_material",
                        Identifier.withDefaultNamespace("cobblestone")).forGetter(Abilities::buildMaterial),
                Codec.INT.optionalFieldOf("dynamite", 0).forGetter(Abilities::dynamite),
                Identifier.CODEC.optionalFieldOf("dynamite_item",
                        Identifier.withDefaultNamespace("tnt")).forGetter(Abilities::dynamiteItem),
                Codec.DOUBLE.optionalFieldOf("dynamite_range", 16.0).forGetter(Abilities::dynamiteRange),
                Codec.BOOL.optionalFieldOf("dynamite_suicide", false).forGetter(Abilities::dynamiteSuicide),
                Codec.BOOL.optionalFieldOf("hear_players", true).forGetter(Abilities::hearPlayers)
        ).apply(inst, Abilities::new));
    }

    /** Documentation/spawner metadata. Natural spawn weights live in biome modifiers. */
    public record SpawnInfo(int weight, int min, int max) {
        public static final SpawnInfo DEFAULT = new SpawnInfo(0, 1, 1);

        public static final Codec<SpawnInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.optionalFieldOf("weight", 0).forGetter(SpawnInfo::weight),
                Codec.INT.optionalFieldOf("min", 1).forGetter(SpawnInfo::min),
                Codec.INT.optionalFieldOf("max", 1).forGetter(SpawnInfo::max)
        ).apply(inst, SpawnInfo::new));
    }

    public static final Codec<MobProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA).forGetter(MobProfile::schemaVersion),
            Stats.CODEC.optionalFieldOf("stats", Stats.DEFAULT).forGetter(MobProfile::stats),
            Combat.CODEC.optionalFieldOf("combat", Combat.DEFAULT).forGetter(MobProfile::combat),
            Abilities.CODEC.optionalFieldOf("abilities", Abilities.DEFAULT).forGetter(MobProfile::abilities),
            SpawnInfo.CODEC.optionalFieldOf("spawn", SpawnInfo.DEFAULT).forGetter(MobProfile::spawn)
    ).apply(inst, MobProfile::new));

    // --- convenience ---

    public boolean armed() {
        return !combat.weapons().isEmpty();
    }

    /** Weighted-free pick: NPCs pick a random service weapon per spawn. */
    @Nullable
    public Identifier pickWeapon(RandomSource random) {
        List<Identifier> pool = combat.weapons();
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    public Optional<Item> buildMaterialItem() {
        Item item = BuiltInRegistries.ITEM.getValue(abilities.buildMaterial());
        return item == null || item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    public Optional<Item> dynamiteItem() {
        Item item = BuiltInRegistries.ITEM.getValue(abilities.dynamiteItem());
        return item == null || item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    /** @return error text or {@code null} when the profile is usable. */
    public String validate() {
        if (stats.maxHealth() <= 0.0 || stats.maxHealth() > 1024.0) return "max_health out of range";
        if (stats.movementSpeed() < 0.0 || stats.movementSpeed() > 4.0) return "movement_speed out of range";
        if (stats.attackDamage() < 0.0) return "attack_damage must not be negative";
        if (stats.armor() < 0.0 || stats.armor() > 30.0) return "armor out of range (0..30)";
        if (stats.knockbackResistance() < 0.0 || stats.knockbackResistance() > 1.0) {
            return "knockback_resistance must be 0..1";
        }
        if (stats.stepHeight() < 0.0 || stats.stepHeight() > 10.0) return "step_height out of range (0..10)";
        if (stats.followRange() < 1.0) return "follow_range must be positive";
        if (stats.scale() < 0.0625 || stats.scale() > 16.0) return "scale out of range";
        if (stats.blockBreakSpeed() < 0.0) return "block_break_speed must not be negative";
        if (combat.accuracy() < 0.0 || combat.accuracy() > 1.0) return "accuracy must be 0..1";
        if (combat.reactionTicks() < 0) return "reaction_ticks must not be negative";
        if (combat.burst() < 1) return "burst must be >= 1";
        if (combat.burstPauseTicks() < 0) return "burst_pause_ticks must not be negative";
        if (combat.preferredRange() <= 0.0 || combat.maxRange() <= 0.0) return "ranges must be positive";
        if (combat.maxRange() < combat.preferredRange()) return "max_range must be >= preferred_range";
        if (combat.damageMultiplier() < 0.0 || combat.damageMultiplier() > 10.0) return "damage_multiplier out of range";
        if (combat.ammoMagazines() < -1) return "ammo_magazines must be -1 or >= 0";
        if (abilities.fleeHealth() < 0.0 || abilities.fleeHealth() > 1.0) return "flee_health must be 0..1";
        if (abilities.pickupRadius() < 0.0) return "pickup_radius must not be negative";
        if (abilities.digRange() < 1.0) return "dig_range must be >= 1";
        if (abilities.buildBlocks() < 0) return "build_blocks must not be negative";
        if (abilities.dynamite() < 0) return "dynamite must not be negative";
        if (abilities.dynamiteRange() < 1.0) return "dynamite_range must be >= 1";
        for (Identifier weapon : combat.weapons()) {
            Item item = BuiltInRegistries.ITEM.getValue(weapon);
            if (item == null || item == Items.AIR) {
                return "unknown weapon item " + weapon;
            }
            if (!(item instanceof com.techguns.techguns3.item.GenericGunItem)) {
                return "weapon " + weapon + " is not a Techguns gun";
            }
        }
        if (abilities.build()) {
            Optional<Item> buildItem = buildMaterialItem();
            if (buildItem.isEmpty()) return "unknown build_material " + abilities.buildMaterial();
            if (!(buildItem.get() instanceof net.minecraft.world.item.BlockItem)) {
                return "build_material " + abilities.buildMaterial() + " is not a block";
            }
        }
        if (abilities.dynamite() > 0 && dynamiteItem().isEmpty()) {
            return "unknown dynamite_item " + abilities.dynamiteItem();
        }
        return null;
    }

    /** New stack of the profile's building material. */
    public ItemStack newBuildStack(int count) {
        return buildMaterialItem()
                .map(item -> new ItemStack(item, Mth.clamp(count, 0, item.getDefaultMaxStackSize())))
                .orElse(ItemStack.EMPTY);
    }
}
