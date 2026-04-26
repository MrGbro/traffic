package io.homeey.traffic.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseTest {

    @Test
    void shouldHaveSixPhases() {
        assertThat(Phase.values()).hasSize(6);
    }
}
