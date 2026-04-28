package io.homeey.traffic.spi;

import io.homeey.traffic.spi.extension.ExtensionLoader;
import io.homeey.traffic.spi.loader.DuplicateNameTestSpi;
import io.homeey.traffic.spi.loader.InvalidBindingTestSpi;
import io.homeey.traffic.spi.loader.LegacyFormatTestSpi;
import io.homeey.traffic.spi.loader.LegacyOnlyImpl;
import io.homeey.traffic.spi.loader.NamedFirstImpl;
import io.homeey.traffic.spi.loader.NamedSecondImpl;
import io.homeey.traffic.spi.loader.NamedTestSpi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionLoaderTest {

    @Test
    void shouldLoadNameClassFormatAndDefaultName() {
        ExtensionLoader<NamedTestSpi> loader = ExtensionLoader.getExtensionLoader(NamedTestSpi.class);

        Map<String, Class<? extends NamedTestSpi>> classes = loader.extensionClasses();

        assertThat(classes).containsEntry("first", NamedFirstImpl.class);
        assertThat(classes).containsEntry("second", NamedSecondImpl.class);
        assertThat(loader.defaultExtensionName()).contains("first");
    }

    @Test
    void shouldSupportLegacyClassOnlyFormat() {
        ExtensionLoader<LegacyFormatTestSpi> loader = ExtensionLoader.getExtensionLoader(LegacyFormatTestSpi.class);

        Map<String, Class<? extends LegacyFormatTestSpi>> classes = loader.extensionClasses();

        assertThat(classes).containsEntry("legacyOnlyImpl", LegacyOnlyImpl.class);
        assertThat(classes).containsEntry(LegacyOnlyImpl.class.getName(), LegacyOnlyImpl.class);
    }

    @Test
    void shouldFailFastWhenDuplicateNameMappedToDifferentClasses() {
        ExtensionLoader<DuplicateNameTestSpi> loader = ExtensionLoader.getExtensionLoader(DuplicateNameTestSpi.class);

        assertThatThrownBy(loader::extensionClasses)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate SPI name");
    }

    @Test
    void shouldFailFastWhenImplementationNotAssignableToSpiType() {
        ExtensionLoader<InvalidBindingTestSpi> loader = ExtensionLoader.getExtensionLoader(InvalidBindingTestSpi.class);

        assertThatThrownBy(loader::extensionClasses)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not assignable");
    }
}
