package com.techguns.techguns3.client;

import com.techguns.techguns3.entity.GunSoldierEntity;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.registry.TGEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Both source NPCs used the vanilla biped geometry and their own 64x64 skin. */
public final class GunSoldierRenderer extends HumanoidMobRenderer<GunSoldierEntity,
        HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier ZOMBIE_TEXTURE = Identifier.fromNamespaceAndPath(
            "techguns3", "textures/entity/zombie_soldier.png");
    private static final Identifier BANDIT_TEXTURE = Identifier.fromNamespaceAndPath(
            "techguns3", "textures/entity/bandit.png");

    public GunSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(GunSoldierEntity entity, HumanoidArm arm) {
        if (entity.getMainHandItem().getItem() instanceof GenericGunItem gun && gun.isTwoHanded()) {
            return arm == entity.getMainArm() ? HumanoidModel.ArmPose.CROSSBOW_HOLD
                    : HumanoidModel.ArmPose.EMPTY;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return state.entityType == TGEntities.BANDIT.get() ? BANDIT_TEXTURE : ZOMBIE_TEXTURE;
    }
}
