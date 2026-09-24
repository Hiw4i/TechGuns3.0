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

    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_FIRE = sound("bolt_action_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RELOAD = sound("bolt_action_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RECHAMBER = sound("bolt_action_rechamber");

    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_ROCK = sound("impact_rock");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_METAL = sound("impact_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_DIRT = sound("impact_dirt");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_WOOD = sound("impact_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_GLASS = sound("impact_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_BRICKS = sound("impact_bricks");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_WATER = sound("impact_water");
}
