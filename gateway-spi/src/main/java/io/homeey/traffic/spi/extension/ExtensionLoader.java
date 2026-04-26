package io.homeey.traffic.spi.extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExtensionLoader<T> {

    private static final String SPI_BASE = "META-INF/gateway/";

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final ConcurrentMap<String, T> singletons = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return (ExtensionLoader<T>) LOADERS.computeIfAbsent(type, ExtensionLoader::new);
    }

    public T getExtension(String name) {
        return singletons.computeIfAbsent(name, this::loadExtension);
    }

    public T getDefaultExtension() {
        SPI spi = type.getAnnotation(SPI.class);
        if (spi == null) {
            throw new IllegalStateException(type.getName() + " is not an SPI interface");
        }
        String defaultValue = spi.value();
        if (defaultValue.isEmpty()) {
            throw new IllegalStateException(type.getName() + " has no default extension");
        }
        return getExtension(defaultValue);
    }

    private T loadExtension(String name) {
        try {
            String fileName = SPI_BASE + type.getName();
            List<String> implClassNames = readSpiConfig(fileName);
            for (String implClassName : implClassNames) {
                Class<?> implClass = Class.forName(implClassName);
                Activate activate = implClass.getAnnotation(Activate.class);
                if (activate != null) {
                    Class<?>[] interfaces = implClass.getInterfaces();
                    for (Class<?> iface : interfaces) {
                        if (iface == type) {
                            @SuppressWarnings("unchecked")
                            T instance = (T) implClass.getDeclaredConstructor().newInstance();
                            return instance;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load extension: " + name + " for " + type.getName(), e);
        }
        throw new IllegalStateException("Extension not found: " + name + " for " + type.getName());
    }

    private List<String> readSpiConfig(String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ExtensionLoader.class.getClassLoader();
        }
        Enumeration<URL> resources = classLoader.getResources(fileName);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        lines.add(line);
                    }
                }
            }
        }
        return lines;
    }
}
