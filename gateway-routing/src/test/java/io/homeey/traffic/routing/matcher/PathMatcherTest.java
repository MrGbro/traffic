package io.homeey.traffic.routing.matcher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathMatcherTest {

    private final PathMatcher matcher = new PathMatcher();

    @Test
    void shouldMatchWhenPathExactlyEquals() {
        assertThat(matcher.matches("/orders", "/orders")).isTrue();
    }

    @Test
    void shouldNotMatchWhenPathDifferent() {
        assertThat(matcher.matches("/orders", "/users")).isFalse();
    }

    @Test
    void shouldNotMatchWhenAnyPathIsNull() {
        assertThat(matcher.matches(null, "/orders")).isFalse();
        assertThat(matcher.matches("/orders", null)).isFalse();
    }
}
