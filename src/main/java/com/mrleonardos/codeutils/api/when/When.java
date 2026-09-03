package com.mrleonardos.codeutils.api.when;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Разобранный блок {@code when}: пять встроенных ключей плюс имена чужих условий.
 *
 * <p>
 * Ключи соединяются по И: не выполнен один, не выполнен весь блок. Пустой блок разрешает всё.
 *
 * <p>
 * Имена из {@link #custom()} этот класс не проверяет: их разрешает мод по своему реестру, потому что
 * чужое условие спрашивают уже после заморозки реестра.
 */
public final class When {

    /** Значение, снимающее ключ: ноль игроков, ноль миллисекунд, отсутствующая минута. */
    public static final int UNSET = 0;

    /** Минута, которой не бывает: ей помечено снятое окно времени. */
    public static final int NO_MINUTE = -1;

    private static final int MINUTES_IN_DAY = 24 * 60;

    private static final When ALWAYS = builder().build();

    private final int minPlayers;
    private final int maxPlayers;
    private final int fromMinute;
    private final int toMinute;
    private final Set<DayOfWeek> days;
    private final int maxTickMillis;
    private final List<String> custom;

    private When(Builder builder) {
        this.minPlayers = Math.max(0, builder.minPlayers);
        this.maxPlayers = Math.max(0, builder.maxPlayers);
        this.fromMinute = builder.fromMinute;
        this.toMinute = builder.toMinute;
        this.days = builder.days.isEmpty() ? Collections.<DayOfWeek>emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(builder.days));
        this.maxTickMillis = Math.max(0, builder.maxTickMillis);
        this.custom = Collections.unmodifiableList(new ArrayList<>(builder.custom));
    }

    /** Блок без единого ключа: разрешает всегда. */
    public static When always() {
        return ALWAYS;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Выполнены ли встроенные ключи. Имена из {@link #custom()} сюда не входят. */
    public boolean allows(ServerSnapshot snapshot) {
        if (minPlayers > UNSET && snapshot.players() < minPlayers) {
            return false;
        }
        if (maxPlayers > UNSET && snapshot.players() > maxPlayers) {
            return false;
        }
        if (maxTickMillis > UNSET && snapshot.tickMillis() > maxTickMillis) {
            return false;
        }
        if (!days.isEmpty() && !days.contains(snapshot.day())) {
            return false;
        }
        return inWindow(snapshot.minuteOfDay());
    }

    /** Задано ли окно времени суток. */
    public boolean windowed() {
        return fromMinute != NO_MINUTE && toMinute != NO_MINUTE;
    }

    /** Попадает ли минута от полуночи в окно. Окно через полночь считается как надо. */
    public boolean inWindow(int minuteOfDay) {
        if (!windowed()) {
            return true;
        }
        if (fromMinute == toMinute) {
            return minuteOfDay == fromMinute;
        }
        if (fromMinute < toMinute) {
            return minuteOfDay >= fromMinute && minuteOfDay < toMinute;
        }
        return minuteOfDay >= fromMinute || minuteOfDay < toMinute;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    /** Начало окна в минутах от полуночи или {@link #NO_MINUTE}. */
    public int fromMinute() {
        return fromMinute;
    }

    /** Конец окна в минутах от полуночи или {@link #NO_MINUTE}. */
    public int toMinute() {
        return toMinute;
    }

    /** Дни недели; пустое множество означает все дни. */
    public Set<DayOfWeek> days() {
        return days;
    }

    public int maxTickMillis() {
        return maxTickMillis;
    }

    /** Имена условий из чужих модов. */
    public List<String> custom() {
        return custom;
    }

    /** Не задано ни одного ключа. */
    public boolean empty() {
        return minPlayers == UNSET && maxPlayers == UNSET
            && !windowed()
            && days.isEmpty()
            && maxTickMillis == UNSET
            && custom.isEmpty();
    }

    public static final class Builder {

        private final Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        private final List<String> custom = new ArrayList<>();
        private int minPlayers;
        private int maxPlayers;
        private int fromMinute = NO_MINUTE;
        private int toMinute = NO_MINUTE;
        private int maxTickMillis;

        private Builder() {}

        public Builder minPlayers(int value) {
            this.minPlayers = value;
            return this;
        }

        public Builder maxPlayers(int value) {
            this.maxPlayers = value;
            return this;
        }

        /**
         * Окно времени суток в минутах от полуночи. Начало больше конца означает окно через полночь.
         *
         * @throws IllegalArgumentException если минута выходит за сутки
         */
        public Builder window(int from, int to) {
            check(from);
            check(to);
            this.fromMinute = from;
            this.toMinute = to;
            return this;
        }

        /** Снять окно времени суток. */
        public Builder anyTime() {
            this.fromMinute = NO_MINUTE;
            this.toMinute = NO_MINUTE;
            return this;
        }

        public Builder day(DayOfWeek value) {
            this.days.add(value);
            return this;
        }

        public Builder maxTickMillis(int value) {
            this.maxTickMillis = value;
            return this;
        }

        public Builder custom(String name) {
            if (name != null && !name.isEmpty() && !custom.contains(name)) {
                custom.add(name);
            }
            return this;
        }

        public When build() {
            return new When(this);
        }

        private static void check(int minute) {
            if (minute < 0 || minute >= MINUTES_IN_DAY) {
                throw new IllegalArgumentException(
                    "Minute of day must be 0.." + (MINUTES_IN_DAY - 1) + ", got " + minute);
            }
        }
    }
}
