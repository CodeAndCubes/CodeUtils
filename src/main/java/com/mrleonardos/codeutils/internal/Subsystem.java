package com.mrleonardos.codeutils.internal;

import com.mrleonardos.codeutils.api.when.ServerSnapshot;

public interface Subsystem {

    void tick(long from, long to, ServerSnapshot snapshot);
}
