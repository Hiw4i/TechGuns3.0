package com.techguns.techguns3.client.fx;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

/**
 * Direct calls into the Iris public API (v0).
 *
 * <p>This class is only loaded when Iris is actually installed —
 * {@link FxRenderTypes} guards every call with a {@code ModList} check and a
 * {@code Throwable} catch, so a machine without Iris never resolves the
 * {@code IrisApi} references. The API classes themselves come from the
 * compile-only {@code libs/iris-api} jar and are never packaged.</p>
 */
final class IrisFxBridge {
    private IrisFxBridge() {}

    /** True while an Iris shader pack is rendering the world. */
    static boolean shaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    /** True inside the shadow pass of a shader pack (pack shadows are being drawn). */
    static boolean renderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    /**
     * Hands the enhanced pipelines to the pack: Iris binds its own program for
     * the slot while the pipeline keeps its vertex format and blend, so the
     * pack's lighting, fog and bloom apply to the effects.
     */
    static void assignEnhancedPipelines(RenderPipeline sprite, RenderPipeline glow,
                                        RenderPipeline atlasGlow, RenderPipeline atlasEnergy,
                                        RenderPipeline beam) {
        IrisApi api = IrisApi.getInstance();
        api.assignPipeline(sprite, IrisProgram.ENTITIES_TRANSLUCENT);
        api.assignPipeline(glow, IrisProgram.ENTITIES_TRANSLUCENT);
        api.assignPipeline(atlasGlow, IrisProgram.ENTITIES_TRANSLUCENT);
        api.assignPipeline(atlasEnergy, IrisProgram.ENTITIES_TRANSLUCENT);
        api.assignPipeline(beam, IrisProgram.ENTITIES_TRANSLUCENT);
    }
}
