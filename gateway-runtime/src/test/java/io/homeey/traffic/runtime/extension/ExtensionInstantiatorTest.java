package io.homeey.traffic.runtime.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionInstantiatorTest {

    private final ExtensionInstantiator instantiator = new ExtensionInstantiator();

    @Test
    void shouldReuseSingletonWhenNoConstructorArgs() {
        SingletonCreatedCounter.createdCount = 0;

        TestExtension first = instantiator.create(TestExtension.class, SingletonCreatedCounter.class);
        TestExtension second = instantiator.create(TestExtension.class, SingletonCreatedCounter.class);

        assertThat(first).isSameAs(second);
        assertThat(SingletonCreatedCounter.createdCount).isEqualTo(1);
    }

    @Test
    void shouldCreateNewInstanceWhenConstructorArgsPresent() {
        ConstructorArgExtension first = instantiator.create(ConstructorArgExtension.class, ConstructorArgExtension.class, "alpha");
        ConstructorArgExtension second = instantiator.create(ConstructorArgExtension.class, ConstructorArgExtension.class, "alpha");

        assertThat(first).isNotSameAs(second);
        assertThat(first.name).isEqualTo("alpha");
        assertThat(second.name).isEqualTo("alpha");
    }

    @Test
    void shouldMatchPrimitiveConstructorWithWrapperArgument() {
        PrimitiveArgExtension extension = instantiator.create(PrimitiveArgExtension.class, PrimitiveArgExtension.class, Integer.valueOf(8));
        assertThat(extension.size).isEqualTo(8);
    }

    @Test
    void shouldFailFastWhenNoConstructorMatches() {
        assertThatThrownBy(() -> instantiator.create(ConstructorArgExtension.class, ConstructorArgExtension.class, 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No matching constructor found");
    }

    @Test
    void shouldPreserveRootCauseWhenConstructorThrows() {
        assertThatThrownBy(() -> instantiator.create(TestExtension.class, BrokenExtension.class))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("boom");
    }

    private interface TestExtension {
    }

    private static final class SingletonCreatedCounter implements TestExtension {
        private static int createdCount;

        private SingletonCreatedCounter() {
            createdCount++;
        }
    }

    static final class ConstructorArgExtension {
        final String name;

        ConstructorArgExtension(String name) {
            this.name = name;
        }
    }

    static final class PrimitiveArgExtension {
        final int size;

        PrimitiveArgExtension(int size) {
            this.size = size;
        }
    }

    private static final class BrokenExtension implements TestExtension {
        private BrokenExtension() {
            throw new IllegalArgumentException("boom");
        }
    }
}
