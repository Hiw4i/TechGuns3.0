package com.techguns.techguns3.client;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.item.AnimatedGunItem;

/**
 * View model renderer for Techguns firearms.
 *
 * <p>Adds everything that makes a held weapon feel like an object with mass
 * rather than a static prop:</p>
 * <ul>
 *   <li>the aim pose - the weapon comes up to the shoulder while aiming,</li>
 *   <li>the sprint pose - it drops out of the way while running,</li>
 *   <li>mouse sway and walk bob,</li>
 *   <li>the recoil spring that kicks the weapon back after a shot.</li>
 * </ul>
 *
 * <p>The muzzle flash is a separate render layer ({@link MuzzleFlashLayer}),
 * because it has to be drawn on the weapon model rather than in the world.</p>
 *
 * <p>All of it is client side and purely visual.</p>
 */
public final class TechGunsGeoItemRenderer extends GeoItemRenderer<AnimatedGunItem> {
    public TechGunsGeoItemRenderer(AnimatedGunItem item) {
        super(item);
        withRenderLayer(new MuzzleFlashLayer(this));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        // Keeps GeckoLib's own centring of the 0..1 block model.
        super.adjustRenderPose(renderPassInfo);
        if (!isFirstPerson(renderPassInfo)) {
            return;
        }
        PoseStack poseStack = renderPassInfo.poseStack();
        GunFeel feel = GunFeel.get();

        float aim = feel.aimProgress() * (float) TGConfig.adsPose();
        float sprint = feel.sprintProgress();
        float sway = (float) TGConfig.weaponSway();
        float bob = (float) TGConfig.weaponBob();

        // Aim: raise the weapon towards the sight line and pull it in.
        if (aim > 0.0f) {
            poseStack.translate(-0.22f * aim, 0.20f * aim, -0.30f * aim);
            poseStack.rotateDegrees(Axis.ZP, -4.0f * aim);
            poseStack.rotateDegrees(Axis.XP, -2.0f * aim);
        }

        // Sprint: the weapon drops and rolls inwards.
        if (sprint > 0.0f) {
            poseStack.rotateDegrees(Axis.XP, 26.0f * sprint);
            poseStack.rotateDegrees(Axis.YP, -16.0f * sprint);
            poseStack.translate(-0.08f * sprint, -0.30f * sprint, 0.20f * sprint);
        }

        // Mouse sway: the model keeps following the camera for a moment.
        if (sway > 0.0f) {
            poseStack.rotateDegrees(Axis.XP, (float) Math.toDegrees(feel.swayPitch()) * sway);
            poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(feel.swayYaw()) * sway);
        }

        // Walk bob: a slow figure of eight that fades out while aiming.
        if (bob > 0.0f) {
            float amount = feel.walkAmount() * bob;
            float phase = feel.bobPhase();
            poseStack.translate((float) Math.sin(phase) * 0.09f * amount,
                    (float) -Math.abs(Math.cos(phase)) * 0.07f * amount,
                    0.0f);
            poseStack.rotateDegrees(Axis.ZP, (float) Math.sin(phase) * 2.5f * amount);
        }

        // Recoil: the weapon is pushed back and the muzzle climbs.
        float kick = feel.recoilPitch();
        if (kick != 0.0f || feel.recoilYaw() != 0.0f) {
            poseStack.rotateDegrees(Axis.XP, (float) Math.toDegrees(kick));
            poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(feel.recoilYaw()));
            poseStack.translate(0.0f, 0.0f, kick * 0.55f);
        }
    }

    private static boolean isFirstPerson(RenderPassInfo<GeoRenderState> info) {
        var perspective = info.renderState().getGeckolibData(
                com.geckolib.constant.DataTickets.ITEM_RENDER_PERSPECTIVE);
        return perspective != null && perspective.firstPerson();
    }
}
