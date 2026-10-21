package com.techguns.techguns3.client;

import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.client.fx.FxGeometry;
import com.techguns.techguns3.entity.SlimeBlobEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Slime blob: a translucent green blob, sized by growth and squashed along
 * the block face normal.
 *
 * <p>It is a translucent solid and not an additive glow - the gel is lit by
 * the world, a glowing cube on the wall reads as a bug.</p>
 */
public final class SlimeBlobRenderer extends EntityRenderer<SlimeBlobEntity, SlimeBlobRenderer.State> {
    private static final Identifier SLIME_TEXTURE = Identifier.fromNamespaceAndPath(
            TechGuns3.MODID, "textures/fx/slime.png");

    public SlimeBlobRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    public static final class State extends EntityRenderState {
        public Direction face = Direction.UP;
        public float growth = 0.1f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SlimeBlobEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.face = entity.face();
        state.growth = entity.growth();
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        float size = 0.25f + 0.65f * Math.min(1.2f, state.growth);
        float squash = 0.7f;
        float bx = size;
        float by = size;
        float bz = size;
        switch (state.face.getAxis()) {
            case X -> bx *= squash;
            case Y -> by *= squash;
            case Z -> bz *= squash;
        }
        final float sx = bx;
        final float sy = by;
        final float sz = bz;
        // The core of the blob is brighter than the shell around it.
        float core = 0.55f;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(SLIME_TEXTURE),
                (pose, consumer) -> {
            var matrix = pose.pose();
            FxGeometry.texturedBox(matrix, consumer, sx, sy, sz,
                    0, 0, 0, 0, 0, 1, 1,
                    0.55f, 1.0f, 0.60f, 0.55f, state.lightCoords);
            FxGeometry.texturedBox(matrix, consumer, sx * core, sy * core, sz * core,
                    0, 0, 0, 0, 0, 1, 1,
                    0.75f, 1.0f, 0.80f, 0.85f, state.lightCoords);
        });
    }
}
