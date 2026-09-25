package com.techguns.techguns3.item;

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

import java.util.function.Consumer;
import java.util.function.Supplier;

/** GeckoLib visual shell for animated Techguns firearms. Fire/reload/spin are
 * triggered server-side (synced animatable); spin runs while LMB is held on
 * guns whose {@code GunStats.spinsBarrels()} is true. */
public final class AnimatedGunItem extends GenericGunItem implements GeoItem {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedGunItem(Item.Properties properties, GunStats stats,
                           Supplier<Item> ammoItem, String ammoId) {
        super(properties, stats, ammoItem, ammoId);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AnimatedGunItem>("action", 0, state -> PlayState.STOP)
                .triggerableAnim("fire", RawAnimation.begin().thenPlay("fire"))
                .triggerableAnim("reload", RawAnimation.begin().thenPlay("reload")));
        controllers.add(new AnimationController<AnimatedGunItem>("barrels", 0, state -> PlayState.STOP)
                .triggerableAnim("spin", RawAnimation.begin().thenLoop("spin")));
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
                if (renderer == null) renderer = new GeoItemRenderer<>(AnimatedGunItem.this);
                return renderer;
            }
        });
    }
}
