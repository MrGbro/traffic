package io.homeey.traffic.routing.matcher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodMatcherTest {

    private final MethodMatcher matcher = new MethodMatcher();

    @Test
    void shouldMatchIgnoringCase() {
        assertThat(matcher.matches("get", "GET")).isTrue();
    }

    @Test
    void shouldNotMatchWhenMethodDifferent() {
        assertThat(matcher.matches("GET", "POST")).isFalse();
    }

    @Test
    void shouldNotMatchWhenAnyMethodIsNull() {
        assertThat(matcher.matches(null, "GET")).isFalse();
        assertThat(matcher.matches("GET", null)).isFalse();
    }
}
