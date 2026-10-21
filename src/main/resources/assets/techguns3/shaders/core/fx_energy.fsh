#version 330
#extension GL_ARB_separate_shader_objects : require

// Energy effect: an animated sprite that flows like a plasma.
//
// The flow is built from sine waves of the game time instead of a texture
// animation, so it never repeats in a visible pattern and it stays smooth at
// any frame rate. GameTime is (gameTime + partialTick) / 24000, so it is
// fractional and continuous; multiplying it by an integer keeps the wave
// continuous across the wrap back to zero.
//
// The waves run over the cell of the sprite sheet that is drawn, not over the
// whole sheet, so a sprite that changes its cell does not jump.

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:globals.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 6) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    color *= vertexColor * ColorModulator;

    vec2 local = texCoord0;
#ifdef ATLAS_GRID
    local = fract(texCoord0 * float(ATLAS_GRID));
#endif

    float time = GameTime * 6.2831853;

    // Two travelling waves at different speeds and directions.
    float waveA = sin(time * 3.0 + local.y * 9.0) * 0.5 + 0.5;
    float waveB = sin(time * 2.0 - local.x * 7.0) * 0.5 + 0.5;
    float flow = mix(0.55, 1.45, waveA * 0.6 + waveB * 0.4);
    color.rgb *= flow;

#ifdef SOFT_EDGE
    vec2 offset = local * 2.0 - 1.0;
    float falloff = clamp(1.0 - dot(offset, offset), 0.0, 1.0);
    color.a *= falloff * falloff;
#endif

    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance,
                                FogEnvironmentalStart, FogEnvironmentalEnd,
                                FogRenderDistanceStart, FogRenderDistanceEnd);
    color *= (1.0 - fog);

    fragColor = color;
}
