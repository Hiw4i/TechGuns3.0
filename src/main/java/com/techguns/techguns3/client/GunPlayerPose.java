package com.techguns.techguns3.client;

import com.techguns.techguns3.item.GenericGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.techguns.techguns3.TechGuns3;

/**
 * Gun stances. Hip fire keeps the original two-arm hold; aiming (RMB, vanilla
 * {@code use} with the {@code BOW} animation) raises the bow-aim pose so both
 * first- and third-person reads match modern shooters.
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class GunPlayerPose {
    private GunPlayerPose() {}

    @SubscribeEvent
    public static void beforePlayerRender(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (!(state.getMainHandItemStack().getItem() instanceof GenericGunItem gun)) return;
        boolean aiming = isAiming(state);
        HumanoidModel.ArmPose mainPose =
                aiming ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.CROSSBOW_HOLD;
        if (!gun.isTwoHanded() && !aiming) {
            // One-handed hip fire: vanilla arm, no forced crossbow stance.
            return;
        }
        if (state.mainArm == HumanoidArm.RIGHT) {
            state.rightArmPose = mainPose;
            state.leftArmPose = gun.isTwoHanded() ? HumanoidModel.ArmPose.EMPTY : state.leftArmPose;
        } else {
            state.leftArmPose = mainPose;
            state.rightArmPose = gun.isTwoHanded() ? HumanoidModel.ArmPose.EMPTY : state.rightArmPose;
        }
    }

    private static boolean isAiming(AvatarRenderState state) {
        if (state instanceof HumanoidRenderState humanoid) {
            return humanoid.isUsingItem
                    && humanoid.getMainHandItemStack().getItem() instanceof GenericGunItem;
        }
        return false;
    }

    /** Keep the free hand under the fore-end in first person while holding a long gun. */
    @SubscribeEvent
    public static void supportArm(RenderArmEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getOffhandItem().isEmpty()
                || !(player.getMainHandItem().getItem() instanceof GenericGunItem gun)
                || !gun.isTwoHanded() || event.getArm() == player.getMainArm()) return;
        if (player.isUsingItem()) return; // BOW animation already places both arms.
        int side = event.getArm() == HumanoidArm.LEFT ? 1 : -1;
        event.getPoseStack().translate(side * 0.38, 0.10, -0.18);
    }
}
