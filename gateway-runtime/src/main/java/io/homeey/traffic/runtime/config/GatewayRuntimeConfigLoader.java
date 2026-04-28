package io.homeey.traffic.runtime.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class GatewayRuntimeConfigLoader {

    private GatewayRuntimeConfigLoader() {
    }

    public static GatewayRuntimeConfig load(Path path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }

        int port = Integer.parseInt(props.getProperty("gateway.server.port", "8080"));

        boolean staticEnabled = Boolean.parseBoolean(props.getProperty("gateway.static.enabled", "false"));
        String staticPrefix = props.getProperty("gateway.static.uriPrefix", "/static/");
        String staticRoot = props.getProperty("gateway.static.rootDir", "./public");
        GatewayRuntimeConfig.StaticConfig staticConfig =
                new GatewayRuntimeConfig.StaticConfig(staticEnabled, staticPrefix, staticRoot);

        GatewayRuntimeConfig.SpiConfig spiConfig = new GatewayRuntimeConfig.SpiConfig(
                nullable(props.getProperty("gateway.spi.transport")),
                nullable(props.getProperty("gateway.spi.forwarder")),
                nullable(props.getProperty("gateway.spi.serviceDiscovery")),
                nullable(props.getProperty("gateway.spi.loadBalancer")),
                nullable(props.getProperty("gateway.spi.routeLocator"))
        );

        List<GatewayRuntimeConfig.RouteConfig> routes = parseRoutes(props);
        Map<String, List<String>> serviceInstances = parseServices(props);

        return new GatewayRuntimeConfig(port, staticConfig, spiConfig, routes, serviceInstances);
    }

    private static List<GatewayRuntimeConfig.RouteConfig> parseRoutes(Properties props) {
        String routesRaw = props.getProperty("gateway.routes", "").trim();
        if (routesRaw.isEmpty()) {
            return List.of();
        }

        List<GatewayRuntimeConfig.RouteConfig> routes = new ArrayList<>();
        for (String id : routesRaw.split(",")) {
            String routeId = id.trim();
            if (routeId.isEmpty()) {
                continue;
            }
            String prefix = "gateway.route." + routeId + ".";
            String target = props.getProperty(prefix + "target", "");
            int order = Integer.parseInt(props.getProperty(prefix + "order", "0"));
            String path = props.getProperty(prefix + "path", "");
            String method = props.getProperty(prefix + "method", "GET");
            routes.add(new GatewayRuntimeConfig.RouteConfig(routeId, target, order, path, method));
        }
        return List.copyOf(routes);
    }

    private static Map<String, List<String>> parseServices(Properties props) {
        String servicesRaw = props.getProperty("gateway.services", "").trim();
        if (servicesRaw.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        for (String name : servicesRaw.split(",")) {
            String service = name.trim();
            if (service.isEmpty()) {
                continue;
            }
            String key = "gateway.service." + service + ".instances";
            String instancesRaw = props.getProperty(key, "").trim();
            if (instancesRaw.isEmpty()) {
                result.put(service, List.of());
                continue;
            }
            List<String> instances = new ArrayList<>();
            for (String item : instancesRaw.split(",")) {
                String instance = item.trim();
                if (!instance.isEmpty()) {
                    instances.add(instance);
                }
            }
            result.put(service, List.copyOf(instances));
        }
        return Map.copyOf(result);
    }

    private static String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
