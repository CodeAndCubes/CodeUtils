package com.mrleonardos.codeutils.internal;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codeutils.api.when.When;

public final class WhenBlock {

    @Comment("Минимум игроков онлайн. Ноль снимает.")
    public int minPlayers;

    @Comment("Потолок игроков онлайн. Ноль снимает.")
    public int maxPlayers;

    @Comment("Окно времени суток вида \"ЧЧ:ММ-ЧЧ:ММ\". Пустое снимает. Окно через полночь писать можно.")
    public String between = "";

    @Comment("Дни недели: mon, tue, wed, thu, fri, sat, sun. Пустой список означает все дни.")
    public List<String> days = new ArrayList<>();

    @Comment("Не слать, когда средний тик длиннее этого числа миллисекунд. Ноль снимает.")
    public int maxTickMillis;

    @Comment("Условия из чужих модов по их идентификаторам.")
    public List<String> custom = new ArrayList<>();

    public WhenBlock() {}

    public WhenBlock(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public When toWhen(String where, Logger log) {
        When.Builder builder = When.builder()
            .minPlayers(minPlayers)
            .maxPlayers(maxPlayers)
            .maxTickMillis(maxTickMillis);
        window(where, log, builder);
        for (String written : days == null ? new ArrayList<String>() : days) {
            DayOfWeek day = Moments.day(written);
            if (day == null) {
                log.warn("{}: day {} is not one of mon, tue, wed, thu, fri, sat, sun, it is skipped", where, written);
                continue;
            }
            builder.day(day);
        }
        for (String name : custom == null ? new ArrayList<String>() : custom) {
            builder.custom(name == null ? "" : name.trim());
        }
        return builder.build();
    }

    private void window(String where, Logger log, When.Builder builder) {
        if (!Moments.written(between)) {
            return;
        }
        int from = Moments.windowStart(between);
        int to = Moments.windowEnd(between);
        if (from == Moments.NONE || to == Moments.NONE) {
            log.warn("{}: between = {} is not a window of the form HH:MM-HH:MM, the key is skipped", where, between);
            return;
        }
        builder.window(from, to);
    }
}
