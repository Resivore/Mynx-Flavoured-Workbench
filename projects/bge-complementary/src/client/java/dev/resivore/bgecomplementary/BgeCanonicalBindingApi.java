package dev.resivore.bgecomplementary;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fail-closed capability check for the authoritative BGE canonical-binding contract consumed by
 * {@link BgeShaderMaterialBridge}. It intentionally has no BGE release-string knowledge.
 */
public final class BgeCanonicalBindingApi {
    private static final String BINDINGS = "dev.aero.cnmterraincompat.BgeMaterialBindings";
    private static final String BINDING = BINDINGS + "$Binding";

    private BgeCanonicalBindingApi() {}

    public static boolean isAvailable() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader fallback = BgeCanonicalBindingApi.class.getClassLoader();
        return supports(context) || (fallback != context && supports(fallback));
    }

    static boolean supports(ClassLoader classLoader) {
        if (classLoader == null) return false;
        try {
            Class<?> bindings = Class.forName(BINDINGS, false, classLoader);
            Class<?> binding = Class.forName(BINDING, false, classLoader);
            return hasStaticMethod(bindings, "all", List.class)
                    && hasInstanceMethod(binding, "physicalBlock", Block.class)
                    && hasInstanceMethod(binding, "canonicalMaterial", Block.class)
                    && hasInstanceMethod(binding, "canonicalState", Optional.class, BlockState.class);
        } catch (ClassNotFoundException | LinkageError | SecurityException ignored) {
            return false;
        }
    }

    private static boolean hasStaticMethod(Class<?> owner, String name, Class<?> returnType,
                                           Class<?>... parameters) {
        return hasMethod(owner, name, returnType, true, parameters);
    }

    private static boolean hasInstanceMethod(Class<?> owner, String name, Class<?> returnType,
                                             Class<?>... parameters) {
        return hasMethod(owner, name, returnType, false, parameters);
    }

    private static boolean hasMethod(Class<?> owner, String name, Class<?> returnType,
                                     boolean isStatic, Class<?>... parameters) {
        try {
            Method method = owner.getMethod(name, parameters);
            return method.getReturnType() == returnType
                    && Modifier.isStatic(method.getModifiers()) == isStatic;
        } catch (NoSuchMethodException | SecurityException ignored) {
            return false;
        }
    }
}
