package com.techguns.techguns3.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Curve-driven sprite particle: size eases out, color lerps start to end,
 * alpha snaps in and fades out. Glow instances render fullbright, puffs shaded.
 */
public final class CurveParticle extends SingleQuadParticle {
    private final float r0;
    private final float g0;
    private final float b0;
    private final float r1;
    private final float g1;
    private final float b1;
    private final float size0;
    private final float size1;
    private final float drag;
    private final boolean emissive;
    private final float rollSpeed;

    public CurveParticle(ClientLevel level, double x, double y, double z,
                         double vx, double vy, double vz, TextureAtlasSprite sprite,
                         int color, int endColor, float size0, float size1, int life,
                         float gravity, float drag, boolean emissive, float rollSpeed) {
        super(level, x, y, z, vx, vy, vz, sprite);
        this.r0 = ((color >> 16) & 0xFF) / 255.0f;
        this.g0 = ((color >> 8) & 0xFF) / 255.0f;
        this.b0 = (color & 0xFF) / 255.0f;
        this.r1 = ((endColor >> 16) & 0xFF) / 255.0f;
        this.g1 = ((endColor >> 8) & 0xFF) / 255.0f;
        this.b1 = (endColor & 0xFF) / 255.0f;
        this.size0 = size0;
        this.size1 = size1;
        this.drag = drag;
        this.emissive = emissive;
        this.rollSpeed = rollSpeed;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize = size0;
        this.setColor(r0, g0, b0);
        this.setLifetime(life);
        this.gravity = gravity;
        this.roll = random.nextFloat() * (float) (Math.PI * 2);
        this.oRoll = this.roll;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return emissive ? 0xF000F0 : super.getLightCoords(partialTick);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float k = (float) this.age / (float) this.lifetime;
        float eased = 1.0f - (1.0f - k) * (1.0f - k) * (1.0f - k);
        this.quadSize = size0 + (size1 - size0) * eased;
        this.rCol = r0 + (r1 - r0) * k;
        this.gCol = g0 + (g1 - g0) * k;
        this.bCol = b0 + (b1 - b0) * k;
        this.alpha = Math.min(1.0f, k * 7.0f) * (float) Math.pow(1.0f - k, 1.3f);
        this.roll += rollSpeed;
        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= drag;
        this.yd *= drag;
        this.zd *= drag;
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
    }
}
