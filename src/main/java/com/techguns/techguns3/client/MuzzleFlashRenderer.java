package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.client.fx.FxRenderTypes;
import com.techguns.techguns3.client.fx.MuzzleFlashArt;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Baked TG2-style muzzle/impact star: a textured star sprite
 * ({@code textures/fx/muzzle_flash.png}) on crossed planes, additive-feel
 * fullbright via the {@code eyes} pipeline, seeded roll + arm jitter so every
 * shot looks different, pop scale + fade alpha over
 * {@link MuzzleFlashEntity#MAX_LIFE} ticks, and a first-person guard so a
 * flash at the barrel tip never fills the screen.
 */
public final class MuzzleFlashRenderer extends EntityRenderer<MuzzleFlashEntity, MuzzleFlashRenderer.State> {
    private static final Identifier FLASH_TEXTURE = Identifier.fromNamespaceAndPath(
            TechGuns3.MODID, "textures/fx/muzzle_flash.png");

    /**
     * CE muzzle art (IScreenEffect 1:1): texture path, grid divisions and used
     * frames. Rendered additively as crossed quads, frame advancing over life.
     */
    private static final java.util.Map<Integer, MuzzleFlashArt.Art> FLASH_ART = java.util.Map.ofEntries(
            java.util.Map.entry(1, MuzzleFlashArt.art(1)),
            java.util.Map.entry(2, MuzzleFlashArt.art(2)),
            java.util.Map.entry(3, MuzzleFlashArt.art(3)),
            java.util.Map.entry(4, MuzzleFlashArt.art(4)),
            java.util.Map.entry(5, MuzzleFlashArt.art(5)),
            java.util.Map.entry(6, MuzzleFlashArt.art(6)),
            java.util.Map.entry(7, MuzzleFlashArt.art(7)),
            java.util.Map.entry(8, MuzzleFlashArt.art(8)),
            java.util.Map.entry(9, MuzzleFlashArt.art(9)),
            java.util.Map.entry(10, MuzzleFlashArt.art(10)),
            java.util.Map.entry(11, MuzzleFlashArt.art(11)));

    public MuzzleFlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public int color = MuzzleFlashEntity.WHITE;
        public float scale = 0.5f;
        public int seed;
        public float fade = 1.0f;
        public int texture;
        public double wx;
        public double wy;
        public double wz;
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
        state.seed = entity.seed();
        state.texture = entity.texture();
        // Interpolate the pop between ticks so slow clients still see a decay.
        float life = entity.lifeLeft() - partialTick;
        state.fade = Math.max(0.0f, Math.min(1.0f, life / MuzzleFlashEntity.MAX_LIFE));
        var pos = entity.position();
        state.wx = pos.x;
        state.wy = pos.y;
        state.wz = pos.z;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fade <= 0.01f) return;
        MuzzleFlashArt.Art art = FLASH_ART.get(state.texture);
        if (art != null) {
            submitTextured(state, poseStack, collector, art);
            return;
        }
        submitStar(state, poseStack, collector, camera);
    }

    /**
     * CE muzzle art: full-frame quad on 3 crossed planes, frame advancing with
     * age (ScreenEffect behaviour), tinted by the preset color, additive glow.
     */
    private static void submitTextured(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                       SubmitNodeCollector collector, MuzzleFlashArt.Art art) {
        float age = MuzzleFlashEntity.MAX_LIFE * (1.0f - state.fade);
        int frame = Math.min(art.frames() - 1, (int) (age / MuzzleFlashEntity.MAX_LIFE * art.frames()));
        int grid = art.grid();
        float u0 = (frame % grid) / (float) grid;
        float v0 = (frame / grid) / (float) grid;
        float u1 = u0 + 1.0f / grid;
        float v1 = v0 + 1.0f / grid;
        float r = ((state.color >> 16) & 0xFF) / 255.0f;
        float g = ((state.color >> 8) & 0xFF) / 255.0f;
        float b = (state.color & 0xFF) / 255.0f;
        // Pop hard on tick 0, ease out; CE flashes are big (scale ≈ meters).
        float s = state.scale * (0.75f + 0.25f * state.fade);
        float alpha = Math.min(1.0f, state.fade * 1.4f);
        float guard = firstPersonGuard(state);
        if (guard < 1.0f) {
            s *= (0.45f + 0.55f * guard);
            alpha *= guard;
            if (alpha < 0.02f) return;
        }
        final float fs = s;
        final float fa = alpha;
        collector.submitCustomGeometry(poseStack, FxRenderTypes.additive(art.texture()), (pose, consumer) -> {
            var matrix = pose.pose();
            // Three crossed planes so the art reads volumetrically in motion.
            FxGeometry.uvQuad(matrix, consumer,
                    -fs, -fs, 0, fs, -fs, 0, fs, fs, 0, -fs, fs, 0,
                    u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, fa, FxGeometry.FULLBRIGHT);
            FxGeometry.uvQuad(matrix, consumer,
                    0, -fs, -fs, 0, -fs, fs, 0, fs, fs, 0, fs, -fs,
                    u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, fa * 0.85f, FxGeometry.FULLBRIGHT);
            FxGeometry.uvQuad(matrix, consumer,
                    -fs, 0, -fs, fs, 0, -fs, fs, 0, fs, -fs, 0, fs,
                    u0, v0, u1, v0, u1, v1, u0, v1, r, g, b, fa * 0.7f, FxGeometry.FULLBRIGHT);
        });
    }

    private void submitStar(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                            SubmitNodeCollector collector, CameraRenderState camera) {
        float r = ((state.color >> 16) & 0xFF) / 255.0f;
        float g = ((state.color >> 8) & 0xFF) / 255.0f;
        float b = (state.color & 0xFF) / 255.0f;

        // Seeded TG2 jitter: roll + per-arm length so no two shots match.
        Random rand = new Random(state.seed);
        float roll = rand.nextFloat() * (float) (Math.PI * 2);
        float armJitter = 0.82f + 0.36f * rand.nextFloat();
        float armJitter2 = 0.82f + 0.36f * rand.nextFloat();

        // Pop: full size on tick 0, shrink as it fades.
        float s = state.scale * armJitter * (0.55f + 0.45f * state.fade);
        float s2 = state.scale * armJitter2 * (0.55f + 0.45f * state.fade);
        float core = s * 0.45f;

        float alpha = state.fade * state.fade;
        float coreAlpha = Math.min(1.0f, state.fade * 1.2f);

        // First-person guard: camera inside/near the flash -> shrink + fade.
        float guard = firstPersonGuard(state);
        if (guard < 1.0f) {
            s *= (0.45f + 0.55f * guard);
            s2 *= (0.45f + 0.55f * guard);
            core *= (0.45f + 0.55f * guard);
            alpha *= guard;
            coreAlpha *= guard;
            if (alpha < 0.02f) return;
        }

        final float fs = s;
        final float fs2 = s2;
        final float fcore = core;
        final float fa = alpha;
        final float fca = coreAlpha;
        final float cos = (float) Math.cos(roll);
        final float sin = (float) Math.sin(roll);
        collector.submitCustomGeometry(poseStack, FxRenderTypes.additive(FLASH_TEXTURE), (pose, consumer) -> {
            var matrix = pose.pose();
            // Rolled star (XY) + two crossed planes sharing the same sprite.
            starRolled(matrix, consumer, fs, r, g, b, fa, cos, sin);
            FxGeometry.uvQuad(matrix, consumer,
                    0, -fs2, -fs2, 0, -fs2, fs2, 0, fs2, fs2, 0, fs2, -fs2,
                    0, 0, 1, 0, 1, 1, 0, 1, r, g, b, fa, FxGeometry.FULLBRIGHT);
            FxGeometry.uvQuad(matrix, consumer,
                    -fs2, 0, -fs2, fs2, 0, -fs2, fs2, 0, fs2, -fs2, 0, fs2,
                    0, 0, 1, 0, 1, 1, 0, 1, r, g, b, fa * 0.8f, FxGeometry.FULLBRIGHT);
            // White-hot heart: smaller star so the tinted arms read around it.
            starRolled(matrix, consumer, fcore, 1, 1, 1, fca, cos, sin);
        });
    }

    private static void starRolled(org.joml.Matrix4fc pose,
                                   com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                   float s, float r, float g, float b, float a,
                                   float cos, float sin) {
        float x1 = -s * cos + s * sin;
        float y1 = -s * sin - s * cos;
        float x2 = s * cos + s * sin;
        float y2 = s * sin - s * cos;
        float x3 = s * cos - s * sin;
        float y3 = s * sin + s * cos;
        float x4 = -s * cos - s * sin;
        float y4 = -s * sin + s * cos;
        FxGeometry.uvQuad(pose, consumer,
                x1, y1, 0, x2, y2, 0, x3, y3, 0, x4, y4, 0,
                0, 0, 1, 0, 1, 1, 0, 1, r, g, b, a, FxGeometry.FULLBRIGHT);
    }

    /**
     * 1 far away, 0 when the viewer stands inside the flash.
     *
     * <p>A weapon with a 3D model draws its own flash on the barrel, so the
     * world flash of the shooter is hidden completely - it would only be a
     * blob in the player's face. Flat weapons without a model keep the world
     * flash, faded down so it does not fill the screen.</p>
     */
    private static float firstPersonGuard(State state) {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return 1.0f;
            var cam = mc.player.getEyePosition();
            double dx = cam.x - state.wx;
            double dy = cam.y - state.wy;
            double dz = cam.z - state.wz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= 1.8) return 1.0f;
            boolean modelled = mc.player.getMainHandItem().getItem()
                    instanceof com.techguns.techguns3.item.AnimatedGunItem;
            if (modelled) {
                // The model draws the flash itself.
                return dist <= 0.8 ? 0.0f : (float) (dist - 0.8);
            }
            if (dist <= 0.35) return 0.12f;
            return (float) ((dist - 0.35) / (1.45) * 0.88 + 0.12);
        } catch (Exception e) {
            return 1.0f;
        }
    }
}
