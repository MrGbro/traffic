package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;
import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.filter.core.FilterContext;
import io.homeey.traffic.filter.core.GatewayFilter;
import io.homeey.traffic.spi.extension.Activate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseRegistryOrderingTest {

    @Test
    void shouldSortByActivateOrderAscending() {
        PhaseRegistry registry = new PhaseRegistry();
        List<String> names = new ArrayList<>();

        GatewayFilter f200 = new NamedFilter("b");
        GatewayFilter f100 = new NamedFilter("a");

        registry.register(Phase.PRE_ROUTE, f200, activateOf(200, "auth"));
        registry.register(Phase.PRE_ROUTE, f100, activateOf(100, "auth"));

        registry.filters(Phase.PRE_ROUTE).forEach(filter -> names.add(((NamedFilter) filter).name()));
        assertThat(names).containsExactly("a", "b");
    }

    private Activate activateOf(int order, String group) {
        return new Activate() {
            @Override
            public String[] group() {
                return new String[]{group};
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Activate.class;
            }
        };
    }

    static class NamedFilter implements GatewayFilter {
        private final String name;

        NamedFilter(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        @Override
        public void filter(FilterContext context, FilterChain chain) {
            // no-op for ordering test
        }
    }
}
