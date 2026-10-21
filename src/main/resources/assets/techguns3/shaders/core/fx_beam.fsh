#version 330
#extension GL_ARB_separate_shader_objects : require

// Beam: a textured ribbon that fades out towards its edges.
//
// The geometry of a beam puts the transverse coordinate in U and the length
// of the beam in V, so the cross section of the tube can be derived from U
// alone. Only the outer part of the profile is feathered, the middle of the
// beam keeps the brightness of the texture.

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

    float cross = abs(texCoord0.x * 2.0 - 1.0);
    float profile = smoothstep(1.0, 0.72, cross);
    color.a *= profile;
    color.rgb *= mix(0.7, 1.0, profile);

    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance,
                                FogEnvironmentalStart, FogEnvironmentalEnd,
                                FogRenderDistanceStart, FogRenderDistanceEnd);
    color *= (1.0 - fog);

    fragColor = color;
}
