// GL 3.3 compatible terrain draw transforms.
// Uses per-instance vertex attribute (location 4) instead of gl_BaseInstance
// which requires GL_ARB_shader_draw_parameters (GL 4.3+).

// Apply view transform using per-instance offset attribute
vec3 _apply_view_transform_alt(vec3 position, vec3 instanceOffset) {
    return instanceOffset + position;
}
