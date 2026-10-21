package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puts the rear sight of the held weapon on the crosshair while aiming.
 *
 * <p>The weapon is slid in camera space by the offset stored in the gun
 * definition. That offset is computed offline (see
 * {@code tools/add_ads_offsets.py}) from the position of the sight in the
 * model, the item display transform and the first person hand placement of
 * the game, so the sight ends up exactly in the middle of the screen while
 * the weapon is shouldered.</p>
 *
 * <p>While the weapon is at the hip the renderer measures where the sight
 * really ends up and that measurement is used instead of the stored offset,
 * so the alignment stays exact when a model or a display transform changes.</p>
 *
 * <p>The event fires before the hand is drawn, in the camera space of the
 * first person view, which is the only place where a screen aligned movement
 * can be applied. The support arm of a two handed weapon is drawn afterwards
 * and therefore follows the weapon for free.</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class GunAimHandler {
    private GunAimHandler() {}

    /** Offset that is currently applied to the pose stack (main hand). */
    private static float appliedX;
    private static float appliedY;

    /** Weapons whose sight was already compared with the stored offset. */
    private static final Set<String> REPORTED = new HashSet<>();

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        boolean offHand = event.getHand() == InteractionHand.OFF_HAND;

        if (!offHand) {
            // The main hand is rendered first, it owns the offset.
            appliedX = 0.0f;
            appliedY = 0.0f;
        } else if (appliedX != 0.0f || appliedY != 0.0f) {
            // The off hand has its own offset, so the main hand one is undone
            // first. It would otherwise drag a torch or a block along.
            event.getPoseStack().translate(-appliedX, -appliedY, 0.0f);
            appliedX = 0.0f;
            appliedY = 0.0f;
        }

        if (!(stack.getItem() instanceof GenericGunItem gun)) {
            return;
        }
        float aim = GunFeel.get().aimProgress() * (float) TGConfig.adsPose();
        if (aim <= 0.001f) {
            return;
        }
        ItemDisplayContext perspective = perspective(event, offHand);
        // The real position of the sight is measured by the renderer while the
        // weapon is at the hip, that is exact. The datapack offset is only the
        // fallback for the very first frames.
        float[] hip = GunSightAlignment.hip(gun, perspective);
        List<Float> stored = offHand ? gun.def().adsOffsetLeft() : gun.def().adsOffset();
        float x;
        float y;
        if (hip != null) {
            x = Mth.clamp(-hip[0], -2.5f, 2.5f) * aim;
            y = Mth.clamp(-hip[1], -1.5f, 1.5f) * aim;
            if (stored.size() >= 2) {
                reportDrift(gun, offHand, hip[0], hip[1], stored.get(0), stored.get(1));
            }
        } else {
            if (stored.size() < 2) {
                return;
            }
            x = Mth.clamp(stored.get(0), -2.5f, 2.5f) * aim;
            y = Mth.clamp(stored.get(1), -1.5f, 1.5f) * aim;
        }
        event.getPoseStack().translate(x, y, 0.0f);
        if (!offHand) {
            appliedX = x;
            appliedY = y;
        }
    }

    /**
     * Compares the measured position of the sight with the offset that was
     * computed offline.
     *
     * <p>Both describe the same thing, so they have to agree. When they do
     * not, the model or the display transform of the weapon changed and the
     * stored offset is stale - the log says so instead of the sight quietly
     * ending up somewhere else.</p>
     */
    private static void reportDrift(GenericGunItem gun, boolean offHand,
                                    float measuredX, float measuredY,
                                    float storedX, float storedY) {
        String key = gun.gunId() + (offHand ? ":left" : ":right");
        if (!REPORTED.add(key)) {
            return;
        }
        float dx = measuredX + storedX;
        float dy = measuredY + storedY;
        if (Math.abs(dx) > 0.25f || Math.abs(dy) > 0.25f) {
            TechGuns3.LOGGER.warn(
                    "[TechGuns3] the sight of {} does not match its stored offset "
                            + "(measured ({:.3f},{:.3f}), stored ({:.3f},{:.3f})). "
                            + "Run tools/add_ads_offsets.py to refresh the datapack.",
                    gun.gunId(), measuredX, measuredY, -storedX, -storedY);
        }
    }

    /** The display context a hand is drawn with, the same rule the game uses. */
    private static ItemDisplayContext perspective(RenderHandEvent event, boolean offHand) {
        var player = Minecraft.getInstance().player;
        boolean mainHandRight = player == null || player.getMainArm() == HumanoidArm.RIGHT;
        boolean right = offHand ? !mainHandRight : mainHandRight;
        return right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
}
