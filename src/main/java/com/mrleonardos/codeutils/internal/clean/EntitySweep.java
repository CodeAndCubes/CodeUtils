package com.mrleonardos.codeutils.internal.clean;

import java.util.List;
import java.util.Set;

import com.mrleonardos.codeutils.api.clean.EntityView;

public interface EntitySweep {

    List<EntityView> loaded(Set<Integer> dimensions);

    int remove(List<EntityView> victims);
}
