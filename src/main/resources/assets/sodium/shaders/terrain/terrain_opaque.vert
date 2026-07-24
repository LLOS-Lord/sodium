#version 330 core

#import <sodium:include/terrain_fog.vert>
#import <sodium:include/terrain_draw.vert>
#import <sodium:include/terrain_view.vert>
#import <sodium:include/terrain_format.vert>

// Per-instance chunk translation passed as an instanced vertex attribute (location 4)
// This replaces gl_BaseInstance for GL 3.3 compatibility
layout(location = 4) in vec3 in_instance_offset;

out VertexOutput {
    vec3 color;
    float shade;

    vec2 tex_diffuse_coord;
    vec2 tex_light_coord;

    float fog_depth;
} vs_out;

void main() {
    _vert_init();

    // Local space -> View space using per-instance attribute
    vec3 view_position = _apply_view_transform_alt(_vert_position, in_instance_offset);

    // View space -> Clip space
    gl_Position = mat_modelviewproj * vec4(view_position, 1.0);

    // Unpack the vertex color and shade values
    vs_out.color = _vert_color_shade.rgb;
    vs_out.shade = _vert_color_shade.a;

    // Pass the texture coordinates verbatim
    vs_out.tex_diffuse_coord = _vert_tex_diffuse_coord;
    vs_out.tex_light_coord = _vert_tex_light_coord;

    // The distance of the vertex from the camera is just the view-space coordinate
    vs_out.fog_depth = _get_fog_depth(view_position);
}
