package com.mrleonardos.codeutils.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;

public final class SpiNames {

    public interface Source {

        Map<String, String> sinks();

        List<String> conditions();
    }

    private final UtilsRegistry registry;
    private final Logger log;
    private final List<Source> sources = new ArrayList<>();

    public SpiNames(UtilsRegistry registry, Logger log) {
        this.registry = registry;
        this.log = log;
    }

    public SpiNames add(Source source) {
        if (source != null) {
            sources.add(source);
        }
        return this;
    }

    public void resolve() {
        for (Map.Entry<String, String> named : unknownSinks().entrySet()) {
            log.warn(
                "{} names the sink {}, and no mod registered it, the set stays quiet",
                named.getKey(),
                named.getValue());
        }
        for (String name : unknownConditions()) {
            log.warn(
                "Run condition {} is named in a when block but no mod registered it, "
                    + "every schedule that names it stays quiet",
                name);
        }
    }

    public Map<String, String> unknownSinks() {
        Map<String, String> missing = new LinkedHashMap<>();
        for (Source source : sources) {
            for (Map.Entry<String, String> named : source.sinks()
                .entrySet()) {
                if (!registry.sink(named.getValue())
                    .isPresent()) {
                    missing.put(named.getKey(), named.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(missing);
    }

    public List<String> unknownConditions() {
        Set<String> missing = new LinkedHashSet<>();
        for (Source source : sources) {
            for (String name : source.conditions()) {
                if (!registry.condition(name)
                    .isPresent()) {
                    missing.add(name);
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(missing));
    }
}
