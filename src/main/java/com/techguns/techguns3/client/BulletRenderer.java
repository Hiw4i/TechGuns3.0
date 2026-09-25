package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.BulletProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;

/**
 * Custom tracer: white-hot core inside a warm glow, stretched along the
 * velocity. Crossed ribbon pairs read from any angle with no textures.
 */
public final class BulletRenderer extends EntityRenderer<BulletProjectile, BulletRenderer.State> {
    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public final Vector3f motion = new Vector3f(0, 0, -1);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletProjectile entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var motion = entity.getDeltaMovement();
        state.motion.set((float) motion.x, (float) motion.y, (float) motion.z);
        if (state.motion.lengthSquared() < 1e-6f) state.motion.set(0, 0, -1);
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        Vector3f dir = new Vector3f(state.motion).normalize();
        float speed = state.motion.length();
        float len = Math.min(1.3f, Math.max(0.45f, speed * 0.4f));

        Vector3f up = Math.abs(dir.y) > 0.94f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        float tx = -dir.x * len;
        float ty = -dir.y * len;
        float tz = -dir.z * len;

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            // Warm glow, crossed pair.
            FxGeometry.ribbon(matrix, consumer, 0, 0, 0, tx, ty, tz,
                    u.x, u.y, u.z, 0.11f, 1.0f, 0.5f, 0.12f, 0.55f);
            FxGeometry.ribbon(matrix, consumer, 0, 0, 0, tx, ty, tz,
                    w.x, w.y, w.z, 0.11f, 1.0f, 0.5f, 0.12f, 0.55f);
            // White-hot core, crossed pair.
            FxGeometry.ribbon(matrix, consumer, 0, 0, 0, tx, ty, tz,
                    u.x, u.y, u.z, 0.035f, 1.0f, 0.96f, 0.82f, 1.0f);
            FxGeometry.ribbon(matrix, consumer, 0, 0, 0, tx, ty, tz,
                    w.x, w.y, w.z, 0.035f, 1.0f, 0.96f, 0.82f, 1.0f);
        });
    }
}
