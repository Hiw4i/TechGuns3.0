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

import java.util.List;

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
        // The real position of the sight is measured by the renderer while the
        // weapon is at the hip, that is exact. The datapack offset is only the
        // fallback for the very first frames.
        float[] hip = GunSightAlignment.hip(gun, perspective(event, offHand));
        float x;
        float y;
        if (hip != null) {
            x = Mth.clamp(-hip[0], -2.5f, 2.5f) * aim;
            y = Mth.clamp(-hip[1], -1.5f, 1.5f) * aim;
        } else {
            List<Float> offset = offHand ? gun.def().adsOffsetLeft() : gun.def().adsOffset();
            if (offset.size() < 2) {
                return;
            }
            x = Mth.clamp(offset.get(0), -2.5f, 2.5f) * aim;
            y = Mth.clamp(offset.get(1), -1.5f, 1.5f) * aim;
        }
        event.getPoseStack().translate(x, y, 0.0f);
        if (!offHand) {
            appliedX = x;
            appliedY = y;
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
