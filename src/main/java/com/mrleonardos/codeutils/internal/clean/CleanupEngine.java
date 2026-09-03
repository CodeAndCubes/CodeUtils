package com.mrleonardos.codeutils.internal.clean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.clean.CleanupReport;
import com.mrleonardos.codeutils.api.clean.EntityKind;
import com.mrleonardos.codeutils.api.clean.EntityView;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;
import com.mrleonardos.codeutils.internal.Announcer;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.SpiNames;
import com.mrleonardos.codeutils.internal.Subsystem;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.clean.CleanupFile.RuleBlock;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

public final class CleanupEngine implements Subsystem, SpiNames.Source {

    /** Правило с одним порогом и без расписания считает по такту раз в столько секунд. */
    public static final int THRESHOLD_SECONDS = 30;

    private static final long NANOS_IN_MILLI = 1_000_000L;

    private final Supplier<CleanupFile> file;
    private final UtilsRegistry registry;
    private final Conditions conditions;
    private final EntitySweep sweep;
    private final Announcer announcer;
    private final IntSupplier slowPassMillis;
    private final Supplier<Boolean> audit;
    private final Logger log;

    private final Map<String, Live> live = new LinkedHashMap<>();

    public CleanupEngine(Supplier<CleanupFile> file, UtilsRegistry registry, Conditions conditions, EntitySweep sweep,
        Announcer announcer, IntSupplier slowPassMillis, Supplier<Boolean> audit, Logger log) {
        this.file = file;
        this.registry = registry;
        this.conditions = conditions;
        this.sweep = sweep;
        this.announcer = announcer;
        this.slowPassMillis = slowPassMillis;
        this.audit = audit;
        this.log = log;
    }

    public void arm(long now) {
        live.clear();
        for (Map.Entry<String, RuleBlock> entry : file.get().rules.entrySet()) {
            String name = entry.getKey();
            RuleBlock block = entry.getValue();
            if (!block.enabled) {
                continue;
            }
            Live rule = new Live(name, block);
            if (rule.matcher.empty()) {
                log.warn("Cleanup rule {} holds no types, it stays quiet", name);
                continue;
            }
            for (String guard : rule.guards.unknown(registry)) {
                log.warn(
                    "Cleanup rule {} takes off the guard {}, and there is no guard of that name, "
                        + "the line does nothing",
                    name,
                    guard);
            }
            int seconds = every(block);
            rule.deadline = seconds > 0 ? now + seconds * Clocks.MILLIS : 0L;
            if (seconds == 0) {
                log.info(
                    "Cleanup rule {} has neither intervalSeconds nor a threshold, "
                        + "it works only by /codeutils clean {} now",
                    name,
                    name);
            }
            live.put(name, rule);
        }
    }

    @Override
    public Map<String, String> sinks() {
        Map<String, String> named = new LinkedHashMap<>();
        for (Map.Entry<String, RuleBlock> entry : file.get().rules.entrySet()) {
            RuleBlock block = entry.getValue();
            if (block.enabled && !block.warnSeconds.isEmpty()) {
                named.put(where(entry.getKey()), word(block.warnSink));
            }
        }
        return named;
    }

    @Override
    public List<String> conditions() {
        List<String> named = new ArrayList<>();
        for (RuleBlock block : file.get().rules.values()) {
            if (block.enabled && block.when != null && block.when.custom != null) {
                named.addAll(block.when.custom);
            }
        }
        return named;
    }

    @Override
    public void tick(long from, long to, ServerSnapshot snapshot) {
        for (Live rule : live.values()) {
            if (rule.removeAt > 0L) {
                warn(rule, from, to);
                if (to >= rule.removeAt) {
                    rule.removeAt = 0L;
                    sweep(rule, true);
                }
                continue;
            }
            if (rule.deadline == 0L || to < rule.deadline) {
                continue;
            }
            rule.deadline = to + every(rule.block) * Clocks.MILLIS;
            if (!conditions.allows(rule.when, snapshot)) {
                continue;
            }
            due(rule, to);
        }
    }

    /** Посчитать подходящие сущности и ничего не снимать. */
    public CleanupReport count(String name) {
        Live rule = live.get(name);
        return rule == null ? null : sweep(rule, false);
    }

    /** Пройти правило прямо сейчас, вместе с удалением. */
    public CleanupReport clean(String name) {
        Live rule = live.get(name);
        return rule == null ? null : sweep(rule, true);
    }

    public boolean knows(String name) {
        return live.containsKey(name);
    }

    public boolean written(String name) {
        return file.get().rules.containsKey(name);
    }

    public List<String> names() {
        return new ArrayList<>(live.keySet());
    }

    public int workingRules() {
        return live.size();
    }

    /** Ближайший проход правила; ноль, если правило ходит только руками. */
    public long nextPassOf(String name) {
        Live rule = live.get(name);
        return rule == null ? 0L : rule.deadline;
    }

    private void due(Live rule, long now) {
        CleanupReport counted = sweep(rule, false);
        if (counted.matched() == 0) {
            return;
        }
        List<Integer> seconds = warnSeconds(rule.block);
        if (seconds.isEmpty()) {
            sweep(rule, true);
            return;
        }
        rule.counted = counted.matched();
        rule.removeAt = now + seconds.get(0)
            .intValue() * Clocks.MILLIS;
        announce(
            rule,
            seconds.get(0)
                .intValue());
    }

    private void warn(Live rule, long from, long to) {
        for (Integer second : warnSeconds(rule.block)) {
            long at = rule.removeAt - second.intValue() * Clocks.MILLIS;
            if (at > from && at <= to) {
                announce(rule, second.intValue());
            }
        }
    }

    private void announce(Live rule, int seconds) {
        announcer.say(
            rule.sink(registry),
            "",
            UtilsMessages.CLEAN_WARN,
            Integer.valueOf(seconds),
            Integer.valueOf(rule.counted));
    }

    private CleanupReport sweep(Live rule, boolean remove) {
        long started = System.nanoTime();
        List<EntityView> loaded = sweep.loaded(rule.dimensions());
        List<EntityView> matched = new ArrayList<>();
        Map<Long, Integer> perChunk = new LinkedHashMap<>();
        for (EntityView entity : loaded) {
            if (!fits(rule, entity)) {
                continue;
            }
            matched.add(entity);
            Long chunk = Long.valueOf(chunk(entity));
            Integer held = perChunk.get(chunk);
            perChunk.put(chunk, Integer.valueOf(held == null ? 1 : held.intValue() + 1));
        }
        List<EntityView> victims = victims(rule, matched, perChunk);
        int removed = remove && !victims.isEmpty() ? sweep.remove(victims) : 0;
        CleanupReport report = CleanupReport
            .of(rule.name, loaded.size(), matched.size(), removed, (System.nanoTime() - started) / NANOS_IN_MILLI);
        report(rule, report, remove);
        return report;
    }

    private List<EntityView> victims(Live rule, List<EntityView> matched, Map<Long, Integer> perChunk) {
        if (rule.block.worldThreshold > 0 && matched.size() <= rule.block.worldThreshold) {
            return Collections.emptyList();
        }
        List<EntityView> chosen = new ArrayList<>();
        for (EntityView entity : matched) {
            if (rule.block.chunkThreshold > 0) {
                Integer held = perChunk.get(Long.valueOf(chunk(entity)));
                if (held == null || held.intValue() <= rule.block.chunkThreshold) {
                    continue;
                }
            }
            chosen.add(entity);
        }
        int cap = Math.max(0, rule.block.maxRemovals);
        return cap > 0 && chosen.size() > cap ? new ArrayList<>(chosen.subList(0, cap)) : chosen;
    }

    private boolean fits(Live rule, EntityView entity) {
        if (entity.kind() == EntityKind.PLAYER) {
            return false;
        }
        if (!rule.matcher.matches(entity)) {
            return false;
        }
        int minAge = Math.max(0, rule.block.minAgeSeconds);
        if (minAge > 0 && (entity.ageSeconds() == EntityView.AGE_UNKNOWN || entity.ageSeconds() < minAge)) {
            return false;
        }
        return !rule.guards.protects(entity);
    }

    private void report(Live rule, CleanupReport pass, boolean removed) {
        int slow = slowPassMillis.getAsInt();
        if (slow > 0 && pass.millis() > slow) {
            log.warn(
                "Cleanup rule {} took {} ms, it looked at {} entity(ies) and removed {}",
                rule.name,
                Long.valueOf(pass.millis()),
                Integer.valueOf(pass.scanned()),
                Integer.valueOf(pass.removed()));
        }
        if (removed && pass.removed() > 0
            && audit.get()
                .booleanValue()) {
            log.info(
                "Cleanup rule {} removed {} of {} matching entity(ies)",
                rule.name,
                Integer.valueOf(pass.removed()),
                Integer.valueOf(pass.matched()));
        }
    }

    private static List<Integer> warnSeconds(RuleBlock block) {
        Set<Integer> found = new LinkedHashSet<>();
        for (Integer second : block.warnSeconds) {
            if (second != null && second.intValue() > 0) {
                found.add(second);
            }
        }
        List<Integer> sorted = new ArrayList<>(found);
        Collections.sort(sorted, Collections.reverseOrder());
        return sorted;
    }

    private static int every(RuleBlock block) {
        if (block.intervalSeconds > 0) {
            return block.intervalSeconds;
        }
        return block.worldThreshold > 0 || block.chunkThreshold > 0 ? THRESHOLD_SECONDS : 0;
    }

    private static long chunk(EntityView entity) {
        return ((long) entity.chunkX() << 32) ^ (entity.chunkZ() & 0xFFFFFFFFL);
    }

    private static String where(String name) {
        return CleanupFile.FILE_NAME + " rule " + name;
    }

    private static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private final class Live {

        private final String name;
        private final RuleBlock block;
        private final When when;
        private final EntityMatcher matcher;
        private final Guards guards;

        private long deadline;
        private long removeAt;
        private int counted;

        private Live(String name, RuleBlock block) {
            this.name = name;
            this.block = block;
            WhenBlock written = block.when == null ? new WhenBlock() : block.when;
            this.when = written.toWhen(where(name), log);
            this.matcher = EntityMatcher.of(block.types, where(name), log);
            this.guards = Guards.of(block.ignoreGuards, registry);
        }

        private Set<Integer> dimensions() {
            return new LinkedHashSet<>(block.dimensions);
        }

        private BroadcastSink sink(UtilsRegistry from) {
            return from.sink(word(block.warnSink))
                .orElse(null);
        }
    }
}
