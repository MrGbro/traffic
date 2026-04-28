package io.homeey.traffic.spi.extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExtensionLoader<T> {

    private static final String SPI_BASE = "META-INF/gateway/";

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();

    private final Class<T> type;
    private volatile Map<String, Class<? extends T>> extensionClasses;

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return (ExtensionLoader<T>) LOADERS.computeIfAbsent(type, ExtensionLoader::new);
    }

    public Map<String, Class<? extends T>> extensionClasses() {
        Map<String, Class<? extends T>> local = extensionClasses;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (extensionClasses == null) {
                extensionClasses = Collections.unmodifiableMap(loadExtensionClasses());
            }
            return extensionClasses;
        }
    }

    public Optional<Class<? extends T>> extensionClass(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(extensionClasses().get(name.trim()));
    }

    public Optional<String> defaultExtensionName() {
        SPI spi = type.getAnnotation(SPI.class);
        if (spi == null || spi.value().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(spi.value().trim());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends T>> loadExtensionClasses() {
        String fileName = SPI_BASE + type.getName();
        ClassLoader classLoader = resolveClassLoader();
        Map<String, Class<? extends T>> mapping = new LinkedHashMap<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(fileName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int lineNo = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNo++;
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        ParsedLine parsed = parseLine(trimmed, url, lineNo);
                        Class<?> rawClass = Class.forName(parsed.className(), false, classLoader);
                        if (!type.isAssignableFrom(rawClass)) {
                            throw new IllegalStateException("SPI type mismatch in " + url + ":" + lineNo
                                    + ", " + rawClass.getName() + " is not assignable to " + type.getName());
                        }
                        Class<? extends T> implClass = (Class<? extends T>) rawClass;
                        putMapping(mapping, parsed.name(), implClass, url, lineNo);

                        // 兼容：总是注册 FQCN 别名，支持配置直接写类名
                        mapping.putIfAbsent(implClass.getName(), implClass);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SPI config: " + fileName, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load SPI implementation class for " + type.getName(), e);
        }

        return mapping;
    }

    private ParsedLine parseLine(String line, URL url, int lineNo) {
        int eqIndex = line.indexOf('=');
        if (eqIndex >= 0) {
            String name = line.substring(0, eqIndex).trim();
            String className = line.substring(eqIndex + 1).trim();
            if (name.isEmpty() || className.isEmpty()) {
                throw new IllegalStateException("Invalid SPI line in " + url + ":" + lineNo + " -> " + line);
            }
            return new ParsedLine(name, className);
        }

        // 兼容旧格式：只写类名
        String className = line.trim();
        if (className.isEmpty()) {
            throw new IllegalStateException("Invalid SPI line in " + url + ":" + lineNo + " -> " + line);
        }
        return new ParsedLine(deriveLegacyName(className), className);
    }

    private String deriveLegacyName(String className) {
        int split = className.lastIndexOf('.');
        String simpleName = split >= 0 ? className.substring(split + 1) : className;
        if (simpleName.isEmpty()) {
            throw new IllegalStateException("Invalid SPI class name: " + className);
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private void putMapping(Map<String, Class<? extends T>> mapping,
                            String name,
                            Class<? extends T> implClass,
                            URL url,
                            int lineNo) {
        Class<? extends T> existing = mapping.putIfAbsent(name, implClass);
        if (existing != null && !existing.equals(implClass)) {
            throw new IllegalStateException("Duplicate SPI name '" + name + "' in " + url + ":" + lineNo
                    + ", existing=" + existing.getName() + ", new=" + implClass.getName());
        }
    }

    private ClassLoader resolveClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ExtensionLoader.class.getClassLoader();
        }
        return classLoader;
    }

    private record ParsedLine(String name, String className) {
    }
}
