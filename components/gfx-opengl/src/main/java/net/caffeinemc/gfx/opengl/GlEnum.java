package net.caffeinemc.gfx.opengl;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.opengl.ARBComputeShader;

import java.util.function.Consumer;

/**
 * OpenGL 3.3 compatible enum mapping.
 * All enum values are the same across GL versions, so we use GL11/GL20 constants
 * which are universally available.
 */
public class GlEnum {
    private static final int[] PRIMITIVE_TYPES = build(PrimitiveType.class, (map) -> {
        map.put(PrimitiveType.TRIANGLES,        GL11C.GL_TRIANGLES);
        map.put(PrimitiveType.TRIANGLE_STRIP,   GL11C.GL_TRIANGLE_STRIP);
        map.put(PrimitiveType.TRIANGLE_FAN,     GL11C.GL_TRIANGLE_FAN);
        map.put(PrimitiveType.LINES,            GL11C.GL_LINES);
        map.put(PrimitiveType.LINE_STRIP,       GL11C.GL_LINE_STRIP);
    });

    private static final int[] BLEND_SRC_FACTOR = build(BlendFunc.SrcFactor.class, (map) -> {
        map.put(BlendFunc.SrcFactor.ZERO,                       GL11C.GL_ZERO);
        map.put(BlendFunc.SrcFactor.ONE,                        GL11C.GL_ONE);
        map.put(BlendFunc.SrcFactor.SRC_COLOR,                  GL11C.GL_SRC_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_SRC_COLOR,        GL11C.GL_ONE_MINUS_SRC_COLOR);
        map.put(BlendFunc.SrcFactor.DST_COLOR,                  GL11C.GL_DST_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_DST_COLOR,        GL11C.GL_ONE_MINUS_DST_COLOR);
        map.put(BlendFunc.SrcFactor.SRC_ALPHA,                  GL11C.GL_SRC_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_SRC_ALPHA,        GL11C.GL_ONE_MINUS_SRC_ALPHA);
        map.put(BlendFunc.SrcFactor.DST_ALPHA,                  GL11C.GL_DST_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_DST_ALPHA,        GL11C.GL_ONE_MINUS_DST_ALPHA);
        map.put(BlendFunc.SrcFactor.CONSTANT_COLOR,             GL11C.GL_CONSTANT_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_CONSTANT_COLOR,   GL11C.GL_ONE_MINUS_CONSTANT_COLOR);
        map.put(BlendFunc.SrcFactor.CONSTANT_ALPHA,             GL11C.GL_CONSTANT_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_CONSTANT_ALPHA,   GL11C.GL_ONE_MINUS_CONSTANT_ALPHA);
        map.put(BlendFunc.SrcFactor.SRC_ALPHA_SATURATE,         GL11C.GL_SRC_ALPHA_SATURATE);
    });

    private static final int[] BLEND_DST_FACTOR = build(BlendFunc.DstFactor.class, (map) -> {
        map.put(BlendFunc.DstFactor.ZERO,                       GL11C.GL_ZERO);
        map.put(BlendFunc.DstFactor.ONE,                        GL11C.GL_ONE);
        map.put(BlendFunc.DstFactor.SRC_COLOR,                  GL11C.GL_SRC_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_SRC_COLOR,        GL11C.GL_ONE_MINUS_SRC_COLOR);
        map.put(BlendFunc.DstFactor.DST_COLOR,                  GL11C.GL_DST_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_DST_COLOR,        GL11C.GL_ONE_MINUS_DST_COLOR);
        map.put(BlendFunc.DstFactor.SRC_ALPHA,                  GL11C.GL_SRC_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,        GL11C.GL_ONE_MINUS_SRC_ALPHA);
        map.put(BlendFunc.DstFactor.DST_ALPHA,                  GL11C.GL_DST_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_DST_ALPHA,        GL11C.GL_ONE_MINUS_DST_ALPHA);
        map.put(BlendFunc.DstFactor.CONSTANT_COLOR,             GL11C.GL_CONSTANT_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_CONSTANT_COLOR,   GL11C.GL_ONE_MINUS_CONSTANT_COLOR);
        map.put(BlendFunc.DstFactor.CONSTANT_ALPHA,             GL11C.GL_CONSTANT_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_CONSTANT_ALPHA,   GL11C.GL_ONE_MINUS_CONSTANT_ALPHA);
    });

    private static final int[] INT_TYPE = build(ElementFormat.class, (map) -> {
        map.put(ElementFormat.UNSIGNED_BYTE,  GL11C.GL_UNSIGNED_BYTE);
        map.put(ElementFormat.UNSIGNED_SHORT, GL11C.GL_UNSIGNED_SHORT);
        map.put(ElementFormat.UNSIGNED_INT,   GL11C.GL_UNSIGNED_INT);
    });

    private static final int[] DEPTH_FUNC = build(DepthFunc.class, (map) -> {
        map.put(DepthFunc.NEVER,                    GL11C.GL_NEVER);
        map.put(DepthFunc.LESS,                     GL11C.GL_LESS);
        map.put(DepthFunc.LESS_THAN_OR_EQUAL,       GL11C.GL_LEQUAL);
        map.put(DepthFunc.EQUAL,                    GL11C.GL_EQUAL);
        map.put(DepthFunc.NOT_EQUAL,                GL11C.GL_NOTEQUAL);
        map.put(DepthFunc.GREATER,                  GL11C.GL_GREATER);
        map.put(DepthFunc.GREATER_THAN_OR_EQUAL,    GL11C.GL_GEQUAL);
        map.put(DepthFunc.ALWAYS,                   GL11C.GL_ALWAYS);
    });

    private static final int[] ATTRIBUTE_FORMAT = build(VertexAttributeFormat.class, (map) -> {
        map.put(VertexAttributeFormat.FLOAT,            GL11C.GL_FLOAT);
        map.put(VertexAttributeFormat.BYTE,             GL11C.GL_BYTE);
        map.put(VertexAttributeFormat.UNSIGNED_BYTE,    GL11C.GL_UNSIGNED_BYTE);
        map.put(VertexAttributeFormat.SHORT,            GL11C.GL_SHORT);
        map.put(VertexAttributeFormat.UNSIGNED_SHORT,   GL11C.GL_UNSIGNED_SHORT);
        map.put(VertexAttributeFormat.INT,              GL11C.GL_INT);
        map.put(VertexAttributeFormat.UNSIGNED_INT,     GL11C.GL_UNSIGNED_INT);
    });

    private static final int[] SHADER_TYPES = build(ShaderType.class, (map) -> {
        map.put(ShaderType.VERTEX,                  GL20C.GL_VERTEX_SHADER);
        map.put(ShaderType.FRAGMENT,                GL20C.GL_FRAGMENT_SHADER);
        map.put(ShaderType.GEOMETRY,                GL32C.GL_GEOMETRY_SHADER);
        map.put(ShaderType.COMPUTE,                 ARBComputeShader.GL_COMPUTE_SHADER);
        map.put(ShaderType.TESSELLATION_CONTROL,    GL40C.GL_TESS_CONTROL_SHADER);
        map.put(ShaderType.TESSELLATION_EVALUATION, GL40C.GL_TESS_EVALUATION_SHADER);
    });

    public static int from(PrimitiveType value) {
        return PRIMITIVE_TYPES[value.ordinal()];
    }

    public static int from(BlendFunc.SrcFactor value) {
        return BLEND_SRC_FACTOR[value.ordinal()];
    }

    public static int from(BlendFunc.DstFactor value) {
        return BLEND_DST_FACTOR[value.ordinal()];
    }

    public static int from(ElementFormat value) {
        return INT_TYPE[value.ordinal()];
    }

    public static int from(DepthFunc value) {
        return DEPTH_FUNC[value.ordinal()];
    }

    public static int from(VertexAttributeFormat value) {
        return ATTRIBUTE_FORMAT[value.ordinal()];
    }

    public static int from(ShaderType value) {
        return SHADER_TYPES[value.ordinal()];
    }

    private static <T extends Enum<T>> int[] build(Class<T> type, Consumer<Reference2IntMap<T>> consumer) {
        Enum<T>[] universe = type.getEnumConstants();

        Reference2IntMap<T> map = new Reference2IntOpenHashMap<>(universe.length);
        map.defaultReturnValue(-1);

        consumer.accept(map);

        int[] values = new int[universe.length];

        for (Enum<T> e : universe) {
            int value = map.getInt(e);

            if (value == -1) {
                throw new RuntimeException("No mapping defined for " + e.name());
            }

            values[e.ordinal()] = value;
        }

        return values;
    }
}
