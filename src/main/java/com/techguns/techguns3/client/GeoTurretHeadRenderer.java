package com.techguns.techguns3.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.renderer.base.GeoRenderState;
import com.techguns.techguns3.turret.TurretHeadEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class GeoTurretHeadRenderer extends GeoEntityRenderer<TurretHeadEntity, GeoTurretHeadRenderer.TurretRenderState> {
    public static final class TurretRenderState extends LivingEntityRenderState implements GeoRenderState {}

    public GeoTurretHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                Identifier.fromNamespaceAndPath("techguns3", "turret_head")));
        withRenderLayer(new ItemInHandGeoLayer<TurretHeadEntity, Void, TurretRenderState>(
                context, this, "Mount", "Mount"));
    }

    @Override
    public TurretRenderState createRenderState(TurretHeadEntity entity, Void context) {
        return new TurretRenderState();
    }
}
