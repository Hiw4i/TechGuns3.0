package com.techguns.techguns3.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/**
 * A bullet hole on a surface.
 *
 * <p>The quad lies flat on the face that was hit and is a little bit in front
 * of it, so it does not fight with the surface of the block. It is lit by the
 * block, so a hole in a dark corner is a dark hole, and it disappears when the
 * block it was made in is gone.</p>
 */
public final class BulletHoleParticle extends SingleQuadParticle {
    /** How long a hole stays on a wall, in ticks. */
    private static final int MAX_LIFE = 1200;
    /** The hole fades out over the last seconds. */
    private static final int FADE = 120;
    /** How far the quad floats in front of the surface. */
    private static final float OFFSET = 0.006f;

    /** The rotation that turns the quad towards a face, indexed like Direction. */
    private static final Quaternionf[] FACE_ROTATIONS = {
            faceRotation(0.0f, 180.0f, 0.0f),   // south
            faceRotation(0.0f, 0.0f, 0.0f),     // north
            faceRotation(0.0f, 90.0f, 0.0f),    // east
            faceRotation(0.0f, -90.0f, 0.0f),   // west
            faceRotation(-90.0f, 0.0f, 0.0f),   // up
            faceRotation(90.0f, 0.0f, 0.0f),    // down
    };

    private final BlockPos blockPos;
    private final BlockState blockState;
    private final Quaternionf rotation = new Quaternionf();

    public BulletHoleParticle(ClientLevel level, double x, double y, double z,
                              TextureAtlasSprite sprite, Direction face, BlockPos blockPos) {
        super(level, x, y, z, sprite);
        this.blockPos = blockPos.immutable();
        this.blockState = level.getBlockState(blockPos);
        // A little in front of the surface, otherwise the hole and the face of
        // the block fight for the same pixels.
        this.setPos(x + face.getStepX() * OFFSET,
                y + face.getStepY() * OFFSET,
                z + face.getStepZ() * OFFSET);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.quadSize = 0.13f;
        this.setLifetime(MAX_LIFE);
        this.rotation.set(FACE_ROTATIONS[face.ordinal()]);
    }

    private static Quaternionf faceRotation(float x, float y, float z) {
        Quaternionf q = new Quaternionf();
        if (x != 0.0f) q.rotateX((float) Math.toRadians(x));
        if (y != 0.0f) q.rotateY((float) Math.toRadians(y));
        if (z != 0.0f) q.rotateZ((float) Math.toRadians(z));
        return q;
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return (target, camera, partialTickTime) -> target.set(rotation);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        // The hole belongs to the block it was made in: when the block is gone
        // the hole is gone with it.
        if (!this.level.getBlockState(this.blockPos).equals(this.blockState)) {
            this.remove();
            return;
        }
        int left = this.lifetime - this.age;
        if (left < FADE) {
            this.alpha = Mth.clamp(left / (float) FADE, 0.0f, 1.0f);
        }
    }
}
