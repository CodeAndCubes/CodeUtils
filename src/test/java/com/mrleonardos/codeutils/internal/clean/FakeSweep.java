package com.mrleonardos.codeutils.internal.clean;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mrleonardos.codeutils.api.clean.EntityView;

/**
 * Проход по миру, которого нет.
 *
 * <p>
 * Список загруженных сущностей отдаётся так, что обход видно снаружи: удаление во время обхода роняет
 * прогон, а не проходит молча. Ровно это правило и стоит за двумя шагами прохода: список загруженных
 * сущностей нельзя править, пока по нему идут.
 */
public final class FakeSweep implements EntitySweep {

    private final List<EntityView> world = new ArrayList<>();
    private final List<EntityView> removed = new ArrayList<>();

    private boolean walking;
    private int removeCalls;

    public FakeSweep add(EntityView... entities) {
        for (EntityView entity : entities) {
            world.add(entity);
        }
        return this;
    }

    public List<EntityView> removed() {
        return removed;
    }

    public int removeCalls() {
        return removeCalls;
    }

    public int alive() {
        return world.size();
    }

    @Override
    public List<EntityView> loaded(Set<Integer> dimensions) {
        List<EntityView> found = new ArrayList<>();
        for (EntityView entity : world) {
            if (dimensions.isEmpty() || dimensions.contains(Integer.valueOf(entity.dimension()))) {
                found.add(entity);
            }
        }
        return new Watched(found);
    }

    @Override
    public int remove(List<EntityView> victims) {
        if (walking) {
            throw new IllegalStateException("удалять во время обхода списка загруженных сущностей нельзя");
        }
        removeCalls++;
        removed.addAll(victims);
        world.removeAll(victims);
        return victims.size();
    }

    /** Список, который поднимает флаг обхода на время своего перебора. */
    private final class Watched extends AbstractList<EntityView> {

        private final List<EntityView> held;

        private Watched(List<EntityView> held) {
            this.held = held;
        }

        @Override
        public EntityView get(int index) {
            return held.get(index);
        }

        @Override
        public int size() {
            return held.size();
        }

        @Override
        public Iterator<EntityView> iterator() {
            Iterator<EntityView> walk = held.iterator();
            walking = true;
            return new Iterator<EntityView>() {

                @Override
                public boolean hasNext() {
                    boolean more = walk.hasNext();
                    if (!more) {
                        walking = false;
                    }
                    return more;
                }

                @Override
                public EntityView next() {
                    return walk.next();
                }
            };
        }
    }
}
