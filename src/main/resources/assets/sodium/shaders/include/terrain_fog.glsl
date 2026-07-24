// Fog parameters UBO at binding 1 (shifted from 2 since instance data UBO was removed)
layout(std140, binding = 1) uniform FogParameters {
    vec4 fog_color;
    float fog_start;
    float fog_end;
    int fog_mode;
};
