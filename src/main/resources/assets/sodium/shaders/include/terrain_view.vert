// Camera matrices UBO at binding 0
layout(std140, binding = 0) uniform CameraMatrices {
    mat4 mat_proj;
    mat4 mat_modelview;
    mat4 mat_modelviewproj;
};
