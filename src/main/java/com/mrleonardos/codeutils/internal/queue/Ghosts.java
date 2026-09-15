package com.mrleonardos.codeutils.internal.queue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Ghosts {

    private final Set<UUID> refused = ConcurrentHashMap.newKeySet();

    public void refuse(UUID id) {
        if (id != null) {
            refused.add(id);
        }
    }

    public void admit(UUID id) {
        if (id != null) {
            refused.remove(id);
        }
    }

    public boolean countsJoin(UUID id) {
        return id == null || !refused.contains(id);
    }

    public boolean countsLeave(UUID id) {
        return id == null || !refused.remove(id);
    }
}
