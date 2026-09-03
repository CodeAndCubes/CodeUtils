package com.mrleonardos.codeutils.internal;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mrleonardos.codecore.api.actor.PlayerRef;

public interface ServerFacts extends Rights {

    long now();

    List<PlayerRef> online();

    int dimensionOf(UUID player);

    Set<Integer> dimensions();

    double tickMillis();
}
