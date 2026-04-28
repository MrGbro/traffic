package io.homeey.traffic.runtime.app;

import io.homeey.traffic.runtime.bootstrap.GatewayBootstrap;
import io.homeey.traffic.runtime.config.GatewayRuntimeConfig;
import io.homeey.traffic.runtime.config.GatewayRuntimeConfigLoader;
import io.homeey.traffic.runtime.extension.ExtensionInstantiator;
import io.homeey.traffic.runtime.extension.RuntimeExtensionResolver;
import io.homeey.traffic.routing.locator.RouteLocator;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import io.homeey.traffic.spi.contract.forward.Forwarder;
import io.homeey.traffic.spi.contract.transport.TransportServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GatewayApplication {

    private GatewayApplication() {
    }

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 0 ? args[0] : "./gateway.properties";
        GatewayRuntimeConfig config = GatewayRuntimeConfigLoader.load(Path.of(configPath));

        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(new ExtensionInstantiator());
        GatewayRuntimeConfig.SpiConfig spi = config.spiConfig();

        List<RouteDefinition> routes = toRoutes(config.routes());
        Map<String, List<ServiceInstance>> services = toServiceRegistry(config.serviceInstances());

        RouteLocator routeLocator = resolver.resolve(RouteLocator.class, spi.routeLocator(), routes);
        ServiceDiscovery serviceDiscovery = resolver.resolve(ServiceDiscovery.class, spi.serviceDiscovery(), services);
        LoadBalancer loadBalancer = resolver.resolve(LoadBalancer.class, spi.loadBalancer());
        Forwarder forwarder = resolver.resolve(Forwarder.class, spi.forwarder());

        TransportServer transportServer;
        if (config.staticConfig().enabled()) {
            transportServer = resolver.resolve(
                    TransportServer.class,
                    spi.transport(),
                    Path.of(config.staticConfig().rootDir()),
                    config.staticConfig().uriPrefix()
            );
        } else {
            transportServer = resolver.resolve(TransportServer.class, spi.transport());
        }

        GatewayBootstrap bootstrap = new GatewayBootstrap(routeLocator, serviceDiscovery, loadBalancer, forwarder, transportServer);
        bootstrap.start(config.port());

        Runtime.getRuntime().addShutdownHook(new Thread(bootstrap::stop));
        Thread.currentThread().join();
    }

    private static List<RouteDefinition> toRoutes(List<GatewayRuntimeConfig.RouteConfig> routeConfigs) {
        List<RouteDefinition> routes = new ArrayList<>();
        for (GatewayRuntimeConfig.RouteConfig route : routeConfigs) {
            routes.add(new RouteDefinition(
                    route.id(),
                    route.target(),
                    route.order(),
                    List.of(
                            new PredicateDefinition("Path", Map.of("value", route.path())),
                            new PredicateDefinition("Method", Map.of("value", route.method()))
                    )
            ));
        }
        return List.copyOf(routes);
    }

    private static Map<String, List<ServiceInstance>> toServiceRegistry(Map<String, List<String>> raw) {
        return raw.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                        .map(address -> toInstance(entry.getKey(), address))
                        .toList()
        ));
    }

    private static ServiceInstance toInstance(String serviceName, String address) {
        String[] parts = address.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid service instance address: " + address);
        }
        return new ServiceInstance(serviceName, parts[0], Integer.parseInt(parts[1]), Map.of());
    }
}
