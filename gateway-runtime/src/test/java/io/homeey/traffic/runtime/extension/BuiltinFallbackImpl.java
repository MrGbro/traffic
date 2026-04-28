package io.homeey.traffic.runtime.extension;

public class BuiltinFallbackImpl implements BuiltinFallbackTestSpi {

    @Override
    public String marker() {
        return "builtin";
    }
}
