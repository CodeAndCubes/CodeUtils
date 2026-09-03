package com.mrleonardos.codeutils.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mrleonardos.codecore.api.actor.PlayerRef;

/** Сервер, которого нет: заданные числа вместо мира, менеджера игроков и прав. */
public final class FakeFacts implements ServerFacts {

    private final List<PlayerRef> online = new ArrayList<>();
    private final Map<UUID, Integer> dimensions = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> rights = new LinkedHashMap<>();
    private final Set<Integer> loaded = new LinkedHashSet<>();

    private long now;
    private double tickMillis = 20.0D;

    public FakeFacts() {
        loaded.add(Integer.valueOf(0));
    }

    public PlayerRef join(String name, int dimension, String... nodes) {
        PlayerRef player = PlayerRef.of(UUID.nameUUIDFromBytes(name.getBytes()), name);
        online.add(player);
        dimensions.put(player.id(), Integer.valueOf(dimension));
        Set<String> held = new LinkedHashSet<>();
        for (String node : nodes) {
            held.add(node);
        }
        rights.put(player.id(), held);
        loaded.add(Integer.valueOf(dimension));
        return player;
    }

    public void empty() {
        online.clear();
    }

    public FakeFacts at(long millis) {
        now = millis;
        return this;
    }

    public FakeFacts tick(double millis) {
        tickMillis = millis;
        return this;
    }

    @Override
    public long now() {
        return now;
    }

    @Override
    public List<PlayerRef> online() {
        return new ArrayList<>(online);
    }

    @Override
    public int dimensionOf(UUID player) {
        Integer found = dimensions.get(player);
        return found == null ? Integer.MIN_VALUE : found.intValue();
    }

    @Override
    public Set<Integer> dimensions() {
        return new LinkedHashSet<>(loaded);
    }

    @Override
    public double tickMillis() {
        return tickMillis;
    }

    @Override
    public boolean allowed(UUID player, String node) {
        Set<String> held = rights.get(player);
        return held != null && held.contains(node);
    }
}
