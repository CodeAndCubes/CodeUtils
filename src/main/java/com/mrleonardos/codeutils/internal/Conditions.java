package com.mrleonardos.codeutils.internal;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.when.RunCondition;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;

public final class Conditions {

    private final UtilsRegistry registry;

    public Conditions(UtilsRegistry registry) {
        this.registry = registry;
    }

    public boolean allows(When when, ServerSnapshot snapshot) {
        if (!when.allows(snapshot)) {
            return false;
        }
        for (String name : when.custom()) {
            RunCondition condition = registry.condition(name)
                .orElse(null);
            if (condition == null || !condition.allows(snapshot)) {
                return false;
            }
        }
        return true;
    }
}
