#version 330
#extension GL_ARB_separate_shader_objects : require

// Additive effect sprite.
//
// Two things separate this from the vanilla "core/entity" shader:
//
//   * Additive light is dimmed by the fog instead of being tinted by it.
//     The vanilla entity shader mixes the colour towards the fog colour,
//     which for an additive blend adds a grey haze to every distant tracer.
//   * An optional soft radial edge. A sprite that covers the whole quad ends
//     in a hard square, a glow that fades out towards the border of the quad
//     reads as light instead of as a sticker.
//
// The soft edge works on atlas sprites as well: ATLAS_GRID is the number of
// cells per row and per column of the sprite sheet, the edge is then evaluated
// inside the cell that is actually drawn.

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 6) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    color *= vertexColor * ColorModulator;

#ifdef SOFT_EDGE
    vec2 local = texCoord0;
#ifdef ATLAS_GRID
    local = fract(texCoord0 * float(ATLAS_GRID));
#endif
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
