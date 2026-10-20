package com.techguns.techguns3.client;

/**
 * Critically damped spring used for every "heavy" piece of weapon feedback:
 * the recoil that kicks the weapon model, the aim blend and the camera punch.
 *
 * <p>A spring behaves like real gun mass: the kick is an impulse, the model
 * then settles back on its own instead of snapping to the rest pose. That is
 * what separates a weapon that feels heavy from one that just teleports.</p>
 */
public final class Spring {
    private float value;
    private float velocity;
    private float target;
    private float stiffness;
    private float damping;

    public Spring() {
        this(90.0f, 14.0f);
    }

    public Spring(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void configure(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    /** Impulse: adds velocity, the spring converts it into displacement. */
    public void kick(float impulse) {
        velocity += impulse;
    }

    /** Where the spring settles. */
    public void setTarget(float target) {
        this.target = target;
    }

    /** Hard set, used when a state is toggled rather than physically reached. */
    public void set(float value) {
        this.value = value;
        this.target = value;
        velocity = 0.0f;
    }

    public void update(float dt) {
        if (dt <= 0.0f) return;
        // Clamp the step so a lag spike cannot launch the spring.
        float step = Math.min(dt, 0.05f);
        float accel = -stiffness * (value - target) - damping * velocity;
        velocity += accel * step;
        value += velocity * step;
    }

    public float value() {
        return value;
    }

    public float velocity() {
        return velocity;
    }

    public boolean isSettled() {
        return Math.abs(value - target) < 1.0e-4f && Math.abs(velocity) < 1.0e-4f;
    }
}
