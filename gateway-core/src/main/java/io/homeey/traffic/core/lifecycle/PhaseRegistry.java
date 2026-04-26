package io.homeey.traffic.core.lifecycle;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.filter.core.GatewayFilter;
import io.homeey.traffic.spi.extension.Activate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PhaseRegistry {

    private final Map<Phase, List<FilterEntry>> entries = new EnumMap<>(Phase.class);

    record FilterEntry(GatewayFilter filter, Activate activate) {
    }

    public PhaseRegistry() {
        for (Phase phase : Phase.values()) {
            entries.put(phase, new CopyOnWriteArrayList<>());
        }
    }

    public void register(Phase phase, GatewayFilter filter) {
        register(phase, filter, null);
    }

    public void register(Phase phase, GatewayFilter filter, Activate activate) {
        entries.get(phase).add(new FilterEntry(filter, activate));
    }

    public List<GatewayFilter> filters(Phase phase) {
        return entries.get(phase).stream()
                .sorted((left, right) -> {
                    String leftGroup = groupOf(left.activate());
                    String rightGroup = groupOf(right.activate());
                    int byGroup = leftGroup.compareTo(rightGroup);
                    if (byGroup != 0) {
                        return byGroup;
                    }
                    return Integer.compare(orderOf(left.activate()), orderOf(right.activate()));
                })
                .map(FilterEntry::filter)
                .toList();
    }

    private int orderOf(Activate activate) {
        return activate == null ? 0 : activate.order();
    }

    private String groupOf(Activate activate) {
        if (activate == null || activate.group().length == 0) {
            return "";
        }
        return activate.group()[0];
    }
}
