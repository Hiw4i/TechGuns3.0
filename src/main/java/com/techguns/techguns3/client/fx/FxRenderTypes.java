package com.techguns.techguns3.client.fx;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Render pipelines for Techguns effects.
 *
 * <p>Vanilla's {@code eyes} pipeline blends translucently, which makes muzzle
 * flashes, tracers and beams read as flat stickers instead of light. These
 * pipelines reuse the <em>vanilla</em> core shaders ({@code core/entity}) with
 * a different blend and depth state, so nothing has to be written in GLSL and
 * the effect works on every backend the game ships with.</p>
 *
 * <ul>
 *   <li>{@link #ADDITIVE_TEXTURED} — textured, fullbright, additive
 *       ({@code SRC_ALPHA, ONE}), depth tested but not depth written.</li>
 *   <li>{@link #ADDITIVE_COLOR} — the same for untextured geometry, built on
 *       the vanilla lightning shader (position + colour only).</li>
 * </ul>
 */
@EventBusSubscriber(modid = com.techguns.techguns3.TechGuns3.MODID, value = Dist.CLIENT)
public final class FxRenderTypes {
    private FxRenderTypes() {}

    /** Textured, fullbright, additive. Depth tested, no depth write, no cull. */
    public static final RenderPipeline ADDITIVE_TEXTURED = RenderPipeline.builder(
                    RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET,
                    RenderPipelines.EYES_SNIPPET)
            .withShaderDefine("APPLY_TEXTURE_MATRIX")
            .withLocation(Identifier.fromNamespaceAndPath(
                    com.techguns.techguns3.TechGuns3.MODID, "pipeline/fx_additive"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /** Untextured additive geometry (position + colour only). */
    public static final RenderPipeline ADDITIVE_COLOR = RenderPipeline.builder(
                    RenderPipelines.LIGHTNING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(
                    com.techguns.techguns3.TechGuns3.MODID, "pipeline/fx_additive_color"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    private static final Map<Identifier, RenderType> TEXTURED = new ConcurrentHashMap<>();

    /** Additive textured render type for the given texture. */
    public static RenderType additive(Identifier texture) {
        return TEXTURED.computeIfAbsent(texture, tex -> RenderType.create(
                "techguns3_fx_additive",
                RenderSetup.builder(ADDITIVE_TEXTURED)
                        .withTexture("Sampler0", tex)
                        .setTextureTransform(net.minecraft.client.renderer.rendertype.TextureTransform.DEFAULT_TEXTURING)
                        .sortOnUpload()
                        .createRenderSetup()));
    }


    private static RenderType additiveColor;

    /** Additive untextured render type (position + colour). */
    public static RenderType additiveColor() {
        if (additiveColor == null) {
            additiveColor = RenderType.create("techguns3_fx_additive_color",
                    RenderSetup.builder(ADDITIVE_COLOR)
                            .sortOnUpload()
                            .createRenderSetup());
        }
        return additiveColor;
    }

    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE_TEXTURED);
        event.registerPipeline(ADDITIVE_COLOR);
    }
}
