package com.mrleonardos.codeutils.internal.queue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mrleonardos.codeutils.internal.Rights;

/**
 * Права без мира: узлы раздаются по идентификатору игрока, которого на сервере ещё нет.
 *
 * <p>
 * Ровно поэтому права и вынесены из {@code ServerFacts}: решение на входе спрашивает их на потоке сети,
 * где игрока в мире нет и не будет, пока его не пустят.
 */
public final class FakeRights implements Rights {

    private final Map<UUID, Set<String>> nodes = new LinkedHashMap<>();

    public FakeRights give(UUID player, String node) {
        Set<String> held = nodes.get(player);
        if (held == null) {
            held = new LinkedHashSet<>();
            nodes.put(player, held);
        }
        held.add(node);
        return this;
    }

    @Override
    public boolean allowed(UUID player, String node) {
        Set<String> held = nodes.get(player);
        return held != null && held.contains(node);
    }
}
