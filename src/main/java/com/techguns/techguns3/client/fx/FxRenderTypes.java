package com.techguns.techguns3.client.fx;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
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
 * <p>Two families live side by side:</p>
 *
 * <ul>
 *   <li><b>Legacy</b> ({@link #ADDITIVE_TEXTURED}, {@link #ADDITIVE_COLOR}) reuse the
 *       <em>vanilla</em> core shaders ({@code core/entity}, {@code core/rendertype_lightning})
 *       with a different blend and depth state. Nothing has to be written in GLSL and the
 *       effect works on every backend the game ships with. They are the fallback when the
 *       enhanced shaders are switched off.</li>
 *   <li><b>Enhanced</b> ({@link #FX_SPRITE} and friends) are own core shaders under
 *       {@code assets/techguns3/shaders/core}. They add the two things the vanilla shaders
 *       cannot do for an additive effect: a soft radial edge, so a glow does not end in a
 *       hard square, and a fog that <em>dimmed</em> the light instead of tinting it,
 *       because an additive blend that mixes towards the fog colour adds a grey haze to
 *       every distant tracer.</li>
 * </ul>
 *
 * <p>The enhanced shaders are written in the dialect of the vanilla shaders, so the game
 * compiles them through shaderc into SPIR-V and the exact same source runs on the Vulkan
 * and on the OpenGL backend.</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID, value = Dist.CLIENT)
public final class FxRenderTypes {
    private FxRenderTypes() {}

    /** How an effect sprite is drawn. */
    public enum Style {
        /** The sprite carries its own shape, the edge is left alone (muzzle flash art). */
        SPRITE,
        /** Soft radial edge, single texture. */
        GLOW,
        /** Soft radial edge, 4x4 sprite sheet. */
        ATLAS_GLOW,
        /** Flowing plasma, 4x4 sprite sheet. */
        ATLAS_ENERGY,
        /** Beam cross section profile. */
        BEAM
    }

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

    /** Additive sprite with the shape of the texture, own shader. */
    public static final RenderPipeline FX_SPRITE = enhanced("fx_sprite", false, 0);

    /** Additive sprite with a soft radial edge, own shader. */
    public static final RenderPipeline FX_GLOW = enhanced("fx_sprite", true, 1);

    /** Additive sprite with a soft radial edge inside a 4x4 sheet, own shader. */
    public static final RenderPipeline FX_ATLAS_GLOW = enhanced("fx_sprite", true, 4);

    /** Flowing plasma inside a 4x4 sheet, own shader. */
    public static final RenderPipeline FX_ATLAS_ENERGY = enhanced("fx_energy", true, 4);

    /** Beam with a feathered cross section, own shader. */
    public static final RenderPipeline FX_BEAM = enhanced("fx_beam", false, 0);

    /**
     * Builds an enhanced pipeline on top of the vanilla entity snippet.
     *
     * @param fragmentShader name of the fragment shader in {@code assets/techguns3/shaders/core}
     * @param softEdge       feather the border of the sprite out
     * @param atlasGrid      cells per row and per column of the sprite sheet, 0 = no sheet
     */
    private static RenderPipeline enhanced(String fragmentShader, boolean softEdge, int atlasGrid) {
        RenderPipeline.Builder builder = RenderPipeline.builder(
                        RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET,
                        RenderPipelines.EYES_SNIPPET)
                .withVertexShader(Identifier.fromNamespaceAndPath(
                        TechGuns3.MODID, "core/fx_common"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(
                        TechGuns3.MODID, "core/" + fragmentShader))
                .withLocation(Identifier.fromNamespaceAndPath(
                        TechGuns3.MODID, "pipeline/fx_" + fragmentShader + "_" + atlasGrid))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withCull(false);
        if (softEdge) {
            builder.withShaderDefine("SOFT_EDGE");
        }
        if (atlasGrid > 0) {
            builder.withShaderDefine("ATLAS_GRID", atlasGrid);
        }
        return builder.build();
    }

    private static final Map<String, RenderType> TEXTURED = new ConcurrentHashMap<>();

    /** Additive textured render type for the given texture (vanilla shader). */
    public static RenderType additive(Identifier texture) {
        return TEXTURED.computeIfAbsent("legacy:" + texture, tex -> RenderType.create(
                "techguns3_fx_additive",
                RenderSetup.builder(ADDITIVE_TEXTURED)
                        .withTexture("Sampler0", texture)
                        .setTextureTransform(TextureTransform.DEFAULT_TEXTURING)
                        .sortOnUpload()
                        .createRenderSetup()));
    }

    /**
     * Additive textured render type for the given texture, drawn with the own shader.
     *
     * @param style how the sprite is drawn
     * @param texture the sprite sheet
     */
    public static RenderType styled(Style style, Identifier texture) {
        if (!TGConfig.enhancedFx()) {
            return additive(texture);
        }
        return TEXTURED.computeIfAbsent(style.name() + ":" + texture, key -> {
            RenderPipeline pipeline = switch (style) {
                case SPRITE -> FX_SPRITE;
                case GLOW -> FX_GLOW;
                case ATLAS_GLOW -> FX_ATLAS_GLOW;
                case ATLAS_ENERGY -> FX_ATLAS_ENERGY;
                case BEAM -> FX_BEAM;
            };
            return RenderType.create(
                    "techguns3_fx_" + style.name().toLowerCase(java.util.Locale.ROOT),
                    RenderSetup.builder(pipeline)
                            .withTexture("Sampler0", texture)
                            .setTextureTransform(TextureTransform.DEFAULT_TEXTURING)
                            .sortOnUpload()
                            .createRenderSetup());
        });
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
        event.registerPipeline(FX_SPRITE);
        event.registerPipeline(FX_GLOW);
        event.registerPipeline(FX_ATLAS_GLOW);
        event.registerPipeline(FX_ATLAS_ENERGY);
        event.registerPipeline(FX_BEAM);
    }
}
