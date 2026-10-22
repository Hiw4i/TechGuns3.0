package com.techguns.techguns3.client;

/**
 * Critically damped spring used for every "heavy" piece of weapon feedback:
 * the aim blend, the sprint pose, the camera punch and the scope zoom.
 *
 * <p>A spring behaves like real gun mass: the kick is an impulse, the model
 * then settles back on its own instead of snapping to the rest pose. That is
 * what separates a weapon that feels heavy from one that just teleports.</p>
 *
 * <p>The integration is substepped. A single Euler step of a full frame is
 * unstable at a low frame rate and would leave the spring in a different state
 * depending on the frame rate; a fixed step of at most 1/120 s keeps the
 * motion the same at 30, 60 and 240 frames per second.</p>
 */
public final class Spring {
    /** Longest step the integration takes at once. */
    private static final float MAX_STEP = 1.0f / 120.0f;
    /** Upper bound for the simulated time per frame, so a stall cannot hang the game. */
    private static final int MAX_STEPS = 16;

    private float value;
    private float velocity;
    private float target;
    private final float stiffness;
    private final float damping;

    public Spring(float stiffness, float damping) {
        this(stiffness, damping, 0.0f);
    }

    /**
     * @param initialValue where the spring starts, for a spring that already
     *                     stands for something at the start (the zoom of the
     *                     scope is one, not zero)
     */
    public Spring(float stiffness, float damping, float initialValue) {
        this.stiffness = stiffness;
        this.damping = damping;
        this.value = initialValue;
        this.target = initialValue;
    }

    /** Impulse: adds velocity, the spring converts it into displacement. */
    public void kick(float impulse) {
        velocity += impulse;
    }

    /** Where the spring settles. */
    public void setTarget(float target) {
        this.target = target;
    }

    public void update(float dt) {
        if (dt <= 0.0f) return;
        float remaining = Math.min(dt, MAX_STEP * MAX_STEPS);
        while (remaining > 0.0f) {
            float step = Math.min(remaining, MAX_STEP);
            float accel = -stiffness * (value - target) - damping * velocity;
            velocity += accel * step;
            value += velocity * step;
            remaining -= step;
        }
    }

    public float value() {
        return value;
    }
}
