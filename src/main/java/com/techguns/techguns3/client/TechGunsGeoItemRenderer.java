package com.techguns.techguns3.client;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
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
    /** Guns at or below this zoom FOV count as scoped (single source for HUD overlays). */
    public static final float SCOPE_ZOOM_FOV = 0.5f;

    private final AnimatedGunItem gunItem;
    /** Scratch vector for the position of the sight, the renderer runs on one thread. */
    private final org.joml.Vector4f scratch = new org.joml.Vector4f();

    public TechGunsGeoItemRenderer(AnimatedGunItem item) {
        super(item);
        this.gunItem = item;
        withRenderLayer(new MuzzleFlashLayer(this));
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector tasks) {
        super.preRenderPass(renderPassInfo, tasks);
        ItemDisplayContext perspective = perspective(renderPassInfo);
        if (perspective == null || !perspective.firstPerson()) {
            return;
        }
        // Only the hip pose is measured: while aiming the weapon is already
        // moved, so its sight would report the aimed position instead. An
        // active recoil kick is skipped for the same reason - the measured
        // offset must be the rest pose, or aiming slides the weapon by the
        // kicked position and the shot throws the sights off.
        if (GunFeel.get().aimProgress() > 0.02f || GunFeel.get().recoilPitch() != 0.0f) {
            return;
        }
        // The rear sight. The position the listener reports is relative to the
        // model, the alignment needs the position on the screen, so it is put
        // through the matrix of the render pass.
        renderPassInfo.addBonePositionListener("aim", (world, model, local) -> {
            if (local == null) {
                return;
            }
            // The position of the bone in the space of the render pass, that is
            // the space the weapon is placed in.
            var view = scratch.set(local.x, local.y, local.z, 1.0f)
                    .mul(renderPassInfo.getPreRenderMatrixState());
            GunSightAlignment.recordAim(gunItem, perspective, view.x, view.y);
            // The measured position of the sight, once per weapon and hand.
            GunAimHandler.reportSight(gunItem, perspective, view.x, view.y);
        });
    }

    /**
     * A scoped weapon is not drawn while the player looks through the scope.
     *
     * <p>The scope mask of the screen covers everything outside of the lens,
     * but the tube of the scope itself would still stand in the middle of it.
     * The weapon is therefore hidden once the scope is fully up.</p>
     */
    @Override
    public boolean firePreRenderEvent(RenderPassInfo<GeoRenderState> renderPassInfo,
                                      SubmitNodeCollector renderTasks) {
        if (!super.firePreRenderEvent(renderPassInfo, renderTasks)) {
            return false;
        }
        if (!isFirstPerson(renderPassInfo)) {
            return true;
        }
        var def = gunItem.def();
        boolean scoped = def.canZoom() && def.zoomFov() <= SCOPE_ZOOM_FOV;
        return !(scoped && GunFeel.get().aimProgress() > 0.9f);
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

        float sprint = feel.sprintProgress();
        float sway = (float) TGConfig.weaponSway();
        float bob = (float) TGConfig.weaponBob();
        // The aim pose itself (sliding the weapon so the sights line up with
        // the crosshair) is applied in camera space by GunAimHandler, this
        // is only what happens around it.

        // Sprint: the weapon drops and rolls inwards.
        if (sprint > 0.0f) {
            poseStack.rotateDegrees(Axis.XP, 26.0f * sprint);
            poseStack.rotateDegrees(Axis.YP, -16.0f * sprint);
            poseStack.translate(-0.08f * sprint, -0.30f * sprint, 0.20f * sprint);
        }

        // Mouse sway: the model keeps following the camera for a moment.
        // Aiming steadies the weapon, otherwise the sights would wander off
        // the crosshair.
        if (sway > 0.0f) {
            float steady = 1.0f - 0.7f * feel.aimProgress();
            poseStack.rotateDegrees(Axis.XP, (float) Math.toDegrees(feel.swayPitch()) * sway * steady);
            poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(feel.swayYaw()) * sway * steady);
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

        // Recoil: the muzzle climbs through the burst (the kick stack), while
        // the push toward the shoulder stays bounded per shot (the spring).
        float kick = feel.recoilPitch();
        if (kick != 0.0f || feel.recoilYaw() != 0.0f || feel.recoilPush() != 0.0f) {
            poseStack.rotateDegrees(Axis.XP, (float) Math.toDegrees(kick));
            poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(feel.recoilYaw()));
            poseStack.translate(0.0f, 0.0f, feel.recoilPush() * 0.55f);
        }
    }

    /** True inside the first-person hand pass (the view model). */
    public static boolean isFirstPerson(RenderPassInfo<GeoRenderState> info) {
        ItemDisplayContext perspective = perspective(info);
        return perspective != null && perspective.firstPerson();
    }

    private static ItemDisplayContext perspective(RenderPassInfo<GeoRenderState> info) {
        return info.renderState().getGeckolibData(
                com.geckolib.constant.DataTickets.ITEM_RENDER_PERSPECTIVE);
    }
}
