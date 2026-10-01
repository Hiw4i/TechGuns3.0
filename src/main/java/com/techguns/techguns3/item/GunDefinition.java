package com.techguns.techguns3.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Data-driven gun definition. Single source of truth for all tunables that used
 * to live in {@code TGItems.java} as {@code new GunStats(...)} + {@code GunExtras...}.
 *
 * <p>Loaded from {@code data/<namespace>/guns/*.json} via datapack reload
 * (see {@code resource.GunDataLoader}), synced to clients as JSON text.
 * Combat code keeps using {@link GunStats}/{@link GunExtras} via
 * {@link #toStats()} / {@link #toExtras()} so ballistics stay untouched.</p>
 *
 * <p>Split into nested blocks because {@code RecordCodecBuilder.group} caps at
 * 16 fields. JSON shape: {@code ammo, empty_magazine?, ballistic{}, sounds{},
 * handling{}, extras{}} — see {@code data/techguns3/guns/pistol.json}.</p>
 */
public record GunDefinition(
        int schemaVersion,
        Identifier ammo,
        Optional<Identifier> emptyMagazine,
        Ballistic ballistic,
        SoundSet sounds,
        Handling handling,
        Extras extras
) {
    public record ExplosiveDef(float max, float min, float r1, float r2, float block, boolean nuke) {
        public static final Codec<ExplosiveDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.FLOAT.fieldOf("max").forGetter(ExplosiveDef::max),
                Codec.FLOAT.fieldOf("min").forGetter(ExplosiveDef::min),
                Codec.FLOAT.fieldOf("r1").forGetter(ExplosiveDef::r1),
                Codec.FLOAT.fieldOf("r2").forGetter(ExplosiveDef::r2),
                Codec.FLOAT.optionalFieldOf("block", 0.5f).forGetter(ExplosiveDef::block),
                Codec.BOOL.optionalFieldOf("nuke", false).forGetter(ExplosiveDef::nuke)
        ).apply(inst, ExplosiveDef::new));
    }

    /** Core ballistics (16 fields — codec limit). */
    public record Ballistic(
            float damage, int cooldownTicks, int magazine, int reloadTicks, int rangeTtl,
            float spread, float speed, double gravity, int pellets, float pelletSpread,
            float dropStart, float dropEnd, float dropMin, float penetration,
            boolean semiAuto, boolean magazineFed
    ) {
        public static final Codec<Ballistic> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.FLOAT.optionalFieldOf("damage", 8.0f).forGetter(Ballistic::damage),
                Codec.INT.optionalFieldOf("cooldown_ticks", 4).forGetter(Ballistic::cooldownTicks),
                Codec.INT.optionalFieldOf("magazine", 10).forGetter(Ballistic::magazine),
                Codec.INT.optionalFieldOf("reload_ticks", 40).forGetter(Ballistic::reloadTicks),
                Codec.INT.optionalFieldOf("range_ttl", 40).forGetter(Ballistic::rangeTtl),
                Codec.FLOAT.optionalFieldOf("spread", 0.03f).forGetter(Ballistic::spread),
                Codec.FLOAT.optionalFieldOf("speed", 2.0f).forGetter(Ballistic::speed),
                Codec.DOUBLE.optionalFieldOf("gravity", 0.0).forGetter(Ballistic::gravity),
                Codec.INT.optionalFieldOf("pellets", 1).forGetter(Ballistic::pellets),
                Codec.FLOAT.optionalFieldOf("pellet_spread", 0.0f).forGetter(Ballistic::pelletSpread),
                Codec.FLOAT.optionalFieldOf("drop_start", 20.0f).forGetter(Ballistic::dropStart),
                Codec.FLOAT.optionalFieldOf("drop_end", 30.0f).forGetter(Ballistic::dropEnd),
                Codec.FLOAT.optionalFieldOf("drop_min", 5.0f).forGetter(Ballistic::dropMin),
                Codec.FLOAT.optionalFieldOf("penetration", 0.0f).forGetter(Ballistic::penetration),
                Codec.BOOL.optionalFieldOf("semi_auto", false).forGetter(Ballistic::semiAuto),
                Codec.BOOL.optionalFieldOf("magazine_fed", true).forGetter(Ballistic::magazineFed)
        ).apply(inst, Ballistic::new));
    }

    public record SoundSet(
            Optional<Identifier> fire, Optional<Identifier> reload,
            Optional<Identifier> extra, Optional<Identifier> action,
            int actionDelayTicks
    ) {
        public static final Codec<SoundSet> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Identifier.CODEC.optionalFieldOf("fire").forGetter(SoundSet::fire),
                Identifier.CODEC.optionalFieldOf("reload").forGetter(SoundSet::reload),
                Identifier.CODEC.optionalFieldOf("extra").forGetter(SoundSet::extra),
                Identifier.CODEC.optionalFieldOf("action").forGetter(SoundSet::action),
                Codec.INT.optionalFieldOf("action_delay_ticks", 0).forGetter(SoundSet::actionDelayTicks)
        ).apply(inst, SoundSet::new));

        public static SoundSet of(String fire, String reload, String extra, String action, int delay) {
            java.util.function.Function<String, Optional<Identifier>> p =
                    s -> s == null ? Optional.empty() : Optional.of(Identifier.parse(s));
            return new SoundSet(p.apply(fire), p.apply(reload), p.apply(extra), p.apply(action), delay);
        }
    }

    /** Presentation + feel. */
    public record Handling(
            String projectile, boolean canZoom, float zoomFov, float zoomSpreadMult,
            float recoilPitch, float recoilYaw, float muzzleScale, boolean spinsBarrels,
            float slimePerHit, float bulk, int muzzleFlash, int loopTicks, boolean twoHanded
    ) {
        public static final Codec<Handling> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("projectile", "BALLISTIC").forGetter(Handling::projectile),
                Codec.BOOL.optionalFieldOf("can_zoom", true).forGetter(Handling::canZoom),
                Codec.FLOAT.optionalFieldOf("zoom_fov", 0.8f).forGetter(Handling::zoomFov),
                Codec.FLOAT.optionalFieldOf("zoom_spread_mult", 0.5f).forGetter(Handling::zoomSpreadMult),
                Codec.FLOAT.optionalFieldOf("recoil_pitch", 0.8f).forGetter(Handling::recoilPitch),
                Codec.FLOAT.optionalFieldOf("recoil_yaw", 0.3f).forGetter(Handling::recoilYaw),
                Codec.FLOAT.optionalFieldOf("muzzle_scale", 0.7f).forGetter(Handling::muzzleScale),
                Codec.BOOL.optionalFieldOf("spins_barrels", false).forGetter(Handling::spinsBarrels),
                Codec.FLOAT.optionalFieldOf("slime_per_hit", 0.0f).forGetter(Handling::slimePerHit),
                Codec.FLOAT.optionalFieldOf("bulk", 1.0f).forGetter(Handling::bulk),
                Codec.INT.optionalFieldOf("muzzle_flash", 0).forGetter(Handling::muzzleFlash),
                Codec.INT.optionalFieldOf("loop_ticks", 0).forGetter(Handling::loopTicks),
                Codec.BOOL.optionalFieldOf("two_handed", true).forGetter(Handling::twoHanded)
        ).apply(inst, Handling::new));
    }

    /** Exotic mechanics (15 fields). */
    public record Extras(
            Optional<ExplosiveDef> explosive, boolean beam,
            int chargeTicks, int ammoBase, int ammoFull,
            float knockback, boolean ignoreIframes, float homingTurn,
            boolean guided, int lockTime, int lockPersist, double lockRange,
            int radiationTicks, List<Identifier> extraAmmo, boolean continuousBeam
    ) {
        public static final Codec<Extras> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ExplosiveDef.CODEC.optionalFieldOf("explosive").forGetter(Extras::explosive),
                Codec.BOOL.optionalFieldOf("beam", false).forGetter(Extras::beam),
                Codec.INT.optionalFieldOf("charge_ticks", 0).forGetter(Extras::chargeTicks),
                Codec.INT.optionalFieldOf("ammo_base", 1).forGetter(Extras::ammoBase),
                Codec.INT.optionalFieldOf("ammo_full", 1).forGetter(Extras::ammoFull),
                Codec.FLOAT.optionalFieldOf("knockback", 0.0f).forGetter(Extras::knockback),
                Codec.BOOL.optionalFieldOf("ignore_iframes", false).forGetter(Extras::ignoreIframes),
                Codec.FLOAT.optionalFieldOf("homing_turn", 0.0f).forGetter(Extras::homingTurn),
                Codec.BOOL.optionalFieldOf("guided", false).forGetter(Extras::guided),
                Codec.INT.optionalFieldOf("lock_time", 20).forGetter(Extras::lockTime),
                Codec.INT.optionalFieldOf("lock_persist", 80).forGetter(Extras::lockPersist),
                Codec.DOUBLE.optionalFieldOf("lock_range", 150.0).forGetter(Extras::lockRange),
                Codec.INT.optionalFieldOf("radiation_ticks", 0).forGetter(Extras::radiationTicks),
                Identifier.CODEC.listOf().optionalFieldOf("extra_ammo", List.of()).forGetter(Extras::extraAmmo),
                Codec.BOOL.optionalFieldOf("continuous_beam", false).forGetter(Extras::continuousBeam)
        ).apply(inst, Extras::new));
    }

    public static final int CURRENT_SCHEMA = 1;

    public static final Codec<GunDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA).forGetter(GunDefinition::schemaVersion),
            Identifier.CODEC.fieldOf("ammo").forGetter(GunDefinition::ammo),
            Identifier.CODEC.optionalFieldOf("empty_magazine").forGetter(GunDefinition::emptyMagazine),
            Ballistic.CODEC.optionalFieldOf("ballistic", new Ballistic(
                    8.0f, 4, 10, 40, 40, 0.03f, 2.0f, 0.0, 1, 0.0f,
                    20.0f, 30.0f, 5.0f, 0.0f, false, true)).forGetter(GunDefinition::ballistic),
            SoundSet.CODEC.optionalFieldOf("sounds", new SoundSet(
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0))
                    .forGetter(GunDefinition::sounds),
            Handling.CODEC.optionalFieldOf("handling", new Handling(
                    "BALLISTIC", true, 0.8f, 0.5f, 0.8f, 0.3f, 0.7f, false, 0.0f, 1.0f, 0, 0, true))
                    .forGetter(GunDefinition::handling),
            Extras.CODEC.optionalFieldOf("extras", new Extras(
                    Optional.empty(), false, 0, 1, 1, 0.0f, false, 0.0f,
                    false, 20, 80, 150.0, 0, List.of(), false))
                    .forGetter(GunDefinition::extras)
    ).apply(inst, GunDefinition::new));

    // --- flat delegates (keep combat/call sites terse) ---
    public float damage() { return ballistic.damage(); }
    public int cooldownTicks() { return ballistic.cooldownTicks(); }
    public int magazine() { return ballistic.magazine(); }
    public int reloadTicks() { return ballistic.reloadTicks(); }
    public int rangeTtl() { return ballistic.rangeTtl(); }
    public float spread() { return ballistic.spread(); }
    public float speed() { return ballistic.speed(); }
    public double gravity() { return ballistic.gravity(); }
    public int pellets() { return ballistic.pellets(); }
    public float pelletSpread() { return ballistic.pelletSpread(); }
    public float dropStart() { return ballistic.dropStart(); }
    public float dropEnd() { return ballistic.dropEnd(); }
    public float dropMin() { return ballistic.dropMin(); }
    public float penetration() { return ballistic.penetration(); }
    public boolean semiAuto() { return ballistic.semiAuto(); }
    public boolean magazineFed() { return ballistic.magazineFed(); }
    public Optional<Identifier> fireSound() { return sounds.fire(); }
    public Optional<Identifier> reloadSound() { return sounds.reload(); }
    public Optional<Identifier> extraSound() { return sounds.extra(); }
    public Optional<Identifier> actionSound() { return sounds.action(); }
    public int actionDelayTicks() { return sounds.actionDelayTicks(); }
    public String projectile() { return handling.projectile(); }
    public boolean canZoom() { return handling.canZoom(); }
    public float zoomFov() { return handling.zoomFov(); }
    public float zoomSpreadMult() { return handling.zoomSpreadMult(); }
    public float recoilPitch() { return handling.recoilPitch(); }
    public float recoilYaw() { return handling.recoilYaw(); }
    public float muzzleScale() { return handling.muzzleScale(); }
    public boolean spinsBarrels() { return handling.spinsBarrels(); }
    public float slimePerHit() { return handling.slimePerHit(); }
    public float bulk() { return handling.bulk(); }
    public int muzzleFlash() { return handling.muzzleFlash(); }
    public int loopTicks() { return handling.loopTicks(); }
    public boolean twoHanded() { return handling.twoHanded(); }

    /** Basic sanity: returns error string or null when valid. */
    public String validate() {
        if (ballistic.magazine() <= 0) return "ballistic.magazine must be > 0";
        if (ballistic.cooldownTicks() < 1) return "ballistic.cooldown_ticks must be >= 1";
        if (ballistic.reloadTicks() < 1) return "ballistic.reload_ticks must be >= 1";
        if (ballistic.rangeTtl() < 1) return "ballistic.range_ttl must be >= 1";
        if (ballistic.pellets() < 1) return "ballistic.pellets must be >= 1";
        if (ballistic.spread() < 0 || ballistic.pelletSpread() < 0) return "spread must be >= 0";
        try {
            GunStats.ProjectileKind.valueOf(handling.projectile());
        } catch (Exception e) {
            return "unknown handling.projectile: " + handling.projectile();
        }
        return null;
    }

    private static Supplier<SoundEvent> soundSupplier(Optional<Identifier> id, Supplier<SoundEvent> fallback) {
        if (id.isEmpty()) return null;
        Identifier loc = id.get();
        return () -> {
            SoundEvent resolved = BuiltInRegistries.SOUND_EVENT.getValue(loc);
            if (resolved == null) {
                TechGuns3.LOGGER.warn("[TechGuns3] missing sound {}, falling back", loc);
                return fallback == null ? null : fallback.get();
            }
            return resolved;
        };
    }

    /** Convert to the combat-facing stats (sounds resolved live so /reload swaps them). */
    public GunStats toStats(Supplier<SoundEvent> fallbackFire, Supplier<SoundEvent> fallbackReload) {
        GunStats.ProjectileKind kind;
        try {
            kind = GunStats.ProjectileKind.valueOf(handling.projectile());
        } catch (Exception e) {
            kind = GunStats.ProjectileKind.BALLISTIC;
        }
        Supplier<SoundEvent> fire = soundSupplier(sounds.fire(), fallbackFire);
        Supplier<SoundEvent> reload = soundSupplier(sounds.reload(), fallbackReload);
        Supplier<SoundEvent> extra = sounds.extra().isEmpty() ? null : soundSupplier(sounds.extra(), null);
        Supplier<SoundEvent> action = sounds.action().isEmpty() ? null : soundSupplier(sounds.action(), null);
        if (fire == null) fire = fallbackFire;
        if (reload == null) reload = fallbackReload;
        final Supplier<SoundEvent> fFire = fire;
        final Supplier<SoundEvent> fReload = reload;
        Supplier<SoundEvent> safeFire = () -> {
            try {
                return fFire == null ? null : fFire.get();
            } catch (Exception ex) {
                return fallbackFire == null ? null : fallbackFire.get();
            }
        };
        Supplier<SoundEvent> safeReload = () -> {
            try {
                return fReload == null ? null : fReload.get();
            } catch (Exception ex) {
                return fallbackReload == null ? null : fallbackReload.get();
            }
        };
        var b = ballistic;
        var h = handling;
        return new GunStats(b.damage(), b.cooldownTicks(), b.magazine(), b.reloadTicks(), b.rangeTtl(),
                b.spread(), b.speed(), b.gravity(), b.pellets(), b.pelletSpread(),
                b.dropStart(), b.dropEnd(), b.dropMin(), b.penetration(), b.semiAuto(), b.magazineFed(),
                safeFire, safeReload, extra, kind,
                h.canZoom(), h.zoomFov(), h.zoomSpreadMult(), h.recoilPitch(), h.recoilYaw(),
                h.muzzleScale(), h.spinsBarrels(), h.slimePerHit(), action, sounds.actionDelayTicks(), h.bulk());
    }

    public GunExtras toExtras() {
        GunExtras base;
        if (extras.explosive().isPresent()) {
            var e = extras.explosive().get();
            base = e.nuke()
                    ? GunExtras.nuke(e.max(), e.min(), e.r1(), e.r2())
                    : GunExtras.explosive(e.max(), e.min(), e.r1(), e.r2(), e.block());
        } else {
            base = GunExtras.DEFAULT;
        }
        if (extras.chargeTicks() > 0) base = base.withCharge(extras.chargeTicks(), extras.ammoBase(), extras.ammoFull());
        if (extras.beam()) base = base.withBeam(extras.radiationTicks());
        else if (extras.radiationTicks() > 0) base = base.withRadiation(extras.radiationTicks());
        if (extras.knockback() != 0.0f || extras.ignoreIframes()) base = base.withKnockback(extras.knockback(), extras.ignoreIframes());
        if (extras.guided()) base = base.withGuided(extras.homingTurn() == 0.0f ? 0.16f : extras.homingTurn(),
                extras.lockTime(), extras.lockPersist(), extras.lockRange());
        else if (extras.homingTurn() != 0.0f) base = base.withHoming(extras.homingTurn());
        if (!extras.extraAmmo().isEmpty()) {
            String[] ids = extras.extraAmmo().stream().map(Identifier::getPath).toArray(String[]::new);
            base = base.withExtraAmmo(ids);
        }
        if (extras.explosive().isPresent() && extras.explosive().get().nuke() && extras.radiationTicks() > 0) {
            final int want = extras.radiationTicks();
            GunExtras.ExplosiveSpec spec = base.explosive();
            base = new GunExtras(spec, base.beam(), base.chargeTicks(), base.ammoBase(), base.ammoFull(),
                    base.knockback(), base.ignoreIframes(), base.homingTurn(), base.guided(),
                    base.lockTime(), base.lockPersist(), base.lockRange(), want, base.extraAmmoIds(),
                    base.muzzleFlash(), base.loopTicks(), base.continuousBeam());
        }
        if (handling.muzzleFlash() != GunExtras.FLASH_AUTO) base = base.withMuzzle(handling.muzzleFlash());
        if (handling.loopTicks() > 0) base = base.withLoop(handling.loopTicks());
        if (extras.continuousBeam()) base = base.withContinuous();
        return base;
    }

    /** Resolve the default ammo item (usually overridden by the Item supplier). */
    public Item resolveAmmoItem() {
        Item item = BuiltInRegistries.ITEM.getValue(ammo);
        return item == null ? Items.AIR : item;
    }

    // --- Java builtins (fallback when JSON is missing/broken; mirrors old TGItems numbers) ---

    private static Identifier idOrThrow(String raw) {
        Identifier parsed = Identifier.tryParse(raw);
        if (parsed == null) throw new IllegalArgumentException("Bad identifier: " + raw);
        return parsed;
    }

    public static GunDefinition builtin(String ammoPath, String emptyPath,
                                        float damage, int cooldown, int mag, int reload, int ttl,
                                        float spread, float speed, double gravity, int pellets, float pelletSpread,
                                        float dropStart, float dropEnd, float dropMin, float pen,
                                        boolean semi, boolean magFed,
                                        String fireSound, String reloadSound, String extraSound,
                                        String projectile,
                                        boolean canZoom, float zoomFov, float zoomSpread,
                                        float recoilPitch, float recoilYaw, float muzzle, boolean spin,
                                        float slime, String actionSound, int actionDelay, float bulk,
                                        ExplosiveDef explosive, boolean beam, int charge, int ammoBase, int ammoFull,
                                        float knockback, boolean ignoreIframes, float homing, boolean guided,
                                        int lockTime, int lockPersist, double lockRange, int radiation,
                                        List<String> extraAmmo, int flash, int loop, boolean continuous) {
        Identifier ammoId = idOrThrow(TechGuns3.MODID + ":" + ammoPath);
        Optional<Identifier> emptyId = emptyPath == null ? Optional.empty()
                : Optional.of(idOrThrow(TechGuns3.MODID + ":" + emptyPath));
        java.util.function.Function<String, Optional<Identifier>> snd = s ->
                s == null ? Optional.empty() : Optional.of(idOrThrow(TechGuns3.MODID + ":" + s));
        List<Identifier> extrasAmmo = new ArrayList<>();
        if (extraAmmo != null) for (String s : extraAmmo) extrasAmmo.add(idOrThrow(TechGuns3.MODID + ":" + s));
        return new GunDefinition(CURRENT_SCHEMA, ammoId, emptyId,
                new Ballistic(damage, cooldown, mag, reload, ttl, spread, speed, gravity,
                        pellets, pelletSpread, dropStart, dropEnd, dropMin, pen, semi, magFed),
                new SoundSet(snd.apply(fireSound), snd.apply(reloadSound), snd.apply(extraSound),
                        snd.apply(actionSound), actionDelay),
                new Handling(projectile, canZoom, zoomFov, zoomSpread, recoilPitch, recoilYaw,
                        muzzle, spin, slime, bulk, flash, loop, true),
                new Extras(Optional.ofNullable(explosive), beam, charge, ammoBase, ammoFull,
                        knockback, ignoreIframes, homing, guided, lockTime, lockPersist, lockRange,
                        radiation, extrasAmmo, continuous));
    }

    /** Copy with a different grip style (one-handed pistols vs two-handed long guns). */
    public GunDefinition withTwoHanded(boolean twoHanded) {
        var h = handling;
        return new GunDefinition(schemaVersion, ammo, emptyMagazine, ballistic, sounds,
                new Handling(h.projectile(), h.canZoom(), h.zoomFov(), h.zoomSpreadMult(),
                        h.recoilPitch(), h.recoilYaw(), h.muzzleScale(), h.spinsBarrels(),
                        h.slimePerHit(), h.bulk(), h.muzzleFlash(), h.loopTicks(), twoHanded),
                extras);
    }
}
