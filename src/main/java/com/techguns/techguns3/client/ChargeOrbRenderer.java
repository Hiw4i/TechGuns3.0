package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.ChargeOrbEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * TFG charge orb: camera-facing tfg_flare billboard, frame advancing with
 * charge, size blooming 0.25→1.25m, white-hot core when nearly full.
 */
public final class ChargeOrbRenderer extends EntityRenderer<ChargeOrbEntity, ChargeOrbRenderer.State> {
    private static final Identifier TFG_FLARE = Identifier.fromNamespaceAndPath(
            TechGuns3.MODID, "textures/fx/tfg_flare.png");

    public ChargeOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public float charge;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChargeOrbEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.charge = Math.max(0.0f, Math.min(1.0f, entity.charge()));
    }

    @Override
    protected boolean affectedByCulling(ChargeOrbEntity entity) {
        return false;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float charge = state.charge;
        if (charge <= 0.01f) return;
        int frame = Math.min(15, (int) (charge * 16.0f));
        float u0 = (frame % 4) / 4.0f;
        float v0 = (frame / 4) / 4.0f;
        float size = 0.25f + 1.0f * charge;
        float pulse = 1.0f + 0.06f * (float) Math.sin(System.nanoTime() / 90_000_000.0);
        final float fs = size * pulse;
        final float fa = 0.65f + 0.35f * charge;
        final float[] axes = FxGeometry.cameraAxes();
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(TFG_FLARE), (pose, consumer) -> {
            var matrix = pose.pose();
            FxGeometry.cameraBillboard(matrix, consumer, 0, 0, 0,
                    axes[0], axes[1], axes[2], axes[3], axes[4], axes[5],
                    fs, u0, v0, u0 + 0.25f, v0 + 0.25f,
                    0.45f, 1.0f, 0.45f, fa, FxGeometry.FULLBRIGHT);
            if (charge > 0.75f) {
                float core = fs * 0.4f;
                FxGeometry.cameraBillboard(matrix, consumer, 0, 0, 0,
                        axes[0], axes[1], axes[2], axes[3], axes[4], axes[5],
                        core, u0, v0, u0 + 0.25f, v0 + 0.25f,
                        1, 1, 1, Math.min(1.0f, fa + 0.2f), FxGeometry.FULLBRIGHT);
            }
        });
    }
}
