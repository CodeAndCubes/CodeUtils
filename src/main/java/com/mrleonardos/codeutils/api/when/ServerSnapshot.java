package com.mrleonardos.codeutils.api.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Состояние сервера на один такт: готовые числа, по которым решают все условия сразу.
 *
 * <p>
 * Снимок собирается один раз в секунду и раздаётся всем условиям. Условие ничего не считает само и
 * никуда не ходит: обращение к миру из условия стоило бы столько же, сколько сама работа, и повторялось
 * бы у каждого набора, задания и правила.
 */
public final class ServerSnapshot {

    private final int players;
    private final Set<Integer> dimensions;
    private final double tickMillis;
    private final LocalDateTime time;

    private ServerSnapshot(int players, Set<Integer> dimensions, double tickMillis, LocalDateTime time) {
        this.players = players;
        this.dimensions = Collections.unmodifiableSet(new LinkedHashSet<>(dimensions));
        this.tickMillis = tickMillis;
        this.time = time;
    }

    /**
     * Снимок из готовых значений.
     *
     * @param players    сколько игроков сейчас на сервере
     * @param dimensions номера загруженных измерений
     * @param tickMillis средняя длина тика в миллисекундах
     * @param time       местное время по поясу из настроек мода
     */
    public static ServerSnapshot of(int players, Set<Integer> dimensions, double tickMillis, LocalDateTime time) {
        return new ServerSnapshot(
            players,
            Objects.requireNonNull(dimensions, "dimensions"),
            tickMillis,
            Objects.requireNonNull(time, "time"));
    }

    /** Сколько игроков на сервере. */
    public int players() {
        return players;
    }

    /** Номера загруженных измерений. */
    public Set<Integer> dimensions() {
        return dimensions;
    }

    /** Средняя длина тика в миллисекундах: двадцать это ровный сервер. */
    public double tickMillis() {
        return tickMillis;
    }

    /** Местное время по поясу из {@code utils.toml}. */
    public LocalDateTime time() {
        return time;
    }

    /** День недели местного времени. */
    public DayOfWeek day() {
        return time.getDayOfWeek();
    }

    /** Минуты от полуночи местного времени. */
    public int minuteOfDay() {
        return time.getHour() * 60 + time.getMinute();
    }

    @Override
    public String toString() {
        return "snapshot[players=" + players
            + ", dimensions="
            + dimensions
            + ", tick="
            + tickMillis
            + ", "
            + time
            + "]";
    }
}
