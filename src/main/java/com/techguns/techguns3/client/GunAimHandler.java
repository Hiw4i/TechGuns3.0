package com.techguns.techguns3.client;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
        boolean offHandHasGun = offHand && stack.getItem() instanceof GenericGunItem;

        // The off hand has its own offset, so the main hand one is undone
        // first. When the off hand is empty the main hand offset stays, the
        // support arm then follows the weapon.
        if (offHandHasGun && (appliedX != 0.0f || appliedY != 0.0f)) {
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
        List<Float> offset = offHand ? gun.def().adsOffsetLeft() : gun.def().adsOffset();
        if (offset.size() < 2) {
            return;
        }
        // Clamped, a broken datapack entry must not throw the weapon off screen.
        float x = Mth.clamp(offset.get(0), -2.5f, 2.5f) * aim;
        float y = Mth.clamp(offset.get(1), -1.5f, 1.5f) * aim;
        event.getPoseStack().translate(x, y, 0.0f);
        if (!offHand) {
            appliedX = x;
            appliedY = y;
        }
    }
}
