package com.mrleonardos.codeutils.internal;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.mrleonardos.codeutils.api.when.ServerSnapshot;

public final class UtilsBeat implements Ticker.Beat {

    private final ServerFacts facts;
    private final Supplier<ZoneId> zone;
    private final List<Subsystem> parts = new ArrayList<>();

    private BooleanSupplier paused = () -> false;

    public UtilsBeat(ServerFacts facts, Supplier<ZoneId> zone) {
        this.facts = facts;
        this.zone = zone;
    }

    public UtilsBeat add(Subsystem part) {
        parts.add(part);
        return this;
    }

    public UtilsBeat pause(BooleanSupplier when) {
        this.paused = when;
        return this;
    }

    public int size() {
        return parts.size();
    }

    @Override
    public void tick(long from, long to) {
        if (paused.getAsBoolean()) {
            return;
        }
        ServerSnapshot snapshot = snapshot(to);
        for (Subsystem part : parts) {
            if (paused.getAsBoolean()) {
                return;
            }
            part.tick(from, to, snapshot);
        }
    }

    public ServerSnapshot snapshot(long millis) {
        return ServerSnapshot.of(
            facts.online()
                .size(),
            facts.dimensions(),
            facts.tickMillis(),
            Clocks.local(millis, zone.get()));
    }
}
