package com.techguns.techguns3.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    /** The vanilla base quad lies in the XY plane: its normal is +Z. */
    private static final Vector3f BASE_NORMAL = new Vector3f(0.0f, 0.0f, 1.0f);

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
        // Shortest-arc rotation from the quad's base normal onto the face
        // normal, so the quad always lies flat on the surface that was hit.
        // A hand-written per-face table was indexed with the wrong direction
        // order once already — deriving it from the normal cannot drift.
        this.rotation.rotationTo(BASE_NORMAL,
                new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ()));
    }

    private final SingleQuadParticle.FacingCameraMode facing = (target, camera, partialTickTime) ->
            target.set(rotation);

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return facing;
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
