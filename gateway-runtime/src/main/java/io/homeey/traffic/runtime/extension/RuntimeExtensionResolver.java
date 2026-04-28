package io.homeey.traffic.runtime.extension;

import io.homeey.traffic.routing.locator.RouteLocator;
import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.forward.Forwarder;
import io.homeey.traffic.spi.contract.transport.TransportServer;
import io.homeey.traffic.spi.extension.ExtensionLoader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RuntimeExtensionResolver {

    private final ExtensionInstantiator instantiator;
    private final Map<Class<?>, String> builtinDefaultNames;

    public RuntimeExtensionResolver(ExtensionInstantiator instantiator) {
        this(instantiator, defaultBuiltinDefaults());
    }

    RuntimeExtensionResolver(ExtensionInstantiator instantiator, Map<Class<?>, String> builtinDefaultNames) {
        this.instantiator = Objects.requireNonNull(instantiator, "instantiator must not be null");
        this.builtinDefaultNames = Map.copyOf(builtinDefaultNames);
    }

    public <T> T resolve(Class<T> type, String configuredName, Object... ctorArgs) {
        ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
        Map<String, Class<? extends T>> candidates = loader.extensionClasses();

        String selectedName = selectName(type, configuredName, loader.defaultExtensionName());

        Class<? extends T> implClass = candidates.get(selectedName);
        if (implClass == null && configuredName != null && !configuredName.isBlank()) {
            implClass = tryLoadByClassName(type, configuredName.trim()).orElse(null);
        }

        if (implClass == null) {
            throw new IllegalStateException("No implementation found for " + type.getName()
                    + " with name '" + selectedName + "'. Available: " + candidates.keySet());
        }

        return instantiator.create(type, implClass, ctorArgs);
    }

    private <T> String selectName(Class<T> type, String configuredName, Optional<String> spiDefaultName) {
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName.trim();
        }
        if (spiDefaultName.isPresent()) {
            return spiDefaultName.get();
        }
        String builtIn = builtinDefaultNames.get(type);
        if (builtIn != null && !builtIn.isBlank()) {
            return builtIn;
        }
        throw new IllegalStateException("No extension name configured for " + type.getName());
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<Class<? extends T>> tryLoadByClassName(Class<T> type, String className) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = RuntimeExtensionResolver.class.getClassLoader();
            }
            Class<?> raw = Class.forName(className, false, classLoader);
            if (!type.isAssignableFrom(raw)) {
                return Optional.empty();
            }
            return Optional.of((Class<? extends T>) raw);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private static Map<Class<?>, String> defaultBuiltinDefaults() {
        Map<Class<?>, String> defaults = new LinkedHashMap<>();
        defaults.put(TransportServer.class, "jdkHttp");
        defaults.put(Forwarder.class, "http");
        defaults.put(ServiceDiscovery.class, "inMemory");
        defaults.put(LoadBalancer.class, "roundRobin");
        defaults.put(RouteLocator.class, "inMemory");
        return defaults;
    }
}
