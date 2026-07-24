package net.caffeinemc.gfx.opengl.texture;

import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.api.texture.Sampler;
import org.lwjgl.opengl.GL33C;

/**
 * OpenGL 3.3 compatible sampler wrapper.
 * Samplers were introduced in GL 3.3.
 */
public class GlSampler extends GlObject implements Sampler {
    public GlSampler() {
        this.setHandle(GL33C.glGenSamplers());
    }

    @Override
    public void setParameter(int parameter, int value) {
        GL33C.glSamplerParameteri(this.handle(), parameter, value);
    }

    @Override
    public void setParameter(int parameter, float value) {
        GL33C.glSamplerParameterf(this.handle(), parameter, value);
    }

    public static int handle(Sampler sampler) {
        return ((GlSampler) sampler).handle();
    }
}
