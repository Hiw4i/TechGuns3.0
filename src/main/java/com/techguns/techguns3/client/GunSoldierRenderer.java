package com.techguns.techguns3.client;

import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.mob.TGArmedMob;
import com.techguns.techguns3.registry.TGEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;

import java.util.HashMap;
import java.util.Map;

/**
 * One renderer for every armed NPC: the vanilla biped geometry with a
 * per-entity-type 64x64 skin (see {@code tools/make_mob_skins.py}) and the
 * two-handed gun carry pose. Scale comes free through the SCALE attribute.
 */
public final class GunSoldierRenderer extends HumanoidMobRenderer<TGArmedMob,
        HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Map<EntityType<?>, Identifier> TEXTURES = new HashMap<>();

    static {
        put(TGEntities.ZOMBIE_SOLDIER.get(), "zombie_soldier");
        put(TGEntities.BANDIT.get(), "bandit");
        put(TGEntities.ARMY_SOLDIER.get(), "army_soldier");
        put(TGEntities.COMMANDO.get(), "commando");
        put(TGEntities.OUTCAST.get(), "outcast");
        put(TGEntities.ZOMBIE_POLICEMAN.get(), "zombie_policeman");
        put(TGEntities.ZOMBIE_MINER.get(), "zombie_miner");
        put(TGEntities.PSYCHO_STEVE.get(), "psycho_steve");
        put(TGEntities.SKELETON_SOLDIER.get(), "skeleton_soldier");
        put(TGEntities.ZOMBIE_PIGMAN_SOLDIER.get(), "zombie_pigman_soldier");
        put(TGEntities.SUPER_MUTANT.get(), "super_mutant");
        put(TGEntities.ZOMBIE_FARMER.get(), "zombie_farmer");
    }

    private static void put(EntityType<?> type, String texture) {
        TEXTURES.put(type, Identifier.fromNamespaceAndPath("techguns3",
                "textures/entity/" + texture + ".png"));
    }

    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath(
            "techguns3", "textures/entity/zombie_soldier.png");

    public GunSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(TGArmedMob entity, HumanoidArm arm) {
        if (entity.getMainHandItem().getItem() instanceof GenericGunItem gun && gun.isTwoHanded()) {
            return arm == entity.getMainArm() ? HumanoidModel.ArmPose.CROSSBOW_HOLD
                    : HumanoidModel.ArmPose.EMPTY;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURES.getOrDefault(state.entityType, FALLBACK);
    }
}
