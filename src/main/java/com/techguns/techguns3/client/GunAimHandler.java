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
import java.util.Set;

/**
 * Puts the rear sight of the held weapon on the crosshair while aiming.
 *
 * <p>While the weapon is at the hip the renderer measures where its rear
 * sight really ends up on the screen and caches that position. Aiming slides
 * the weapon by exactly that distance, so the sight ends up in the middle of
 * the screen while the weapon is shouldered.</p>
 *
 * <p>The measurement is the only place where the real position is known: it
 * depends on the model, on the display transform of the item and on the hand
 * placement of the game. A hand written offset would have to be recomputed
 * for every change of any of them.</p>
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
        // The renderer measures where the rear sight of the weapon really ends
        // up on the screen while the weapon is at the hip. The weapon is slid
        // by that distance, so the sight ends up in the middle of the screen.
        float[] hip = GunSightAlignment.hip(gun, perspective);
        if (hip == null) {
            // The weapon was just taken into the hand and has not been measured
            // yet. It is not moved, and the measurement slides it into place as
            // soon as it is there.
            return;
        }
        float x = Mth.clamp(-hip[0], -2.5f, 2.5f) * aim;
        float y = Mth.clamp(-hip[1], -1.5f, 1.5f) * aim;
        event.getPoseStack().translate(x, y, 0.0f);
        if (!offHand) {
            appliedX = x;
            appliedY = y;
        }
    }

    /**
     * Reports the position of the rear sight of the weapon on the screen.
     *
     * <p>The renderer measures it while the weapon is at the hip, and the
     * measurement is the only place where the real position is known: it
     * depends on the model, on the display transform of the item and on the
     * hand placement of the game.</p>
     *
     * @param x camera space X of the rear sight
     * @param y camera space Y of the rear sight
     */
    public static void reportSight(GenericGunItem gun, ItemDisplayContext perspective,
                                   float x, float y) {
        String key = gun.gunId() + ":" + perspective.name();
        if (!REPORTED.add(key)) {
            return;
        }
        TechGuns3.LOGGER.info("[TechGuns3] the sight of {} is at ({},{}) on the screen",
                gun.gunId(), x, y);
    }

    /** The display context a hand is drawn with, the same rule the game uses. */
    private static ItemDisplayContext perspective(RenderHandEvent event, boolean offHand) {
        var player = Minecraft.getInstance().player;
        boolean mainHandRight = player == null || player.getMainArm() == HumanoidArm.RIGHT;
        boolean right = offHand ? !mainHandRight : mainHandRight;
        return right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
}
