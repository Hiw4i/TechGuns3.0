package com.techguns.techguns3.client;

import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.techguns.techguns3.turret.TurretBaseBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;

public final class GeoTurretBaseRenderer extends GeoBlockRenderer<TurretBaseBlockEntity, BlockEntityRenderState> {
    public GeoTurretBaseRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<>(
                Identifier.fromNamespaceAndPath("techguns3", "turret_base")));
    }
}
