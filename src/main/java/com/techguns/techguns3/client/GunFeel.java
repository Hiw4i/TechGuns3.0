package com.techguns.techguns3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Client-side weapon feel for the first-person view model.
 *
 * <p>Owns everything that has to move continuously rather than on key events:
 * the aim blend, the sprint pose, the mouse sway, the walk bob and the recoil
 * spring that kicks the weapon model back after every shot. All of it is
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
    /** Weapon kick: pitch (radians) and yaw (radians) after a shot. */
    private final Spring recoilPitch = new Spring(120.0f, 17.0f);
    private final Spring recoilYaw = new Spring(120.0f, 17.0f);
    /** Camera punch, drives the zoom FOV spike on every shot. */
    private final Spring fovPunch = new Spring(170.0f, 19.0f);

    private boolean aiming;
    private boolean sprinting;

    /** Mouse-lag sway in radians, decays back to zero. */
    private float swayPitch;
    private float swayYaw;
    private float bobPhase;
    private float walkAmount;

    private long lastFrameNanos;
    private float lastYaw;
    private float lastPitch;
    private boolean haveLastAngles;

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
        return recoilPitch.value();
    }

    /** Weapon kick around the Y axis, in radians. */
    public float recoilYaw() {
        return recoilYaw.value();
    }

    /** FOV punch, 0..1, spikes right after a shot. */
    public float fovPunch() {
        return Mth.clamp(fovPunch.value(), 0.0f, 1.0f);
    }

    /** Mouse-lag sway around the X axis, in radians. */
    public float swayPitch() {
        return swayPitch;
    }

    /** Mouse-lag sway around the Y axis, in radians. */
    public float swayYaw() {
        return swayYaw;
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
        float pitch = (float) Math.toRadians(pitchDegrees) * scale;
        float yaw = (float) Math.toRadians(yawDegrees) * scale;
        // The impulse is scaled by the spring frequency, so the peak of the
        // kick is independent of the spring tuning.
        recoilPitch.kick(pitch * 38.0f);
        recoilYaw.kick(yaw * 38.0f);
        fovPunch.kick(1.0f * scale);
    }

    /** Advance every spring. Call once per rendered frame. */
    public void update() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0f : (now - lastFrameNanos) / 1_000_000_000.0f;
        lastFrameNanos = now;
        if (dt <= 0.0f) return;
        dt = Math.min(dt, 0.1f);

        aim.setTarget(aiming ? 1.0f : 0.0f);
        sprint.setTarget(sprinting ? 1.0f : 0.0f);
        aim.update(dt);
        sprint.update(dt);
        recoilPitch.update(dt);
        recoilYaw.update(dt);
        fovPunch.update(dt);

        updateSway(dt);
        updateBob(dt);
    }

    /**
     * Weapon lag: the model keeps following the camera a moment after the
     * mouse moved, which is what gives a held weapon its weight.
     */
    private void updateSway(float dt) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            swayPitch = 0.0f;
            swayYaw = 0.0f;
            haveLastAngles = false;
            return;
        }
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (haveLastAngles) {
            // The mouse delta is already smoothed by the game; a fraction of it
            // becomes the lag, and the lag is pulled back to zero over time.
            float dYaw = yaw - lastYaw;
            float dPitch = pitch - lastPitch;
            if (dYaw > 180.0f) dYaw -= 360.0f;
            if (dYaw < -180.0f) dYaw += 360.0f;
            swayYaw = Mth.clamp(swayYaw - dYaw * 0.0016f, -0.05f, 0.05f);
            swayPitch = Mth.clamp(swayPitch - dPitch * 0.0016f, -0.05f, 0.05f);
        }
        lastYaw = yaw;
        lastPitch = pitch;
        haveLastAngles = true;

        float relax = (float) Math.exp(-dt * 7.0);
        swayYaw *= relax;
        swayPitch *= relax;
    }

    /** Walk bob: the weapon breathes with the player's stride. */
    private void updateBob(float dt) {
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
        bobPhase += dt * (6.0f + 4.0f * walkAmount);
        if (bobPhase > Math.PI * 2.0f) {
            bobPhase -= Math.PI * 2.0f;
        }
    }
}
