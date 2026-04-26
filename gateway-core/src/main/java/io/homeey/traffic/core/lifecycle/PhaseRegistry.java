package io.homeey.traffic.core.lifecycle;

import io.homeey.traffic.common.Phase;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PhaseRegistry {

    private final Map<Phase, List<Runnable>> handlers = new EnumMap<>(Phase.class);

    public PhaseRegistry() {
        for (Phase phase : Phase.values()) {
            handlers.put(phase, new CopyOnWriteArrayList<>());
        }
    }

    public void register(Phase phase, Runnable handler) {
        handlers.get(phase).add(handler);
    }

    public List<Runnable> handlers(Phase phase) {
        return List.copyOf(handlers.get(phase));
    }
}
