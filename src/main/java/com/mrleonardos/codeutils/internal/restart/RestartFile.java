package com.mrleonardos.codeutils.internal.restart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigScope;
import com.mrleonardos.codecore.api.config.ConfigSpec;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

@Comment({ "Перезапуск сервера. Мод останавливает сервер, а поднимает его обратно обёртка процесса:",
    "служба systemd, pm2 или цикл в bat-файле. Своей команды операционной системы мод не выполняет." })
public final class RestartFile {

    public static final int VERSION = 1;

    public static final String FILE = "restart";
    public static final String FILE_NAME = "utils-restart.toml";

    public static final String DEFAULT_PREFIX = "&8[&cПерезапуск&8]&r ";

    public static final int DEFAULT_CLOSE_DOOR_SECONDS = 30;
    public static final int DEFAULT_KICK_SECONDS = 5;
    public static final int DEFAULT_SETTLE_TICKS = 20;

    public Schedule schedule = new Schedule();

    public Warnings warnings = new Warnings();

    public Steps steps = new Steps();

    public Messages messages = new Messages();

    public static ConfigSpec<RestartFile> spec() {
        return ConfigSpec.of(UtilsSettings.MODID, FILE, RestartFile.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(VERSION)
            .defaults(RestartFile::new)
            .validator(RestartFile::heal)
            .build();
    }

    private static void heal(RestartFile file) {
        if (file.schedule == null) {
            file.schedule = new Schedule();
        }
        if (file.schedule.at == null) {
            file.schedule.at = new ArrayList<>();
        }
        if (file.schedule.days == null) {
            file.schedule.days = new ArrayList<>();
        }
        if (file.schedule.when == null) {
            file.schedule.when = new WhenBlock();
        }
        if (file.warnings == null) {
            file.warnings = new Warnings();
        }
        if (file.warnings.seconds == null) {
            file.warnings.seconds = new ArrayList<>();
        }
        if (file.warnings.sink == null) {
            file.warnings.sink = BroadcastsFile.SINK_CHAT;
        }
        if (file.warnings.prefix == null) {
            file.warnings.prefix = DEFAULT_PREFIX;
        }
        if (file.steps == null) {
            file.steps = new Steps();
        }
        if (file.messages == null) {
            file.messages = new Messages();
        }
        if (file.messages.kickKey == null || file.messages.kickKey.isEmpty()) {
            file.messages.kickKey = UtilsMessages.RESTART_KICK;
        }
        if (file.messages.doorKey == null || file.messages.doorKey.isEmpty()) {
            file.messages.doorKey = UtilsMessages.RESTART_DOOR;
        }
    }

    @Comment("Когда сервер останавливается сам.")
    public static final class Schedule {

        @Comment("Ложь оставляет команду /restart, но снимает расписание.")
        public boolean enabled;

        @Comment({ "Моменты вида \"ЧЧ:ММ\": в это время сервер останавливается.",
            "Предупреждения идут раньше, по списку warnings.seconds, и дверь закрывается раньше тоже." })
        public List<String> at = new ArrayList<>(Arrays.asList("06:00"));

        @Comment("Дни недели: mon, tue, wed, thu, fri, sat, sun. Пустой список означает все дни.")
        public List<String> days = new ArrayList<>();

        @Comment({ "Условия расписания. Пустой блок означает «всегда».",
            "Условие проверяется в тот момент, когда отсчёт начинается, а не в момент остановки." })
        public WhenBlock when = new WhenBlock();
    }

    @Comment("Что игроки видят до остановки.")
    public static final class Warnings {

        @Comment("За сколько секунд до остановки предупреждать. Порядок не важен, повторы отбрасываются.")
        public List<Integer> seconds = new ArrayList<>(Arrays.asList(600, 300, 60, 30, 10, 5, 4, 3, 2, 1));

        @Comment("Куда уходит предупреждение. Встроены \"chat\" и \"log\", остальные приёмники приносят моды.")
        public String sink = BroadcastsFile.SINK_CHAT;

        @Comment("Строка перед каждым предупреждением. Пустая снимает.")
        public String prefix = DEFAULT_PREFIX;
    }

    @Comment("Порядок остановки. Числа считаются от момента остановки назад.")
    public static final class Steps {

        @Comment({ "За сколько секунд до остановки закрыть вход. Ноль закрывает вместе с киком.",
            "Игрок, зашедший в последние секунды, получил бы кик сразу и половину записанного состояния." })
        public int closeDoorSeconds = DEFAULT_CLOSE_DOOR_SECONDS;

        @Comment("За сколько секунд до остановки выкинуть игроков.")
        public int kickSeconds = DEFAULT_KICK_SECONDS;

        @Comment("Сколько тиков подождать после кика, прежде чем писать миры.")
        public int settleTicks = DEFAULT_SETTLE_TICKS;

        @Comment("Писать данные игроков своим вызовом, не полагаясь на остановку сервера.")
        public boolean savePlayers = true;

        @Comment("Писать миры своим вызовом, там же.")
        public boolean saveWorlds = true;
    }

    @Comment("Что игрок читает в окне отключения.")
    public static final class Messages {

        @Comment("Ключ перевода в окне отключения у выкинутого игрока.")
        public String kickKey = UtilsMessages.RESTART_KICK;

        @Comment("Ключ перевода у того, кто стучится в закрытую дверь.")
        public String doorKey = UtilsMessages.RESTART_DOOR;
    }
}
