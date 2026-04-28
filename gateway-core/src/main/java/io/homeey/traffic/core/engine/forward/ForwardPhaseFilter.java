package io.homeey.traffic.core.engine.forward;

import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.filter.core.FilterContext;
import io.homeey.traffic.filter.phase.PreForwardFilter;
import io.homeey.traffic.routing.context.RoutingAttributes;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import io.homeey.traffic.spi.contract.forward.Forwarder;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ForwardPhaseFilter implements PreForwardFilter {

    private static final String BAD_GATEWAY = "Bad Gateway";
    private static final String GATEWAY_TIMEOUT = "Gateway Timeout";
    private static final String SERVICE_UNAVAILABLE = "Service Unavailable";
    private static final String SERVICE_PREFIX = "svc://";

    private final Forwarder forwarder;
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalancer loadBalancer;

    public ForwardPhaseFilter(Forwarder forwarder) {
        this(forwarder, service -> List.of(), (service, instances) -> Optional.empty());
    }

    public ForwardPhaseFilter(Forwarder forwarder, ServiceDiscovery serviceDiscovery, LoadBalancer loadBalancer) {
        this.forwarder = forwarder;
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public void filter(FilterContext context, FilterChain chain) throws Exception {
        String target = context.attribute(RoutingAttributes.ROUTE_TARGET);
        String path = context.attribute(ExchangeAttributes.REQUEST_PATH);
        String method = context.attribute(ExchangeAttributes.REQUEST_METHOD);

        if (isBlank(target) || isBlank(path) || isBlank(method)) {
            context.terminate(502, BAD_GATEWAY);
            return;
        }

        String resolvedTarget = resolveTarget(target);
        if (resolvedTarget == null) {
            context.terminate(503, SERVICE_UNAVAILABLE);
            return;
        }
        context.attribute(RoutingAttributes.ROUTE_RESOLVED_TARGET, resolvedTarget);

        Map<String, String> headers = safeMap(context.attribute(ExchangeAttributes.REQUEST_HEADERS));
        byte[] body = safeBody(context.attribute(ExchangeAttributes.REQUEST_BODY));
        Map<String, String> attributes = Map.of(
                RoutingAttributes.ROUTE_ID,
                valueOrEmpty(context.attribute(RoutingAttributes.ROUTE_ID))
        );

        SpiRequestContext request = new SpiRequestContext(
                context.requestId(),
                path,
                method,
                headers,
                body,
                attributes
        );

        try {
            SpiResponseContext response = forwarder.forward(resolvedTarget, request);
            context.attribute(ExchangeAttributes.RESPONSE_STATUS, response.statusCode());
            context.attribute(ExchangeAttributes.RESPONSE_HEADERS, response.headers());
            context.attribute(ExchangeAttributes.RESPONSE_BODY, response.body());
            chain.filter(context);
        } catch (HttpTimeoutException e) {
            context.terminate(504, GATEWAY_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.terminate(502, BAD_GATEWAY);
        } catch (IOException e) {
            context.terminate(502, BAD_GATEWAY);
        }
    }

    private String resolveTarget(String target) {
        if (!target.startsWith(SERVICE_PREFIX)) {
            return target;
        }
        String serviceName = target.substring(SERVICE_PREFIX.length());
        if (serviceName.isBlank()) {
            return null;
        }
        List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
        return loadBalancer.choose(serviceName, instances)
                .map(ServiceInstance::toUrl)
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> safeMap(Object headers) {
        if (headers instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        return Map.of();
    }

    private byte[] safeBody(Object body) {
        if (body instanceof byte[] bytes) {
            return bytes;
        }
        return new byte[0];
    }
}
