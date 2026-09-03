package com.mrleonardos.codeutils.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

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

    public List<String> unknown(Collection<When> blocks) {
        Set<String> missing = new LinkedHashSet<>();
        for (When when : blocks) {
            for (String name : when.custom()) {
                if (!registry.condition(name)
                    .isPresent()) {
                    missing.add(name);
                }
            }
        }
        return new ArrayList<>(missing);
    }

    public void report(Collection<When> blocks, Logger log) {
        for (String name : unknown(blocks)) {
            log.warn(
                "Run condition {} is named in a when block but no mod registered it, "
                    + "every schedule that names it stays quiet",
                name);
        }
    }
}
