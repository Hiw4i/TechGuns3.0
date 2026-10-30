package com.techguns.techguns3.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.techguns.techguns3.item.AnimatedGunItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.function.BiConsumer;

/**
 * Draws the first-person muzzle flash on the weapon model.
 *
 * <p>The flash is attached to the {@code muzzle} anchor bone, so it inherits
 * the whole animated transform of the weapon: it travels with the barrel
 * while the gun recoils and it is never left behind in the world.</p>
 */
public final class MuzzleFlashLayer extends GeoRenderLayer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> {
    public MuzzleFlashLayer(com.geckolib.renderer.base.GeoRenderer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                 BiConsumer<GeoBone, PerBoneRender<GeoRenderState>> consumer) {
        super.addPerBoneRender(renderPassInfo, consumer);
        if (!isFirstPerson(renderPassInfo) || !MuzzleFlashFx.isActive()) {
            return;
        }
        renderPassInfo.model().getBone("muzzle").ifPresent(bone ->
                consumer.accept(bone, (info, muzzleBone, tasks) -> {
                    PoseStack poseStack = info.poseStack();
                    poseStack.pushPose();
                    MuzzleFlashFx.submit(poseStack, tasks, MuzzleFlashFx.size());
                    poseStack.popPose();
                }));
    }

    private static boolean isFirstPerson(RenderPassInfo<GeoRenderState> info) {
        return TechGunsGeoItemRenderer.isFirstPerson(info);
    }
}
