package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.TeslaArcEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Tesla arc link: cyan outer crackle around a white core, rebuilt
 * deterministically from the synced seed so every viewer sees the same bolt.
 */
public final class TeslaArcRenderer extends EntityRenderer<TeslaArcEntity, TeslaArcRenderer.State> {
    private static final int SEGMENTS = 14;

    public TeslaArcRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public final Vector3f end = new Vector3f();
        public int seed;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TeslaArcEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.end.set(entity.endOffset());
        state.seed = entity.seed();
    }

    @Override
    protected boolean affectedByCulling(TeslaArcEntity entity) {
        return false;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float len = state.end.length();
        if (len < 0.05f) return;
        float amp = Math.min(0.4f, len * 0.06f);

        Vector3f dir = new Vector3f(state.end).normalize();
        Vector3f up = Math.abs(dir.y) > 0.94f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        float[] px = new float[SEGMENTS + 1];
        float[] py = new float[SEGMENTS + 1];
        float[] pz = new float[SEGMENTS + 1];
        Random rand = new Random(state.seed);
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float jx = 0;
            float jy = 0;
            float jz = 0;
            if (i > 0 && i < SEGMENTS) {
                float ou = (rand.nextFloat() * 2 - 1) * amp;
                float ow = (rand.nextFloat() * 2 - 1) * amp;
                jx = u.x * ou + w.x * ow;
                jy = u.y * ou + w.y * ow;
                jz = u.z * ou + w.z * ow;
            }
            px[i] = state.end.x * t + jx;
            py[i] = state.end.y * t + jy;
            pz[i] = state.end.z * t + jz;
        }

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            for (int pass = 0; pass < 2; pass++) {
                boolean core = pass == 1;
                float half = core ? 0.035f : 0.10f;
                float r = core ? 0.92f : 0.25f;
                float g = core ? 1.0f : 0.70f;
                float b = core ? 1.0f : 1.0f;
                float a = core ? 1.0f : 0.55f;
                for (int i = 0; i < SEGMENTS; i++) {
                    FxGeometry.ribbon(matrix, consumer,
                            px[i], py[i], pz[i], px[i + 1], py[i + 1], pz[i + 1],
                            u.x, u.y, u.z, half, r, g, b, a);
                    FxGeometry.ribbon(matrix, consumer,
                            px[i], py[i], pz[i], px[i + 1], py[i + 1], pz[i + 1],
                            w.x, w.y, w.z, half, r, g, b, a);
                }
            }
        });
    }
}
