package com.techguns.techguns3.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Client-side weapon feel for the first-person view model.
 *
 * <p>Owns everything that has to move continuously rather than on key events:
 * the aim blend, the sprint pose, the mouse sway, the walk bob, the scope
 * zoom and the recoil of the weapon model after every shot. All of it is
 * driven from the render loop so the motion stays smooth at any frame rate.</p>
 *
 * <p>Purely visual: the server still owns damage, spread and ammo. The class
 * is touched from the render thread only.</p>
 */
public final class GunFeel {
    private static final GunFeel INSTANCE = new GunFeel();

    /** Aim blend: 0 = hip fire, 1 = shouldered. */
    private final Spring aim = new Spring(150.0f, 25.0f);
    /** Sprint pose: 1 = weapon lowered while running. */
    private final Spring sprint = new Spring(120.0f, 20.0f);
    /** Camera punch, drives the zoom FOV spike on every shot. */
    private final Spring fovPunch = new Spring(170.0f, 19.0f);
    /** Scope zoom: 1 = no zoom, below 1 = zoomed in. */
    private final Spring zoom = new Spring(600.0f, 49.0f);

    /**
     * How far the model moves compared to the camera.
     *
     * <p>The camera kick is the angle of the gun definition, the view model
     * moves a little less than that: a real weapon fills the screen more than
     * the aim point moves, but the model is the smaller of the two.</p>
     *
     * <p>The value is the measured peak of the spring that stood here before,
     * so the recoil keeps the strength it had at 60 frames per second. The
     * spring itself was integrated with one step per frame, so its peak was
     * a quarter of this value at 30 frames per second - the recoil was four
     * times weaker on a slow machine.</p>
     */
    private static final float RECOIL_GAIN = 0.7f;

    /** The weapon never swings further than this, in radians. */
    private static final float MAX_SWAY = 0.07f;

    /**
     * How much of the mouse movement becomes the lag of the weapon.
     *
     * <p>A quick flick of the mouse swings the weapon by about one and a half
     * degrees, a slow movement by a fraction of that.</p>
     */
    private static final float SWAY_IMPULSE = 0.09f;

    /**
     * The weapon trails behind the camera: the mouse pushes it, the spring
     * pulls it back. Two of them, one per axis.
     */
    private final Spring swayPitch = new Spring(90.0f, 14.0f);
    private final Spring swayYaw = new Spring(90.0f, 14.0f);

    /**
     * The recoil of the model. Every shot adds its own kick, and the kicks
     * overlap while a burst is firing, so a fast weapon climbs further and
     * further instead of jumping to the same place every time.
     */
    private final List<Kick> kicks = new ArrayList<>(8);

    private boolean aiming;
    private boolean sprinting;

    private float walkAmount;
    private float bobPhase;

    private long lastFrameNanos;
    private float lastYaw;
    private float lastPitch;
    private boolean haveLastAngles;

    private final Random random = new Random();

    private GunFeel() {}

    public static GunFeel get() {
        return INSTANCE;
    }

    public void setAiming(boolean aiming) {
        this.aiming = aiming;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isAiming() {
        return aiming;
    }

    /** Aim blend, 0..1. */
    public float aimProgress() {
        return Mth.clamp(aim.value(), 0.0f, 1.0f);
    }

    /** Sprint blend, 0..1. */
    public float sprintProgress() {
        return Mth.clamp(sprint.value(), 0.0f, 1.0f);
    }

    /** Weapon kick around the X axis (up), in radians. */
    public float recoilPitch() {
        float total = 0.0f;
        for (Kick kick : kicks) {
            total += kick.pitch * kick.shape() + kick.wobble;
        }
        return total;
    }

    /** Weapon kick around the Y axis, in radians. */
    public float recoilYaw() {
        float total = 0.0f;
        for (Kick kick : kicks) {
            total += kick.yaw * kick.shape();
        }
        return total;
    }

    /** FOV punch, 0..1, spikes right after a shot. */
    public float fovPunch() {
        return Mth.clamp(fovPunch.value(), 0.0f, 1.0f);
    }

    /** The scope zoom factor, 1 = no zoom. */
    public float zoom() {
        return zoom.value();
    }

    /** Where the scope zoom settles. */
    public void setZoomTarget(float target) {
        zoom.setTarget(target);
    }

    /** Mouse-lag sway around the X axis, in radians. */
    public float swayPitch() {
        return Mth.clamp(swayPitch.value(), -MAX_SWAY, MAX_SWAY);
    }

    /** Mouse-lag sway around the Y axis, in radians. */
    public float swayYaw() {
        return Mth.clamp(swayYaw.value(), -MAX_SWAY, MAX_SWAY);
    }

    /** Walk bob phase in radians. */
    public float bobPhase() {
        return bobPhase;
    }

    /** How much the weapon is bobbing, 0..1. */
    public float walkAmount() {
        return walkAmount;
    }

    /**
     * Kick the weapon after a shot.
     *
     * @param pitchDegrees how far the muzzle climbs, in degrees
     * @param yawDegrees   sideways wander, in degrees
     * @param scale        extra punch factor (1 = the gun's own recoil value)
     */
    public void kick(float pitchDegrees, float yawDegrees, float scale) {
        // Every shot is a little different, a weapon that kicks by exactly the
        // same amount every time reads as an animation.
        float jitter = 0.86f + random.nextFloat() * 0.28f;
        float pitch = (float) Math.toRadians(pitchDegrees) * scale * jitter * RECOIL_GAIN;
        float yaw = (float) Math.toRadians(yawDegrees) * scale * jitter * RECOIL_GAIN;
        // A gun climbs a little more than it wanders, even on a straight shot.
        pitch *= 0.94f + random.nextFloat() * 0.12f;

        Kick kick = new Kick();
        kick.pitch = pitch;
        kick.yaw = yaw;
        kick.life = 0.18f + 0.05f * Math.min(1.5f, Math.abs(pitchDegrees) * scale);
        kick.seed = random.nextInt();
        kick.wobbleAmount = 0.12f * Math.abs(pitch);
        kicks.add(kick);

        fovPunch.kick(1.0f * scale * jitter);
    }

    /** Advance every spring. Call once per rendered frame. */
    public void update(DeltaTracker tracker) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0f : (now - lastFrameNanos) / 1_000_000_000.0f;
        lastFrameNanos = now;
        if (dt <= 0.0f) return;
        dt = Math.min(dt, 0.1f);

        aim.setTarget(aiming ? 1.0f : 0.0f);
        sprint.setTarget(sprinting ? 1.0f : 0.0f);
        aim.update(dt);
        sprint.update(dt);
        fovPunch.update(dt);
        zoom.update(dt);
        swayPitch.update(dt);
        swayYaw.update(dt);

        updateKicks(dt);
        updateSway(dt, tracker);
        updateBob(dt, tracker);
    }

    /** One shot worth of recoil. */
    private static final class Kick {
        float pitch;
        float yaw;
        float age;
        float life;
        int seed;
        float wobbleAmount;
        /** The small irregularity of this particular kick. */
        float wobble;

        /**
         * The shape of a kick: it snaps up almost immediately and then settles
         * slowly back to the rest pose.
         */
        float shape() {
            if (age >= life) return 0.0f;
            float p = age / life;
            // The kick snaps up in the first 15% of its life, then settles.
            float rise = 0.15f;
            if (p < rise) {
                float k = p / rise;
                float inv = 1.0f - k;
                return 1.0f - inv * inv * inv;
            }
            float q = (p - rise) / (1.0f - rise);
            float inv = 1.0f - q;
            return inv * inv * (1.0f + 2.0f * q);
        }
    }

    private void updateKicks(float dt) {
        if (kicks.isEmpty()) return;
        Iterator<Kick> it = kicks.iterator();
        while (it.hasNext()) {
            Kick kick = it.next();
            kick.age += dt;
            if (kick.age >= kick.life) {
                it.remove();
                continue;
            }
            // A little noise on top of the curve, so the kick is not a perfect
            // arc. The noise is scaled with the kick, a small one stays clean.
            float n = SmoothNoise.value(kick.age * 11.0f, kick.seed);
            kick.wobble = n * kick.wobbleAmount * (1.0f - kick.age / kick.life);
        }
    }

    /**
     * Weapon lag: the model keeps following the camera a moment after the
     * mouse moved, which is what gives a held weapon its weight.
     *
     * <p>The angles of the head are used and not the ones of the body: the
     * body only follows the head when the player walks, so a sway that was
     * built on the body would not react to the mouse at all while standing
     * still.</p>
     */
    private void updateSway(float dt, DeltaTracker tracker) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            swayPitch.setTarget(0.0f);
            swayYaw.setTarget(0.0f);
            haveLastAngles = false;
            return;
        }
        float partial = tracker == null ? 0.0f : tracker.getGameTimeDeltaPartialTick(false);
        float yaw = player.getViewYRot(partial);
        float pitch = player.getViewXRot(partial);
        if (haveLastAngles) {
            // The mouse delta is already smoothed by the game; a fraction of it
            // becomes the lag, and the spring pulls the weapon back to rest.
            float dYaw = yaw - lastYaw;
            float dPitch = pitch - lastPitch;
            if (dYaw > 180.0f) dYaw -= 360.0f;
            if (dYaw < -180.0f) dYaw += 360.0f;
            swayYaw.kick(-dYaw * SWAY_IMPULSE);
            swayPitch.kick(-dPitch * SWAY_IMPULSE);
        }
        lastYaw = yaw;
        lastPitch = pitch;
        haveLastAngles = true;
    }

    /**
     * Walk bob: the weapon breathes with the player's stride.
     *
     * <p>The phase comes from the walk animation of the player, the same value
     * that drives the legs. A bob that runs on its own clock slides against the
     * stride, a bob that follows the legs does not.</p>
     */
    private void updateBob(float dt, DeltaTracker tracker) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            walkAmount = 0.0f;
            return;
        }
        float speed = (float) player.getDeltaMovement().horizontalDistance();
        float moving = Mth.clamp(speed / 0.13f, 0.0f, 1.0f);
        // Aiming steadies the weapon; sprinting replaces the bob with a pose.
        float target = moving * (1.0f - 0.75f * aimProgress());
        walkAmount += (target - walkAmount) * Math.min(1.0f, dt * 8.0f);

        float partial = tracker == null ? 0.0f : tracker.getGameTimeDeltaPartialTick(false);
        // The leg swing of the player runs on walkAnimation * 0.6662, so the
        // bob is in step with the legs and not with the wall clock.
        bobPhase = player.walkAnimation.position(partial) * 0.6662f;
    }
}
