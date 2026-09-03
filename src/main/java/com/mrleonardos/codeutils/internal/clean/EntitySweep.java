package com.mrleonardos.codeutils.internal.clean;

import java.util.List;
import java.util.Set;

import com.mrleonardos.codeutils.api.clean.EntityView;

public interface EntitySweep {

    /** Все загруженные сущности выбранных измерений. Пустой набор означает все загруженные измерения. */
    List<EntityView> loaded(Set<Integer> dimensions);

    /** Снять собранные сущности вторым шагом и сказать, сколько снято. */
    int remove(List<EntityView> victims);
}
