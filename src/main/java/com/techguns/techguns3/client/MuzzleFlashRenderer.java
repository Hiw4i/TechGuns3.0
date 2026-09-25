package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Muzzle/impact star: three crossed quads, colored core inside a tinted shell.
 */
public final class MuzzleFlashRenderer extends EntityRenderer<MuzzleFlashEntity, MuzzleFlashRenderer.State> {
    public MuzzleFlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public int color = MuzzleFlashEntity.WHITE;
        public float scale = 0.5f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MuzzleFlashEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.color = entity.color();
        state.scale = entity.scale();
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float r = ((state.color >> 16) & 0xFF) / 255.0f;
        float g = ((state.color >> 8) & 0xFF) / 255.0f;
        float b = (state.color & 0xFF) / 255.0f;
        float s = state.scale;
        float core = s * 0.45f;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            // Tinted shell on all three planes.
            FxGeometry.quad(matrix, consumer,
                    -s, -s, 0, s, -s, 0, s, s, 0, -s, s, 0, r, g, b, 0.85f);
            FxGeometry.quad(matrix, consumer,
                    0, -s, -s, 0, -s, s, 0, s, s, 0, s, -s, r, g, b, 0.85f);
            FxGeometry.quad(matrix, consumer,
                    -s, 0, -s, s, 0, -s, s, 0, s, -s, 0, s, r, g, b, 0.85f);
            // White-hot heart.
            FxGeometry.quad(matrix, consumer,
                    -core, -core, 0, core, -core, 0, core, core, 0, -core, core, 0,
                    1, 1, 1, 1);
            FxGeometry.quad(matrix, consumer,
                    0, -core, -core, 0, -core, core, 0, core, core, 0, core, -core,
                    1, 1, 1, 1);
        });
    }
}
