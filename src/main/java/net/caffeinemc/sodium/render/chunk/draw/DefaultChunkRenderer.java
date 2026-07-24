package net.caffeinemc.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttribute;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.buffer.GlAbstractBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.StreamingBuffer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.sequence.SequenceIndexBuffer;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.TextureUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.List;

/**
 * GL 3.3 compatible chunk renderer.
 * Uses instanced vertex attributes (divisor) instead of gl_BaseInstance for per-chunk translations.
 * This fixes the block invisible issue on GL 3.3 where gl_BaseInstance is always 0.
 */
public class DefaultChunkRenderer extends AbstractChunkRenderer {
    private final Pipeline<ChunkShaderInterface, BufferTarget> pipeline;
    private final Program<ChunkShaderInterface> program;

    private final StreamingBuffer bufferCameraMatrices;
    private final StreamingBuffer bufferFogParameters;
    private final SequenceIndexBuffer indexBuffer;

    // Instance buffer layout: 16 bytes per instance (x,y,z + 4 padding)
    private static final int INSTANCE_STRIDE = 16;

    public DefaultChunkRenderer(RenderDevice device, SequenceIndexBuffer indexBuffer, TerrainVertexType vertexType, ChunkRenderPass pass) {
        super(device, vertexType);

        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;

        this.bufferCameraMatrices = new StreamingBuffer(device, 192, maxInFlightFrames);
        this.bufferFogParameters = new StreamingBuffer(device, 32, maxInFlightFrames);

        this.indexBuffer = indexBuffer;

        var vertexFormat = vertexType.getCustomVertexFormat();

        // Build VAO with the 4 standard terrain attributes PLUS a 5th instanced attribute for chunk translation
        // Location 4 = instanced chunk translation (vec3 float, divisor=1)
        var vertexArray = new VertexArrayDescription<>(BufferTarget.values(), List.of(
                new VertexArrayResourceBinding<>(BufferTarget.VERTICES, new VertexAttributeBinding[] {
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION,
                                vertexFormat.getAttribute(TerrainMeshAttribute.POSITION)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                vertexFormat.getAttribute(TerrainMeshAttribute.COLOR)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                vertexFormat.getAttribute(TerrainMeshAttribute.BLOCK_TEXTURE)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                vertexFormat.getAttribute(TerrainMeshAttribute.LIGHT_TEXTURE)),
                        // Location 4: instanced chunk translation (vec3 float, stride 16)
                        new VertexAttributeBinding(4, new VertexAttribute(
                                VertexAttributeFormat.FLOAT, 3, false, 0, false))
                })
        ));

        var constants = getShaderConstants(pass, this.vertexType);

        var vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.vert"), constants);
        var fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "terrain/terrain_opaque.frag"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .build();

        this.program = this.device.createProgram(desc, ChunkShaderInterface::new);
        this.pipeline = this.device.createPipeline(pass.pipelineDescription(), this.program, vertexArray);
    }

    @Override
    public void render(ChunkPrep.PreparedRenderList lists, ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        this.indexBuffer.ensureCapacity(lists.largestVertexIndex());

        this.device.usePipeline(this.pipeline, (cmd, programInterface, pipelineState) -> {
            this.setupTextures(renderPass, pipelineState);
            this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

            cmd.bindCommandBuffer(lists.commandBuffer());
            cmd.bindElementBuffer(this.indexBuffer.getBuffer());

            int instanceBufferHandle = GlAbstractBuffer.handle(lists.instanceBuffer());
            int vaoHandle = GlVertexArray.handle(pipeline.getVertexArray());

            for (var batch : lists.batches()) {
                // Bind standard vertex buffer
                cmd.bindVertexBuffer(BufferTarget.VERTICES, batch.vertexBuffer(), 0, batch.vertexStride());

                // Bind the instance data buffer as an instanced attribute at location 4
                // This replaces the UBO-based instance transform lookup that required gl_BaseInstance
                GL30C.glBindVertexArray(vaoHandle);

                // Bind instance buffer to location 4 with divisor=1 (per-instance)
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, instanceBufferHandle);
                GL20C.glVertexAttribPointer(4, 3, GL11C.GL_FLOAT, false, INSTANCE_STRIDE, batch.instanceData().offset());
                GL20C.glVertexAttribDivisor(4, 1);
                GL20C.glEnableVertexAttribArray(4);

                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
                GL30C.glBindVertexArray(0);

                // Draw all chunks in this batch using instanced indirect draw
                cmd.multiDrawElementsIndirect(PrimitiveType.TRIANGLES, ElementFormat.UNSIGNED_INT, batch.commandData().offset(), batch.commandCount());

                // Disable the instanced attribute after drawing
                GL30C.glBindVertexArray(vaoHandle);
                GL20C.glDisableVertexAttribArray(4);
                GL30C.glBindVertexArray(0);
            }
        });
    }

    private void setupTextures(ChunkRenderPass pass, PipelineState pipelineState) {
        pipelineState.bindTexture(0, TextureUtil.getBlockAtlasTexture(), pass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
        pipelineState.bindTexture(1, TextureUtil.getLightTexture(), this.lightTextureSampler);
    }

    private void setupUniforms(ChunkRenderMatrices renderMatrices, ChunkShaderInterface programInterface, PipelineState state, int frameIndex) {
        var matrices = this.bufferCameraMatrices.slice(frameIndex);
        var matricesBuf = matrices.view();

        renderMatrices.projection()
                .get(0, matricesBuf);
        renderMatrices.modelView()
                .get(64, matricesBuf);

        var mvpMatrix = new Matrix4f();
        mvpMatrix.set(renderMatrices.projection());
        mvpMatrix.mul(renderMatrices.modelView());
        mvpMatrix
                .get(128, matricesBuf);

        state.bindUniformBlock(programInterface.uniformCameraMatrices, matrices.buffer(), matrices.offset(), matrices.length());

        var fogParams = this.bufferFogParameters.slice(frameIndex);
        var fogParamsBuf = fogParams.view();

        var paramFogColor = RenderSystem.getShaderFogColor();
        fogParamsBuf.putFloat(0, paramFogColor[0]);
        fogParamsBuf.putFloat(4, paramFogColor[1]);
        fogParamsBuf.putFloat(8, paramFogColor[2]);
        fogParamsBuf.putFloat(12, paramFogColor[3]);
        fogParamsBuf.putFloat(16, RenderSystem.getShaderFogStart());
        fogParamsBuf.putFloat(20, RenderSystem.getShaderFogEnd());
        fogParamsBuf.putInt(24, RenderSystem.getShaderFogShape().getId());

        state.bindUniformBlock(programInterface.uniformFogParameters, fogParams.buffer(), fogParams.offset(), fogParams.length());
    }

    private static ShaderConstants getShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();

        if (pass.isCutout()) {
            constants.add("ALPHA_CUTOFF", String.valueOf(pass.alphaCutoff()));
        }

        if (!MathHelper.approximatelyEquals(vertexType.getVertexRange(), 1.0f)) {
            constants.add("VERT_SCALE", String.valueOf(vertexType.getVertexRange()));
        }

        return constants.build();
    }

    @Override
    public void delete() {
        super.delete();

        this.device.deletePipeline(this.pipeline);
        this.device.deleteProgram(this.program);

        this.bufferFogParameters.delete();
        this.bufferCameraMatrices.delete();
    }

    public enum BufferTarget {
        VERTICES
    }
}
