#version 330
#extension GL_ARB_separate_shader_objects : require

// Shared vertex stage for every Techguns effect pipeline.
//
// It follows the vanilla "core/entity" layout (DefaultVertexFormat.ENTITY),
// but drops the lighting and the overlay: Techguns effects are emissive and
// have no damage tint. Keeping the exact same attribute locations means the
// existing FxGeometry helpers can feed these pipelines unchanged.
//
// Compiled by the game through shaderc into SPIR-V, so the exact same source
// runs on the Vulkan and on the OpenGL backend.

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 6) out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    vertexColor = Color;
    texCoord0 = UV0;
}
