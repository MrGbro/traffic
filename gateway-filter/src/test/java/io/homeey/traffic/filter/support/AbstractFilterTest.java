package io.homeey.traffic.filter.support;

import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.filter.core.FilterContext;
import io.homeey.traffic.filter.core.StubFilterContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractFilterTest {

    @Test
    void shouldCallDoFilterWhenNotSkipped() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        AbstractFilter filter = new AbstractFilter() {
            @Override
            protected void doFilter(FilterContext context, FilterChain chain) throws Exception {
                called.set(true);
                chain.filter(context);
            }
        };

        filter.filter(new StubFilterContext(), ctx -> {
        });

        assertThat(called).isTrue();
    }
}
