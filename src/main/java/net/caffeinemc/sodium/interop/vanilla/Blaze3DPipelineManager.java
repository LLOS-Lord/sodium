package net.caffeinemc.sodium.interop.vanilla;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.pipeline.state.CullMode;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.opengl.buffer.GlAbstractBuffer;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.caffeinemc.gfx.opengl.texture.GlSampler;
import net.minecraft.client.render.BufferRenderer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL33C;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Consumer;

/**
 * OpenGL 3.3 compatible pipeline manager.
 * Replaces GL 4.3+ glBindTextureUnit/glBindSampler with GL 3.3 compatible equivalents.
 */
public class Blaze3DPipelineManager implements GlPipelineManager {
    private final Blaze3DPipelineState state = new Blaze3DPipelineState();

    @Override
    public <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate) {
        BufferRenderer.vertexFormat = null;

        GL20C.glUseProgram(GlProgram.getHandle(pipeline.getProgram()));
        GL30C.glBindVertexArray(GlVertexArray.handle(pipeline.getVertexArray()));

        setRenderState(pipeline.getDescription());

        try {
            gate.accept(this.state);
        } finally {
            this.state.restore();
        }

        unsetRenderState(pipeline.getDescription());
    }

    private static void setRenderState(PipelineDescription desc) {
        if (desc.cullMode == CullMode.ENABLE) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (desc.blendFunc != null) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlEnum.from(desc.blendFunc.srcRGB), GlEnum.from(desc.blendFunc.dstRGB),
                    GlEnum.from(desc.blendFunc.srcAlpha), GlEnum.from(desc.blendFunc.dstAlpha));
        } else {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        if (!desc.writeMask.depth()) {
            RenderSystem.depthMask(false);
        }

        if (!desc.writeMask.color()) {
            RenderSystem.colorMask(false, false, false, false);
        }

        if (desc.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GlEnum.from(desc.depthFunc));
        } else {
            RenderSystem.disableDepthTest();
        }
    }

    private static void unsetRenderState(PipelineDescription pipelineDescription) {
        if (pipelineDescription.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.depthFunc(GL11C.GL_LEQUAL);
        }

        if (!pipelineDescription.writeMask.depth()) {
            RenderSystem.depthMask(true);
        }

        if (!pipelineDescription.writeMask.color()) {
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    private static class Blaze3DPipelineState implements PipelineState {
        private final int maxTextureUnits;
        private final BitSet changedTextures = new BitSet(32);
        private final int[] boundTextures;
        private final int[] boundSamplers;

        public Blaze3DPipelineState() {
            this.maxTextureUnits = GL11C.glGetInteger(GL11C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            int size = Math.max(this.maxTextureUnits, 32);
            this.boundTextures = new int[size];
            this.boundSamplers = new int[size];
            Arrays.fill(this.boundTextures, 0);
            Arrays.fill(this.boundSamplers, 0);
        }

        @Override
        public void bindTexture(int unit, int texture, Sampler sampler) {
            this.changedTextures.set(unit);

            // GL 3.3 compatible: glActiveTexture + glBindTexture + glBindSampler
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);

            int samplerHandle = GlSampler.handle(sampler);
            GL33C.glBindSampler(unit, samplerHandle);

            if (unit < this.boundTextures.length) {
                this.boundTextures[unit] = texture;
                this.boundSamplers[unit] = samplerHandle;
            }
        }

        @Override
        public void bindUniformBlock(BufferBlock block, Buffer buffer) {
            this.bindUniformBlock(block, buffer, 0, buffer.capacity());
        }

        @Override
        public void bindUniformBlock(BufferBlock block, Buffer buffer, long offset, long length) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.isTrue(offset >= 0, "Offset must be greater-than or equal to zero");
                Validate.isTrue(offset + length <= buffer.capacity(), "Range is out of buffer bounds");
            }

            GL31C.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, block.index(), GlAbstractBuffer.handle(buffer), offset, length);
        }

        public void restore() {
            for (int unit = this.changedTextures.nextSetBit(0); unit != -1; unit = this.changedTextures.nextSetBit(unit + 1)) {
                GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);

                int vanillaTexture = 0;
                if (unit < GlStateManager.TEXTURES.length) {
                    vanillaTexture = GlStateManager.TEXTURES[unit].boundTexture;
                }
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, vanillaTexture);
                GL33C.glBindSampler(unit, 0);
            }

            this.changedTextures.clear();
        }
    }
}
