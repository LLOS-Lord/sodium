package net.caffeinemc.gfx.opengl.array;

import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.List;

/**
 * OpenGL 3.3 compatible VAO wrapper.
 * Uses non-DSA VAO APIs instead of GL 4.5+ DSA APIs.
 */
public class GlVertexArray<T extends Enum<T>> extends GlObject implements VertexArray<T> {
    private final VertexArrayDescription<T> desc;

    public GlVertexArray(int id, VertexArrayDescription<T> desc) {
        this.setHandle(id);

        // Bind VAO for GL 3.3 non-DSA attribute setup
        GL30C.glBindVertexArray(id);
        this.setAttributeBindings(desc.vertexBindings());
        GL30C.glBindVertexArray(0);

        this.desc = desc;
    }

    private void setAttributeBindings(List<VertexArrayResourceBinding<T>> bindings) {
        for (var bufferIndex = 0; bufferIndex < bindings.size(); bufferIndex++) {
            var bufferBinding = bindings.get(bufferIndex);

            for (var binding : bufferBinding.attributeBindings()) {
                var attrib = binding.attribute();
                var index = binding.index();

                if (attrib.intType()) {
                    GL30C.glVertexAttribIPointer(index,
                            attrib.count(), GlEnum.from(attrib.format()), attrib.offset());
                } else {
                    GL20C.glVertexAttribPointer(index,
                            attrib.count(), GlEnum.from(attrib.format()), attrib.normalized(), attrib.offset());
                }

                GL20C.glEnableVertexAttribArray(index);
            }
        }
    }

    @Override
    public T[] getBufferTargets() {
        return this.desc.targets();
    }

    public static int handle(VertexArray<?> array) {
        return ((GlVertexArray<?>) array).handle();
    }
}
