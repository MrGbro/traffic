package io.homeey.traffic.runtime.extension;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeExtensionResolverTest {

    @Test
    void shouldUseConfiguredNameBeforeSpiDefault() {
        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(new ExtensionInstantiator());

        ResolverOrderTestSpi extension = resolver.resolve(ResolverOrderTestSpi.class, "configured");

        assertThat(extension).isInstanceOf(ResolverOrderConfiguredImpl.class);
        assertThat(extension.name()).isEqualTo("configured");
    }

    @Test
    void shouldUseSpiDefaultWhenConfigIsEmpty() {
        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(new ExtensionInstantiator());

        ResolverOrderTestSpi extension = resolver.resolve(ResolverOrderTestSpi.class, " ");

        assertThat(extension).isInstanceOf(ResolverOrderSpiDefaultImpl.class);
        assertThat(extension.name()).isEqualTo("spiDefault");
    }

    @Test
    void shouldUseBuiltinDefaultWhenSpiDefaultMissing() {
        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(
                new ExtensionInstantiator(),
                Map.of(BuiltinFallbackTestSpi.class, "builtin")
        );

        BuiltinFallbackTestSpi extension = resolver.resolve(BuiltinFallbackTestSpi.class, null);

        assertThat(extension).isInstanceOf(BuiltinFallbackImpl.class);
        assertThat(extension.marker()).isEqualTo("builtin");
    }

    @Test
    void shouldSupportConfiguredImplementationClassName() {
        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(new ExtensionInstantiator());

        ResolverOrderTestSpi extension = resolver.resolve(
                ResolverOrderTestSpi.class,
                ResolverOrderConfiguredImpl.class.getName()
        );

        assertThat(extension).isInstanceOf(ResolverOrderConfiguredImpl.class);
    }

    @Test
    void shouldFailFastWhenConfiguredNameNotFound() {
        RuntimeExtensionResolver resolver = new RuntimeExtensionResolver(new ExtensionInstantiator());

        assertThatThrownBy(() -> resolver.resolve(ResolverOrderTestSpi.class, "missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No implementation found")
                .hasMessageContaining("missing");
    }
}
