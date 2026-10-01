package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.BlastEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Explosion billboard on 3 crossed planes (explosion4x4_02.png, 16 frames):
 * white-hot core for the first ticks, tinted fireball blooming to full blast
 * radius, fading out. Additive glow, seeded roll so stacked blasts differ.
 */
public final class BlastRenderer extends EntityRenderer<BlastEntity, BlastRenderer.State> {
    private static final Identifier BLAST_TEXTURE = Identifier.fromNamespaceAndPath(
            TechGuns3.MODID, "textures/fx/explosion4x4_02.png");

    public BlastRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public float radius = 4.0f;
        public int tint = 0xFFB43A;
        public int seed;
        public float progress;
        public java.util.UUID uuid;
        public double wx;
        public double wy;
        public double wz;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlastEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.radius = entity.radius();
        state.tint = entity.tint();
        state.seed = entity.seed();
        state.progress = entity.progress();
        state.uuid = entity.getUUID();
        var pos = entity.position();
        state.wx = pos.x;
        state.wy = pos.y;
        state.wz = pos.z;
        if (state.uuid != null) {
            TGShake.onBlastSeen(state.uuid, state.wx, state.wy, state.wz, state.radius);
        }
    }

    @Override
    protected boolean affectedByCulling(BlastEntity entity) {
        return false;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float prog = Math.max(0.0f, Math.min(1.0f, state.progress));
        // Fast bloom, slow fade: 60% size in the first quarter.
        float grow = (float) (1.0 - Math.pow(1.0 - Math.min(1.0f, prog * 2.0f), 2.0));
        float size = Math.max(0.3f, state.radius * (0.25f + 0.75f * grow));
        float alpha = prog < 0.15f ? prog / 0.15f : 1.0f - (prog - 0.15f) / 0.85f;
        if (alpha <= 0.02f) return;
        int frame = Math.min(15, (int) (prog * 16.0f));
        float u0 = (frame % 4) / 4.0f;
        float v0 = (frame / 4) / 4.0f;
        float u1 = u0 + 0.25f;
        float v1 = v0 + 0.25f;
        float r = ((state.tint >> 16) & 0xFF) / 255.0f;
        float g = ((state.tint >> 8) & 0xFF) / 255.0f;
        float b = (state.tint & 0xFF) / 255.0f;
        Random rand = new Random(state.seed);
        float roll = rand.nextFloat() * 3.14159f;
        final float fs = size;
        final float fa = alpha;
        final float c = (float) Math.cos(roll);
        final float s = (float) Math.sin(roll);
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(BLAST_TEXTURE), (pose, consumer) -> {
            var matrix = pose.pose();
            // Rolled main quad + two crossed planes for volume.
            float x1 = -fs * c + fs * s, y1 = -fs * s - fs * c;
            float x2 = fs * c + fs * s, y2 = fs * s - fs * c;
            float x3 = fs * c - fs * s, y3 = fs * s + fs * c;
            float x4 = -fs * c - fs * s, y4 = -fs * s + fs * c;
            FxGeometry.uvQuad(matrix, consumer,
                    x1, y1, 0, x2, y2, 0, x3, y3, 0, x4, y4, 0,
                    u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, fa, FxGeometry.FULLBRIGHT);
            FxGeometry.uvQuad(matrix, consumer,
                    0, -fs, -fs, 0, -fs, fs, 0, fs, fs, 0, fs, -fs,
                    u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, fa * 0.7f, FxGeometry.FULLBRIGHT);
            // White-hot heart while young.
            if (prog < 0.35f) {
                float core = fs * 0.45f;
                float ca = Math.min(1.0f, fa * 1.2f);
                FxGeometry.uvQuad(matrix, consumer,
                        -core, -core, 0, core, -core, 0, core, core, 0, -core, core, 0,
                        u0, v0, u1, v0, u1, v1, u0, v1, 1, 1, 1, ca, FxGeometry.FULLBRIGHT);
            }
        });
    }
}
