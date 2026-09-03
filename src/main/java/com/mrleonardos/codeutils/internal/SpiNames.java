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

        Map<String, String> runners();

        List<String> guards();

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
        for (Map.Entry<String, String> named : unknownRunners().entrySet()) {
            log.warn(
                "{} names the runner {}, and no mod registered it, the job stays quiet",
                named.getKey(),
                named.getValue());
        }
        for (String name : unknownGuards()) {
            log.warn(
                "Guard {} is named in ignoreGuards but there is no guard of that name, the line does nothing",
                name);
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

    public Map<String, String> unknownRunners() {
        Map<String, String> missing = new LinkedHashMap<>();
        for (Source source : sources) {
            for (Map.Entry<String, String> named : source.runners()
                .entrySet()) {
                if (!registry.runner(named.getValue())
                    .isPresent()) {
                    missing.put(named.getKey(), named.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(missing);
    }

    public List<String> unknownGuards() {
        Set<String> missing = new LinkedHashSet<>();
        for (Source source : sources) {
            for (String name : source.guards()) {
                if (!registry.guard(name)
                    .isPresent()) {
                    missing.add(name);
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(missing));
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
