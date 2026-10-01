package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.ShellCasingEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Brass casing: small elongated box tumbling by seeded spin + age.
 */
public final class ShellCasingRenderer extends EntityRenderer<ShellCasingEntity, ShellCasingRenderer.State> {
    public ShellCasingRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public float spin;
        public int age;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ShellCasingEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spin = entity.spin();
        state.age = entity.tickCount;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f()
                .rotationXYZ(state.age * 0.35f + state.spin, state.spin * 1.7f, 0.3f));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            // Brass body + darker tip.
            FxGeometry.box(matrix, consumer, 0.07f, 0.045f, 0.045f, 0.78f, 0.60f, 0.22f, 1.0f);
        });
        poseStack.popPose();
    }
}
