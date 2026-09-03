package com.mrleonardos.codeutils.internal;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigScope;
import com.mrleonardos.codecore.api.config.ConfigSpec;
import com.mrleonardos.codeutils.api.Subsystems;

@Comment({ "Служебные подсистемы CodeUtils. Здесь выключатели и то, что общее для всех пяти.",
    "Содержимое каждой подсистемы лежит в своём файле рядом." })
public final class UtilsSettings {

    public static final String MODID = "codeutils";

    public static final int SETTINGS_VERSION = 1;

    public static final String SYSTEM_ZONE = "system";

    public static final int DEFAULT_SLOW_PASS_MILLIS = 20;

    @Comment({ "Часовой пояс всего, что считается по времени суток: задания, перезапуск, окна условий.",
        "\"system\" берёт пояс машины, иначе имя зоны вида \"Europe/Moscow\"." })
    public String timezone = SYSTEM_ZONE;

    public On on = new On();

    public Log log = new Log();

    public static ConfigSpec<UtilsSettings> spec() {
        return ConfigSpec.settings(MODID, UtilsSettings.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(SETTINGS_VERSION)
            .defaults(UtilsSettings::new)
            .validator(UtilsSettings::heal)
            .build();
    }

    public ZoneId zone() {
        String written = timezone == null ? "" : timezone.trim();
        if (written.isEmpty() || SYSTEM_ZONE.equalsIgnoreCase(written)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(written);
        } catch (DateTimeException unknown) {
            return ZoneId.systemDefault();
        }
    }

    public ZoneId zone(Logger log) {
        ZoneId chosen = zone();
        String written = timezone == null ? "" : timezone.trim();
        boolean asked = !written.isEmpty() && !SYSTEM_ZONE.equalsIgnoreCase(written);
        if (asked && !chosen.getId()
            .equals(written)) {
            log.warn("timezone = {} is not a known zone id, the machine zone {} is used", written, chosen.getId());
        }
        return chosen;
    }

    public boolean enabled(String subsystem) {
        String name = subsystem == null ? "" : subsystem.toLowerCase(Locale.ROOT);
        switch (name) {
            case Subsystems.BROADCASTS:
                return on.broadcasts;
            case Subsystems.JOBS:
                return on.jobs;
            case Subsystems.RESTART:
                return on.restart;
            case Subsystems.CLEANUP:
                return on.cleanup;
            case Subsystems.QUEUE:
                return on.queue;
            default:
                return false;
        }
    }

    public boolean startupSummary() {
        return log.startupSummary;
    }

    public int slowPassMillis() {
        return Math.max(0, log.slowPassMillis);
    }

    private static void heal(UtilsSettings settings) {
        if (settings.on == null) {
            settings.on = new On();
        }
        if (settings.log == null) {
            settings.log = new Log();
        }
        if (settings.timezone == null) {
            settings.timezone = SYSTEM_ZONE;
        }
    }

    @Comment({ "Подсистемы. Ложь снимает подсистему целиком: файл не создаётся, события не подписываются,",
        "такт таймера на неё не тратится." })
    public static final class On {

        @Comment("Рассылка сообщений по расписанию, файл utils-broadcasts.toml.")
        public boolean broadcasts = true;

        @Comment("Запуск команд по расписанию, файл utils-jobs.toml.")
        public boolean jobs = true;

        @Comment("Перезапуск сервера, файл utils-restart.toml.")
        public boolean restart = true;

        @Comment("Очистка сущностей, файл utils-cleanup.toml.")
        public boolean cleanup = true;

        @Comment({ "Мягкие потолки слотов и очередь на вход, файл utils-queue.toml.",
            "Заводское значение ложь: подсистеме нужен max-players выше самой верхней ступени,",
            "и без правки server.properties она не сделает ничего." })
        public boolean queue = false;
    }

    @Comment({ "Что мод пишет в лог сам по себе.", "Записи о действиях приходят из секции [audit] главного файла." })
    public static final class Log {

        @Comment("Сводка при старте: включённые подсистемы, число наборов, заданий и правил.")
        public boolean startupSummary = true;

        @Comment("Проход очистки дольше этого числа миллисекунд получает отдельную строку. Ноль снимает.")
        public int slowPassMillis = DEFAULT_SLOW_PASS_MILLIS;
    }
}
