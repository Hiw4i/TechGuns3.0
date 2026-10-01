package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.fx.BoltGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Fractal tesla bolt: two deterministic midpoint-displacement channels, white
 * core inside a compact cyan halo (layered fake bloom). Widths stay thin so a
 * 60-block miss never reads as a giant beam, and a close-range guard fades the
 * halo when the camera sits inside the arc start.
 */
public final class TeslaArcRenderer extends EntityRenderer<TeslaArcEntity, TeslaArcRenderer.State> {
    public TeslaArcRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public final Vector3f end = new Vector3f();
        public int seed;
        public double sx;
        public double sy;
        public double sz;
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
        var pos = entity.position();
        state.sx = pos.x;
        state.sy = pos.y;
        state.sz = pos.z;
    }

    @Override
    protected boolean affectedByCulling(TeslaArcEntity entity) {
        // Long miss arcs span the view; keep them drawn even when the origin
        // leaves the frustum. Thin widths + close fade keep them readable.
        return false;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float len = state.end.length();
        if (len < 0.05f) return;
        BoltGeometry.Bolt bolt = BoltGeometry.tesla(state.seed, 0, 0, 0, state.end.x, state.end.y, state.end.z);

        Vector3f dir = new Vector3f(state.end).normalize();
        Vector3f up = Math.abs(dir.y) > 0.94f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(dir).cross(u).normalize();

        // Parallel channel offsets, seeded so they match on every client.
        Random chanRand = new Random(state.seed ^ 0x5EED1234L);
        float[][] channelOff = new float[2][3];
        for (int c = 1; c < 2; c++) {
            float ang = chanRand.nextFloat() * (float) (Math.PI * 2);
            float mag = 0.04f + 0.03f * chanRand.nextFloat();
            channelOff[c][0] = (u.x * (float) Math.cos(ang) + w.x * (float) Math.sin(ang)) * mag;
            channelOff[c][1] = (u.y * (float) Math.cos(ang) + w.y * (float) Math.sin(ang)) * mag;
            channelOff[c][2] = (u.z * (float) Math.cos(ang) + w.z * (float) Math.sin(ang)) * mag;
        }

        float closeFade = closeFade(state);
        if (closeFade <= 0.02f) return;
        // Halo shrinks with distance fade; core stays crisp.
        float haloScale = 0.55f + 0.45f * closeFade;

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            var matrix = pose.pose();
            for (int c = 0; c < 2; c++) {
                float[] main = FxGeometry.translated(bolt.main(),
                        channelOff[c][0], channelOff[c][1], channelOff[c][2]);
                boolean primary = c == 0;
                if (primary) {
                    FxGeometry.polyline(matrix, consumer, main,
                            u.x, u.y, u.z, w.x, w.y, w.z,
                            0.06f * haloScale, 0.20f, 0.50f, 1.0f, 0.26f * closeFade);
                }
                FxGeometry.polyline(matrix, consumer, main,
                        u.x, u.y, u.z, w.x, w.y, w.z,
                        (primary ? 0.04f : 0.03f) * haloScale, 0.30f, 0.72f, 1.0f,
                        (primary ? 0.55f : 0.40f) * closeFade);
                FxGeometry.polyline(matrix, consumer, main,
                        u.x, u.y, u.z, w.x, w.y, w.z,
                        primary ? 0.02f : 0.016f, 0.93f, 1.0f, 1.0f, Math.min(1.0f, closeFade + 0.2f));
            }
            for (float[] branch : bolt.branches()) {
                FxGeometry.polyline(matrix, consumer, branch,
                        u.x, u.y, u.z, w.x, w.y, w.z,
                        0.022f * haloScale, 0.35f, 0.75f, 1.0f, 0.40f * closeFade);
                FxGeometry.polyline(matrix, consumer, branch,
                        u.x, u.y, u.z, w.x, w.y, w.z,
                        0.012f, 0.90f, 1.0f, 1.0f, Math.min(1.0f, closeFade + 0.2f));
            }
        });
    }

    /** 1 at range, ->0.25 when the viewer stands at the arc origin. */
    private static float closeFade(State state) {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return 1.0f;
            var cam = mc.player.getEyePosition();
            double dx = cam.x - state.sx;
            double dy = cam.y - state.sy;
            double dz = cam.z - state.sz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= 2.0) return 1.0f;
            if (dist <= 0.4) return 0.25f;
            return (float) (0.25 + (dist - 0.4) / (2.0 - 0.4) * 0.75);
        } catch (Exception e) {
            return 1.0f;
        }
    }
}
