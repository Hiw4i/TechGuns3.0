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
    public static final RenderPipeline FX_GLOW = enhanced("fx_sprite", true, 0);

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
                        TechGuns3.MODID, "pipeline/fx_" + fragmentShader
                                + (softEdge ? "_soft" : "") + (atlasGrid > 0 ? "_atlas" + atlasGrid : "")))
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

    /** Cached Iris shader-pack check, re-queried twice a second. */
    private static boolean shaderPackInUse;
    private static long shaderPackCheckedAt;
    /** True once the enhanced pipelines were handed to the pack's programs. */
    private static volatile boolean packProgramsAssigned;

    /**
     * True while the effects may render. Inside the shadow pass of a shader
     * pack they are skipped: a muzzle flash is a light, not something that
     * blocks light.
     */
    public static boolean renderFxNow() {
        if (!net.neoforged.fml.ModList.get().isLoaded("iris")) return true;
        try {
            return !IrisFxBridge.renderingShadowPass();
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * True while an Iris shader pack is rendering the world.
     *
     * <p>The enhanced pipelines carry own GLSL, which a shader pack knows
     * nothing about: it would not receive the pack's lighting and fog. The
     * vanilla-shader family keeps every effect alive under a pack, so the
     * render types switch to it while a pack is active and back when it is
     * turned off.</p>
     */
    private static boolean shaderPackInUse() {
        long now = System.nanoTime();
        if (shaderPackCheckedAt == 0L || now - shaderPackCheckedAt > 500_000_000L) {
            boolean wasInUse = shaderPackInUse;
            shaderPackCheckedAt = now;
            shaderPackInUse = detectShaderPackInUse();
            if (shaderPackInUse != wasInUse) {
                TechGuns3.LOGGER.info("[TechGuns3] shader pack {}: using the {} FX path",
                        shaderPackInUse ? "active" : "off",
                        shaderPackInUse ? "vanilla-shader" : "enhanced");
            }
        }
        return shaderPackInUse;
    }

    private static boolean detectShaderPackInUse() {
        if (!net.neoforged.fml.ModList.get().isLoaded("iris")) {
            return false;
        }
        try {
            return IrisFxBridge.shaderPackInUse();
        } catch (Throwable t) {
            // Iris is installed but its API is not the expected shape: play
            // safe and draw with the vanilla-shader family.
            TechGuns3.LOGGER.debug("[TechGuns3] Iris API unavailable, assuming a shader pack is active", t);
            return true;
        }
    }

    /**
     * Hands the enhanced pipelines to the pack's programs once, so the pack's
     * lighting, fog and bloom apply to the effects. Failures leave the flag
     * off and the render types fall back to the vanilla-shader family.
     */
    private static void tryAssignPackPrograms() {
        if (packProgramsAssigned || !TGConfig.irisPackPrograms()) return;
        if (!net.neoforged.fml.ModList.get().isLoaded("iris")) return;
        try {
            IrisFxBridge.assignEnhancedPipelines(
                    FX_SPRITE, FX_GLOW, FX_ATLAS_GLOW, FX_ATLAS_ENERGY, FX_BEAM);
            packProgramsAssigned = true;
            TechGuns3.LOGGER.info("[TechGuns3] the enhanced FX pipelines follow the shader pack's lighting");
        } catch (Throwable t) {
            TechGuns3.LOGGER.info("[TechGuns3] the pack program hand-off failed, effects "
                    + "draw with the vanilla-shader path under a pack", t);
        }
    }

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
     * <p>While an Iris shader pack is active the vanilla-shader family is used
     * instead, whatever {@code enhancedFx} says: the pack cannot light or fog
     * the own GLSL.</p>
     *
     * @param style how the sprite is drawn
     * @param texture the sprite sheet
     */
    public static RenderType styled(Style style, Identifier texture) {
        boolean packCovered = shaderPackInUse() && packProgramsAssigned && TGConfig.irisPackPrograms();
        if (!TGConfig.enhancedFx() || (shaderPackInUse() && !packCovered)) {
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
        tryAssignPackPrograms();
    }
}
