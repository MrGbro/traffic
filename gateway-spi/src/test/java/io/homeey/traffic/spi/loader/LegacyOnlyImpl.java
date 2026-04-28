package io.homeey.traffic.spi.loader;

public class LegacyOnlyImpl implements LegacyFormatTestSpi {

    @Override
    public String name() {
        return "legacy";
    }
}
