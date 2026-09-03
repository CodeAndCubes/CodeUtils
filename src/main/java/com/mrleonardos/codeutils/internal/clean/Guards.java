package com.mrleonardos.codeutils.internal.clean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.clean.CleanupGuard;
import com.mrleonardos.codeutils.api.clean.EntityView;

public final class Guards {

    public static final String NAMED = "named";
    public static final String TAMED = "tamed";
    public static final String LEASHED = "leashed";
    public static final String BOSS = "boss";
    public static final String PERSISTENT = "persistent";
    public static final String CARRYING = "carrying";
    public static final String RIDDEN = "ridden";
    public static final String HANGING = "hanging";

    private static final List<String> BUILT_IN = Collections
        .unmodifiableList(Arrays.asList(NAMED, TAMED, LEASHED, BOSS, PERSISTENT, CARRYING, RIDDEN, HANGING));

    private final Set<String> ignored;
    private final Map<String, CleanupGuard> foreign;

    private Guards(Set<String> ignored, Map<String, CleanupGuard> foreign) {
        this.ignored = ignored;
        this.foreign = foreign;
    }

    public static List<String> builtIn() {
        return BUILT_IN;
    }

    public static Guards of(List<String> ignoreGuards, UtilsRegistry registry) {
        Set<String> ignored = new LinkedHashSet<>(names(ignoreGuards));
        Map<String, CleanupGuard> foreign = new LinkedHashMap<>();
        for (Map.Entry<String, CleanupGuard> named : registry.guards()
            .entrySet()) {
            String key = named.getKey()
                .toLowerCase(Locale.ROOT);
            if (!ignored.contains(key)) {
                foreign.put(named.getKey(), named.getValue());
            }
        }
        return new Guards(ignored, foreign);
    }

    public static List<String> foreignNames(List<String> ignoreGuards) {
        List<String> named = new ArrayList<>();
        for (String name : names(ignoreGuards)) {
            if (!BUILT_IN.contains(name)) {
                named.add(name);
            }
        }
        return named;
    }

    private static List<String> names(List<String> written) {
        List<String> cleaned = new ArrayList<>();
        for (String name : written == null ? new ArrayList<String>() : written) {
            if (name != null && !name.trim()
                .isEmpty()) {
                cleaned.add(
                    name.trim()
                        .toLowerCase(Locale.ROOT));
            }
        }
        return cleaned;
    }

    public boolean ignores(String guard) {
        return ignored.contains(guard);
    }

    public boolean protects(EntityView entity) {
        if (!ignored.contains(NAMED) && entity.named()) {
            return true;
        }
        if (!ignored.contains(TAMED) && entity.tamed()) {
            return true;
        }
        if (!ignored.contains(LEASHED) && entity.leashed()) {
            return true;
        }
        if (!ignored.contains(BOSS) && entity.boss()) {
            return true;
        }
        if (!ignored.contains(PERSISTENT) && entity.persistent()) {
            return true;
        }
        if (!ignored.contains(CARRYING) && entity.carrying()) {
            return true;
        }
        if (!ignored.contains(RIDDEN) && entity.ridden()) {
            return true;
        }
        if (!ignored.contains(HANGING) && entity.hanging()) {
            return true;
        }
        for (CleanupGuard guard : foreign.values()) {
            if (guard.protects(entity)) {
                return true;
            }
        }
        return false;
    }
}
