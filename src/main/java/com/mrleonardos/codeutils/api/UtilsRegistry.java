package com.mrleonardos.codeutils.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.when.RunCondition;

/**
 * Реестры швов: приёмники рассылки, условия запуска, исполнители команд.
 *
 * <p>
 * Заявки принимаются в фазе init и только там. В конце постинициализации реестр замерзает, потому что
 * сразу после этого мод разрешает имена, написанные в файлах настроек, и говорит в лог про каждое
 * незнакомое. Заявка, поданная позже, не была бы видна файлам вовсе, поэтому она отклоняется исключением
 * с текстом о фазе, а не теряется молча.
 */
public final class UtilsRegistry {

    private final Map<String, BroadcastSink> sinks = new TreeMap<>();
    private final Map<String, RunCondition> conditions = new TreeMap<>();
    private final Map<String, CommandRunner> runners = new TreeMap<>();

    private volatile boolean frozen;

    /**
     * Заявить приёмник рассылки под своим именем.
     *
     * @throws IllegalArgumentException если имя уже занято другим приёмником
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addSink(String name, BroadcastSink sink) {
        put(sinks, "Broadcast sinks", name, sink);
    }

    /**
     * Заявить условие запуска под своим именем.
     *
     * @throws IllegalArgumentException если имя уже занято другим условием
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addCondition(String name, RunCondition condition) {
        put(conditions, "Run conditions", name, condition);
    }

    /**
     * Заявить исполнитель команд под своим именем.
     *
     * @throws IllegalArgumentException если имя уже занято другим исполнителем
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addRunner(String name, CommandRunner runner) {
        put(runners, "Command runners", name, runner);
    }

    /** Приёмник по имени из ключа {@code sink}. */
    public synchronized Optional<BroadcastSink> sink(String name) {
        return Optional.ofNullable(sinks.get(key(name)));
    }

    /** Условие по имени из списка {@code custom}. */
    public synchronized Optional<RunCondition> condition(String name) {
        return Optional.ofNullable(conditions.get(key(name)));
    }

    /** Исполнитель по имени. */
    public synchronized Optional<CommandRunner> runner(String name) {
        return Optional.ofNullable(runners.get(key(name)));
    }

    /** Имена заявленных приёмников по алфавиту. */
    public synchronized List<String> sinkNames() {
        return names(sinks);
    }

    /** Имена заявленных условий по алфавиту. */
    public synchronized List<String> conditionNames() {
        return names(conditions);
    }

    /** Имена заявленных исполнителей по алфавиту. */
    public synchronized List<String> runnerNames() {
        return names(runners);
    }

    /** Закрыть приём заявок. Зовёт сам CodeUtils в конце постинициализации. */
    public synchronized void freeze() {
        frozen = true;
    }

    /** Закрыт ли приём заявок. */
    public boolean frozen() {
        return frozen;
    }

    private <T> void put(Map<String, T> into, String what, String name, T value) {
        String key = key(name);
        Objects.requireNonNull(value, "value");
        if (key.isEmpty()) {
            throw new IllegalArgumentException(what + " need a name, an empty one is not a name");
        }
        if (frozen) {
            throw new IllegalStateException(
                what + " are frozen since the end of postInit, register " + key + " in your init phase");
        }
        T held = into.get(key);
        if (held != null && held != value) {
            throw new IllegalArgumentException(what + " already hold the name " + key);
        }
        into.put(key, value);
    }

    private static <T> List<String> names(Map<String, T> from) {
        return Collections.unmodifiableList(new ArrayList<>(from.keySet()));
    }

    private static String key(String name) {
        return name == null ? "" : name.trim();
    }
}
