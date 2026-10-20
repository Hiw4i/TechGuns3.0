package com.techguns.techguns3.client;

import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import com.techguns.techguns3.TechGuns3;

/**
 * Gun stances.
 *
 * <p>In third person the weapon keeps the vanilla hold pose, and that is
 * deliberate. The bore of a gun points along the item's -Z, and every vanilla
 * pose that raises the arm (the crossbow hold for instance) turns the arm
 * forward by 90 degrees, which tips the barrel into the ground and swings the
 * weapon across the body to the left. A proper "shouldered aim" in third
 * person would need a transform on the item itself, which the vanilla item
 * renderer does not offer.</p>
 *
 * <p>First person the weapon is a view model: it is handled by
 * {@link TechGunsGeoItemRenderer} (aim pose, sway, bob, recoil) and
 * {@link GunAimHandler} (the sights on the crosshair).</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class GunPlayerPose {
    private GunPlayerPose() {}

    /** Keep the free hand under the fore-end in first person while holding a long gun. */
    @SubscribeEvent
    public static void supportArm(RenderArmEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getOffhandItem().isEmpty()
                || !(player.getMainHandItem().getItem() instanceof GenericGunItem gun)
                || !gun.isTwoHanded() || event.getArm() == player.getMainArm()) {
            return;
        }
        int side = event.getArm() == HumanoidArm.LEFT ? 1 : -1;
        event.getPoseStack().translate(side * 0.38, 0.10, -0.18);
    }
}
