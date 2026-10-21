package com.techguns.techguns3.item;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.techguns.techguns3.client.TechGunsGeoItemRenderer;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * GeckoLib visual shell for animated Techguns firearms.
 *
 * <p>Fire, reload, charge and the barrel spin live in separate controllers so
 * they never fight over the same bones and each one can use the transition
 * length it needs:</p>
 * <ul>
 *   <li>{@code fire} - no transition at all. A weapon that is being fired has
 *       to snap: a blend would smear the kick of an automatic into the next
 *       shot.</li>
 *   <li>{@code reload} - a short blend, so the weapon eases into the reload
 *       pose instead of popping into it.</li>
 *   <li>{@code charge} - additive, so the charging tilt is added on top of
 *       whatever the weapon is doing instead of replacing it.</li>
 *   <li>{@code barrels} - the spinning barrel cluster of the minigun.</li>
 * </ul>
 *
 * <p>Stats come from the datapack (see {@link GenericGunItem}); this class
 * only adds animation.</p>
 */
public final class AnimatedGunItem extends GenericGunItem implements GeoItem {
    public static final String CONTROLLER_FIRE = "fire";
    public static final String CONTROLLER_RELOAD = "reload";
    public static final String CONTROLLER_CHARGE = "charge";
    public static final String CONTROLLER_BARRELS = "barrels";
    public static final String CONTROLLER_DRAW = "draw";
    public static final String CONTROLLER_INSPECT = "inspect";
    public static final String CONTROLLER_DRYFIRE = "dryfire";

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedGunItem(Item.Properties properties, Identifier gunId,
                            Supplier<Item> ammoItem, String ammoId,
                            Supplier<SoundEvent> fallbackFireSound,
                            Supplier<SoundEvent> fallbackReloadSound) {
        super(properties, gunId, ammoItem, ammoId, fallbackFireSound, fallbackReloadSound);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_FIRE, 0, state -> PlayState.STOP)
                .triggerableAnim("fire", RawAnimation.begin().thenPlay("fire")));
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_RELOAD, 4, state -> PlayState.STOP)
                .triggerableAnim("reload", RawAnimation.begin().thenPlay("reload")));
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_BARRELS, 0, state -> PlayState.STOP)
                .triggerableAnim("spin", RawAnimation.begin().thenLoop("spin")));
        // The tracks below are additive: they are laid on top of whatever the
        // weapon is doing instead of replacing it. A weapon that is drawn
        // while it is fired still kicks, and a weapon that is looked over
        // still recoils.
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_CHARGE, 4, state -> PlayState.STOP)
                .additiveAnimations()
                // Plays once and holds the last frame: a looping charge track
                // would snap back to the start pose every few seconds.
                .triggerableAnim("charge", RawAnimation.begin().thenPlayAndHold("charge")));
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_DRAW, 0, state -> PlayState.STOP)
                .additiveAnimations()
                .triggerableAnim("draw", RawAnimation.begin().thenPlay("draw")));
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_INSPECT, 4, state -> PlayState.STOP)
                .additiveAnimations()
                .triggerableAnim("inspect", RawAnimation.begin().thenPlay("inspect")));
        controllers.add(new AnimationController<AnimatedGunItem>(CONTROLLER_DRYFIRE, 0, state -> PlayState.STOP)
                .additiveAnimations()
                .triggerableAnim("dryfire", RawAnimation.begin().thenPlay("dryfire")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<AnimatedGunItem> renderer;

            @Override
            public @Nullable GeoItemRenderer<AnimatedGunItem> getGeoItemRenderer() {
                if (renderer == null) renderer = new TechGunsGeoItemRenderer(AnimatedGunItem.this);
                return renderer;
            }
        });
    }
}
