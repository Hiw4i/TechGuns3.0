package com.techguns.techguns3.client.fx;

import net.irisshaders.iris.api.v0.IrisApi;

/**
 * Direct calls into the Iris public API (v0).
 *
 * <p>This class is only loaded when Iris is actually installed —
 * {@link FxRenderTypes} guards the call with a {@code ModList} check and a
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
}
