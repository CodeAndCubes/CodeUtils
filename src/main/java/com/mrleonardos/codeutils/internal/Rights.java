package com.mrleonardos.codeutils.internal;

import java.util.UUID;

public interface Rights {

    boolean allowed(UUID player, String node);
}
