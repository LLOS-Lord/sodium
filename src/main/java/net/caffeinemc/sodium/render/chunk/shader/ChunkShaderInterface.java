package net.caffeinemc.sodium.render.chunk.shader;

import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.BufferBlock;

/**
 * A forward-rendering shader program for chunks.
 * GL 3.3 compatible: removed uniformInstanceData (binding 1) since we now use
 * instanced vertex attributes instead of UBO-based instance transforms.
 */
public class ChunkShaderInterface {
    public final BufferBlock uniformCameraMatrices;
    public final BufferBlock uniformFogParameters;

    public ChunkShaderInterface(ShaderBindingContext context) {
        this.uniformCameraMatrices = context.bindUniformBlock(0);
        this.uniformFogParameters = context.bindUniformBlock(1);
    }
}
