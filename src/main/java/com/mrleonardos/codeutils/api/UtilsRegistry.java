package com.mrleonardos.codeutils.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import com.mrleonardos.codeutils.api.clean.CleanupGuard;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.queue.QueuePolicy;
import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartStep;
import com.mrleonardos.codeutils.api.run.CommandRunner;
import com.mrleonardos.codeutils.api.when.RunCondition;

/**
 * Реестры швов: приёмники рассылки, условия запуска, исполнители команд, защиты очистки, шаги остановки и
 * политики очереди.
 *
 * <p>
 * Заявки принимаются в фазе init и только там. В конце постинициализации реестр замерзает, потому что
 * сразу после этого мод разрешает имена, написанные в файлах настроек, и говорит в лог про каждое
 * незнакомое. Заявка, поданная позже, не была бы видна файлам вовсе, поэтому она отклоняется исключением
 * с текстом о фазе, а не теряется молча.
 *
 * <p>
 * Четыре шва выбираются по имени там, где ими пользуются: {@code sink} набора рассылки, {@code custom}
 * блока условий, {@code runner} задания и {@code queue.policy}. Два оставшихся имени не выбирают:
 * защиты очистки спрашиваются все сразу, а шаги остановки выполняются все в своей точке.
 */
public final class UtilsRegistry {

    private final Map<String, BroadcastSink> sinks = new TreeMap<>();
    private final Map<String, RunCondition> conditions = new TreeMap<>();
    private final Map<String, CommandRunner> runners = new TreeMap<>();
    private final Map<String, CleanupGuard> guards = new TreeMap<>();
    private final Map<String, RestartStep> steps = new TreeMap<>();
    private final Map<String, QueuePolicy> policies = new TreeMap<>();

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

    /**
     * Заявить защиту очистки под своим именем.
     *
     * <p>
     * Защиту спрашивают при каждом проходе, а не только когда её назвали в файле. Снять её админ может
     * тем же ключом {@code ignoreGuards}, что и встроенные восемь.
     *
     * @throws IllegalArgumentException если имя уже занято другой защитой
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addGuard(String name, CleanupGuard guard) {
        put(guards, "Cleanup guards", name, guard);
    }

    /**
     * Заявить шаг остановки. Имя и точку шаг называет сам.
     *
     * @throws IllegalArgumentException если имя уже занято другим шагом или шаг не назвал имя и точку
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addStep(RestartStep step) {
        Objects.requireNonNull(step, "step");
        if (step.phase() == null) {
            throw new IllegalArgumentException("Restart steps need a phase, " + key(step.name()) + " named none");
        }
        put(steps, "Restart steps", step.name(), step);
    }

    /**
     * Заявить политику очереди под своим именем.
     *
     * @throws IllegalArgumentException если имя уже занято другой политикой
     * @throws IllegalStateException    если реестр уже заморожен
     */
    public synchronized void addPolicy(String name, QueuePolicy policy) {
        put(policies, "Queue policies", name, policy);
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

    /** Защита очистки по имени из списка {@code ignoreGuards}. */
    public synchronized Optional<CleanupGuard> guard(String name) {
        return Optional.ofNullable(guards.get(key(name)));
    }

    /** Все заявленные защиты по алфавиту: их спрашивают на каждой сущности прохода. */
    public synchronized Map<String, CleanupGuard> guards() {
        return Collections.unmodifiableMap(new TreeMap<>(guards));
    }

    /** Шаги остановки этой точки по алфавиту. */
    public synchronized List<RestartStep> steps(RestartPhase phase) {
        List<RestartStep> chosen = new ArrayList<>();
        for (RestartStep step : steps.values()) {
            if (step.phase() == phase) {
                chosen.add(step);
            }
        }
        return Collections.unmodifiableList(chosen);
    }

    /** Политика очереди по имени из ключа {@code queue.policy}. */
    public synchronized Optional<QueuePolicy> policy(String name) {
        return Optional.ofNullable(policies.get(key(name)));
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

    /** Имена заявленных защит по алфавиту. */
    public synchronized List<String> guardNames() {
        return names(guards);
    }

    /** Имена заявленных шагов остановки по алфавиту. */
    public synchronized List<String> stepNames() {
        return names(steps);
    }

    /** Имена заявленных политик очереди по алфавиту. */
    public synchronized List<String> policyNames() {
        return names(policies);
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
