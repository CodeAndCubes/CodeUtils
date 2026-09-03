package com.mrleonardos.codeutils.api.clean;

import java.util.Objects;

/**
 * Чем кончился проход очистки: сколько сущностей просмотрено, сколько подошло правилу и сколько снято.
 *
 * <p>
 * Числа разные не по недосмотру. Просмотрено это все загруженные сущности подходящих измерений, подошло
 * это те, что прошли типы, возраст и защиты, а снято это то, что осталось после порогов и потолка
 * {@code maxRemovals}. Счётный проход даёт отчёт, у которого снято ноль.
 */
public final class CleanupReport {

    private final String rule;
    private final int scanned;
    private final int matched;
    private final int removed;
    private final long millis;

    private CleanupReport(String rule, int scanned, int matched, int removed, long millis) {
        this.rule = rule;
        this.scanned = scanned;
        this.matched = matched;
        this.removed = removed;
        this.millis = millis;
    }

    /** Отчёт о проходе правила. */
    public static CleanupReport of(String rule, int scanned, int matched, int removed, long millis) {
        return new CleanupReport(
            Objects.requireNonNull(rule, "rule"),
            Math.max(0, scanned),
            Math.max(0, matched),
            Math.max(0, removed),
            Math.max(0L, millis));
    }

    /** Имя правила из файла настроек. */
    public String rule() {
        return rule;
    }

    /** Сколько загруженных сущностей просмотрено. */
    public int scanned() {
        return scanned;
    }

    /** Сколько из них подошло правилу и не было защищено. */
    public int matched() {
        return matched;
    }

    /** Сколько снято на самом деле. */
    public int removed() {
        return removed;
    }

    /** Сколько миллисекунд занял проход. */
    public long millis() {
        return millis;
    }

    @Override
    public String toString() {
        return "cleanup[" + rule + ", scanned " + scanned + ", matched " + matched + ", removed " + removed + "]";
    }
}
