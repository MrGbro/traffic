package io.homeey.traffic.runtime.extension;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtensionInstantiator {

    private final ConcurrentMap<ExtensionKey, Object> singletonCache = new ConcurrentHashMap<>();

    public <T> T create(Class<T> type, Class<? extends T> implClass, Object... args) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(implClass, "implClass must not be null");

        Object[] actualArgs = args == null ? new Object[0] : args;
        if (actualArgs.length == 0) {
            ExtensionKey key = new ExtensionKey(type, implClass);
            Object instance = singletonCache.computeIfAbsent(key, ignored -> instantiate(implClass));
            return type.cast(instance);
        }

        return type.cast(instantiate(implClass, actualArgs));
    }

    private Object instantiate(Class<?> implClass, Object... args) {
        try {
            Constructor<?> constructor = findMatchingConstructor(implClass, args);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + implClass.getName()
                    + " with args " + Arrays.toString(argTypes(args)), e);
        }
    }

    private Constructor<?> findMatchingConstructor(Class<?> implClass, Object[] args) {
        for (Constructor<?> constructor : implClass.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isCompatible(paramTypes[i], args[i])) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return constructor;
            }
        }
        throw new IllegalStateException("No matching constructor found for " + implClass.getName()
                + " with args " + Arrays.toString(argTypes(args)));
    }

    private boolean isCompatible(Class<?> paramType, Object arg) {
        if (arg == null) {
            return !paramType.isPrimitive();
        }
        Class<?> wrappedParam = wrap(paramType);
        return wrappedParam.isAssignableFrom(arg.getClass());
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        Map<Class<?>, Class<?>> wrapperMap = Map.of(
                boolean.class, Boolean.class,
                byte.class, Byte.class,
                short.class, Short.class,
                int.class, Integer.class,
                long.class, Long.class,
                float.class, Float.class,
                double.class, Double.class,
                char.class, Character.class
        );
        return wrapperMap.get(type);
    }

    private Class<?>[] argTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? Object.class : args[i].getClass();
        }
        return types;
    }

    private record ExtensionKey(Class<?> type, Class<?> implClass) {
    }
}
