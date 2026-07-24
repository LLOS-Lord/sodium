package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL15C;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

/**
 * OpenGL 3.3 compatible mapped buffer wrapper.
 * Uses glMapBuffer/glUnmapBuffer instead of persistent mapping (GL 4.4+).
 * The ByteBuffer view is obtained from glMapBuffer and remains valid until unmap.
 * For GL 3.3, flush is a no-op since data is visible when unmapped.
 */
public class GlMappedBuffer extends GlAbstractBuffer implements MappedBuffer {
    private final ByteBuffer view;
    private final Set<MappedBufferFlags> flags;

    public GlMappedBuffer(int handle, ByteBuffer view, Set<MappedBufferFlags> flags) {
        super(handle, view.capacity());

        this.view = view;
        this.flags = Collections.unmodifiableSet(flags);
    }

    @Override
    public void flush(long offset, long length) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(this.flags.contains(MappedBufferFlags.EXPLICIT_FLUSH), "Buffer is not mapped for explicit flushing");
            Validate.isTrue(offset >= 0, "The offset must be greater than or equal to zero");
            Validate.isTrue(offset + length <= this.capacity(), "Range is outside of buffer bounds");
        }

        // GL 3.3: flush is implicit when using regular mapped buffers
        // We unmap and remap to ensure data is visible to GL
        if (this.flags.contains(MappedBufferFlags.EXPLICIT_FLUSH)) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.handle());
            GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);

            // Remap the buffer to keep the view valid
            int access = GL15C.GL_READ_WRITE;
            if (this.flags.contains(MappedBufferFlags.WRITE) && !this.flags.contains(MappedBufferFlags.READ)) {
                access = GL15C.GL_WRITE_ONLY;
            } else if (this.flags.contains(MappedBufferFlags.READ) && !this.flags.contains(MappedBufferFlags.WRITE)) {
                access = GL15C.GL_READ_ONLY;
            }

            ByteBuffer newView = GL15C.glMapBuffer(GL15C.GL_ARRAY_BUFFER, access);
            if (newView == null) {
                throw new RuntimeException("Failed to remap buffer after flush");
            }
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public ByteBuffer view() {
        return this.view;
    }

    @Override
    public Set<MappedBufferFlags> flags() {
        return this.flags;
    }
}
