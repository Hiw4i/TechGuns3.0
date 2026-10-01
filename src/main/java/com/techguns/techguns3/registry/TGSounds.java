package com.techguns.techguns3.registry;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MVP wave-1 sounds, ported from 1.12 {@code techguns.TGSounds}.
 *
 * <p>1.12 mapping: {@code createSoundEvent("guns.pistolfire")} etc. became registry names;
 * on 26.3 the ID doubles as the {@code sounds.json} key. Only the seven wave-1 guns
 * (fire/reload + pump/rechamber where the original had them) plus the seven bullet
 * impact types are registered here. File names under {@code sounds/} are kept
 * byte-identical to the originals.</p>
 */
public final class TGSounds {
    private TGSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, TechGuns3.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(TechGuns3.MODID, id)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = sound("pistol_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_RELOAD = sound("pistol_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_FIRE = sound("revolver_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_RELOAD = sound("revolver_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> AK47_FIRE = sound("ak47_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> AK47_RELOAD = sound("ak47_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> M4_FIRE = sound("m4_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> M4_RELOAD = sound("m4_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> THOMPSON_FIRE = sound("thompson_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> THOMPSON_RELOAD = sound("thompson_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_FIRE = sound("combat_shotgun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_RELOAD = sound("combat_shotgun_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_PUMP = sound("combat_shotgun_pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FIRE = sound("minigun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_RELOAD = sound("minigun_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TESLAGUN_FIRE = sound("teslagun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TESLAGUN_RELOAD = sound("teslagun_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_FIRE = sound("biogun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_RELOAD = sound("biogun_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_FIRE = sound("flamethrower_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_RELOAD = sound("flamethrower_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_START = sound("flamethrower_start");

    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_FIRE = sound("bolt_action_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RELOAD = sound("bolt_action_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RECHAMBER = sound("bolt_action_rechamber");

    // --- wave-2 exotic guns (ported 1:1 from Techguns2-CE TGSounds) ---
    public static final DeferredHolder<SoundEvent, SoundEvent> TFG_FIRE = sound("tfg_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TFG_CHARGE = sound("tfg_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> TFG_EXPLOSION = sound("tfg_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> TFG_EXPLOSION_ECHO = sound("tfg_explosion_echo");

    public static final DeferredHolder<SoundEvent, SoundEvent> NDR_FIRE = sound("ndr_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> NDR_START = sound("ndr_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> NDR_RELOAD = sound("ndr_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_FIRE = sound("gauss_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RELOAD = sound("gauss_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RECHAMBER = sound("gauss_rechamber");

    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FIRE = sound("rocket_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_RELOAD = sound("rocket_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION = sound("explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_EXPLOSION = sound("nuke_explosion");

    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDED_FIRE = sound("guided_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCKON_BEEP = sound("lockon_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCKED_BEEP = sound("locked_beep");

    public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_FIRE = sound("sonic_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_RELOAD = sound("sonic_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_FIRE = sound("laser_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_RELOAD = sound("laser_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASERPISTOL_FIRE = sound("laserpistol_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASERPISTOL_RELOAD = sound("laserpistol_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_FIRE = sound("pulse_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RELOAD = sound("pulse_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE = sound("vector_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_RELOAD = sound("vector_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PDW_FIRE = sound("pdw_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PDW_RELOAD = sound("pdw_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> AS50_FIRE = sound("as50_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> AS50_RELOAD = sound("as50_reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_ROCK = sound("impact_rock");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_METAL = sound("impact_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_DIRT = sound("impact_dirt");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_WOOD = sound("impact_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_GLASS = sound("impact_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_BRICKS = sound("impact_bricks");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_WATER = sound("impact_water");

    /**
     * Wet flesh thud on bullet impact. Reuses the water/dirt layers until
     * dedicated gore recordings land under {@code sounds/effects/gore_*}.
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLESH_IMPACT = sound("flesh_impact");
    /** Meaty rip when a Techguns kill gibs. Same placeholder rule as above. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GORE_SPLIT = sound("gore_split");
}
