package com.techguns.techguns3.client;

import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.FlyingGibEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Body chunk: tumbling meat box textured from the original
 * {@code techguns:textures/entity/gore.png}, tinted toward the victim's blood
 * color, with a paler bone/gut core protruding like a snap.
 *
 * <p>Opaque {@code entityCutout} pipeline on purpose: the previous flat-color
 * boxes rode the translucent lightning pipeline, whose blending and missing
 * depth write made big chunks read as ghosts. Cutout writes depth, so chunks
 * occlude honestly.</p>
 */
public final class FlyingGibRenderer extends EntityRenderer<FlyingGibEntity, FlyingGibRenderer.State> {
    private static final Identifier GORE_TEXTURE =
            Identifier.fromNamespaceAndPath("techguns3", "textures/entity/gore.png");

    /** Meat region: bloody torso front of gore.png (128px texture). */
    private static final float MEAT_U1 = 40 / 128.0f;
    private static final float MEAT_V1 = 40 / 128.0f;
    private static final float MEAT_U2 = 56 / 128.0f;
    private static final float MEAT_V2 = 64 / 128.0f;
    /** Bone region: pale bloodied head front of gore.png. */
    private static final float BONE_U1 = 16 / 128.0f;
    private static final float BONE_V1 = 16 / 128.0f;
    private static final float BONE_U2 = 32 / 128.0f;
    private static final float BONE_V2 = 32 / 128.0f;

    public FlyingGibRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.15f;
    }

    public static final class State extends EntityRenderState {
        public int part;
        public int color;
        public float scale;
        public int seed;
        public int age;
        public float endFade;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FlyingGibEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.part = entity.part();
        state.color = entity.color();
        state.scale = entity.gibScale();
        state.seed = entity.seed();
        state.age = entity.tickCount;
        state.endFade = entity.endFade();
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float t = state.age + state.seed % 97 * 0.13f;
        poseStack.mulPose(new org.joml.Matrix4f()
                .rotationXYZ(t * 0.31f + state.seed, t * 0.23f + state.seed * 1.7f, 0.4f));
        // End-of-life squash: flatten and sink instead of vanishing mid-air.
        float squash = 0.35f + 0.65f * state.endFade;
        poseStack.scale(1.0f, squash, 1.0f);
        poseStack.translate(0.0f, -(1.0f - state.endFade) * 0.25f, 0.0f);

        Vec3 dims = FlyingGibEntity.dimsFor(state.part)
                .scale(Math.max(0.4f, state.scale));
        float sx = (float) dims.x;
        float sy = (float) dims.y;
        float sz = (float) dims.z;
        // Meat tint: texture stays dominant, blood color shifts it per victim.
        float br = ((state.color >> 16) & 0xFF) / 255.0f;
        float bg = ((state.color >> 8) & 0xFF) / 255.0f;
        float bb = (state.color & 0xFF) / 255.0f;
        float mr = 0.55f + 0.45f * br;
        float mg = 0.55f + 0.45f * bg;
        float mb = 0.55f + 0.45f * bb;
        boolean torso = state.part == 1;
        // Bone on limbs/head, dark guts bulging from the torso.
        float cr = torso ? 0.45f : 0.92f;
        float cg = torso ? 0.07f : 0.86f;
        float cb = torso ? 0.09f : 0.74f;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(GORE_TEXTURE), (pose, consumer) -> {
            var matrix = pose.pose();
            FxGeometry.texturedBox(matrix, consumer, sx, sy, sz, 0, 0, 0,
                    MEAT_U1, MEAT_V1, MEAT_U2, MEAT_V2, mr, mg, mb, 1.0f, FxGeometry.FULLBRIGHT);
            FxGeometry.texturedBox(matrix, consumer, sx * 0.45f, sy * 0.85f, sz * 0.45f,
                    0, sy * 0.32f, 0,
                    BONE_U1, BONE_V1, BONE_U2, BONE_V2, cr, cg, cb, 1.0f, FxGeometry.FULLBRIGHT);
        });
        poseStack.popPose();
    }
}
