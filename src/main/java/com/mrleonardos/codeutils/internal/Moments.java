package com.mrleonardos.codeutils.internal;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.when.When;

public final class Moments {

    public static final int NONE = -1;

    private static final char TIME_SEPARATOR = ':';
    private static final char WINDOW_SEPARATOR = '-';
    private static final int HOUR_LIMIT = 24;
    private static final int MINUTE_LIMIT = 60;

    private static final String[] DAY_WORDS = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };

    private Moments() {}

    public static int parse(String text) {
        if (text == null) {
            return NONE;
        }
        String written = text.trim();
        int separator = written.indexOf(TIME_SEPARATOR);
        if (separator <= 0 || separator == written.length() - 1) {
            return NONE;
        }
        int hour = number(written.substring(0, separator));
        int minute = number(written.substring(separator + 1));
        if (hour < 0 || hour >= HOUR_LIMIT || minute < 0 || minute >= MINUTE_LIMIT) {
            return NONE;
        }
        return hour * MINUTE_LIMIT + minute;
    }

    public static List<Integer> moments(List<String> at, String where, Logger log) {
        List<Integer> found = new ArrayList<>();
        for (String written : at == null ? new ArrayList<String>() : at) {
            int minute = parse(written);
            if (minute == NONE) {
                log.warn("{}: at holds {}, which is not a moment of the form HH:MM, it is skipped", where, written);
                continue;
            }
            if (!found.contains(Integer.valueOf(minute))) {
                found.add(Integer.valueOf(minute));
            }
        }
        Collections.sort(found);
        return found;
    }

    public static String print(int minuteOfDay) {
        if (minuteOfDay < 0 || minuteOfDay >= When.MINUTES_IN_DAY) {
            return "";
        }
        int hour = minuteOfDay / MINUTE_LIMIT;
        int minute = minuteOfDay % MINUTE_LIMIT;
        return two(hour) + TIME_SEPARATOR + two(minute);
    }

    public static int windowStart(String text) {
        return side(text, true);
    }

    public static int windowEnd(String text) {
        return side(text, false);
    }

    public static boolean window(String text) {
        return windowStart(text) != NONE && windowEnd(text) != NONE;
    }

    public static boolean written(String text) {
        return text != null && !text.trim()
            .isEmpty();
    }

    public static DayOfWeek day(String text) {
        if (text == null) {
            return null;
        }
        String written = text.trim()
            .toLowerCase(Locale.ROOT);
        for (int index = 0; index < DAY_WORDS.length; index++) {
            if (DAY_WORDS[index].equals(written)) {
                return DayOfWeek.of(index + 1);
            }
        }
        return null;
    }

    public static String word(DayOfWeek day) {
        return DAY_WORDS[day.getValue() - 1];
    }

    private static int side(String text, boolean start) {
        if (text == null) {
            return NONE;
        }
        String written = text.trim();
        int separator = written.indexOf(WINDOW_SEPARATOR);
        if (separator <= 0 || separator == written.length() - 1) {
            return NONE;
        }
        return parse(start ? written.substring(0, separator) : written.substring(separator + 1));
    }

    private static int number(String text) {
        String written = text.trim();
        if (written.isEmpty() || written.length() > 2) {
            return -1;
        }
        int value = 0;
        for (int index = 0; index < written.length(); index++) {
            char symbol = written.charAt(index);
            if (symbol < '0' || symbol > '9') {
                return -1;
            }
            value = value * 10 + (symbol - '0');
        }
        return value;
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
