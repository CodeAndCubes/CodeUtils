package com.mrleonardos.codeutils.internal.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigScope;
import com.mrleonardos.codecore.api.config.ConfigSpec;
import com.mrleonardos.codeutils.api.run.SenderChoice;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.WhenBlock;

@Comment({ "Команды, которые сервер выполняет сам. Заданий сколько угодно, имя задания это имя секции.",
    "Команда пишется без косой черты впереди." })
public final class JobsFile {

    public static final int VERSION = 1;

    public static final String FILE = "jobs";
    public static final String FILE_NAME = "utils-jobs.toml";

    public static final String NIGHT_CLEAN = "night-clean";

    public static final String ON_FAILURE_LOG = "log";
    public static final String ON_FAILURE_QUIET = "quiet";
    public static final String ON_FAILURE_RETRY = "retry";

    public static final int DEFAULT_RETRY_SECONDS = 60;

    public Map<String, JobBlock> jobs = factory();

    public static ConfigSpec<JobsFile> spec() {
        return ConfigSpec.of(UtilsSettings.MODID, FILE, JobsFile.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(VERSION)
            .defaults(JobsFile::new)
            .validator(JobsFile::heal)
            .build();
    }

    private static void heal(JobsFile file) {
        if (file.jobs == null) {
            file.jobs = new LinkedHashMap<>();
        }
        for (JobBlock job : file.jobs.values()) {
            if (job.at == null) {
                job.at = new ArrayList<>();
            }
            if (job.when == null) {
                job.when = new WhenBlock();
            }
            if (job.command == null) {
                job.command = "";
            }
            if (job.as == null) {
                job.as = SenderChoice.CONSOLE_WORD;
            }
        }
    }

    private static Map<String, JobBlock> factory() {
        Map<String, JobBlock> made = new LinkedHashMap<>();
        JobBlock clean = new JobBlock();
        clean.enabled = false;
        clean.command = "codeutils clean drops";
        clean.at = new ArrayList<>(Arrays.asList("04:00"));
        made.put(NIGHT_CLEAN, clean);
        return made;
    }

    public static final class JobBlock {

        @Comment("Ложь оставляет задание в файле, но выключает его.")
        public boolean enabled = true;

        @Comment("Что выполнить.")
        public String command = "";

        @Comment("Раз в столько секунд. Ноль снимает интервал.")
        public int everySeconds;

        @Comment({ "В такое время суток, список моментов вида \"ЧЧ:ММ\". Пустой список снимает.",
            "Интервал и время суток можно задать вместе: сработает и то, и другое." })
        public List<String> at = new ArrayList<>();

        @Comment({ "От чьего имени идёт команда:",
            "\"console\" консоль сервера, ответ уходит в лог, ванильное оповещение операторов остаётся;",
            "\"commandblock\" поддельный командный блок на точке спавна, оповещение операторов",
            "  снимается ванильным правилом commandBlockOutput = false;",
            "\"player:<ник>\" от игрока, и если его нет онлайн, срабатывает политика отказа." })
        public String as = SenderChoice.CONSOLE_WORD;

        @Comment({ "Что делать, когда команда не сработала:",
            "\"log\" строка в лог, \"quiet\" молча, \"retry\" повторить." })
        public String onFailure = ON_FAILURE_LOG;

        @Comment("Через сколько секунд повторять при \"retry\".")
        public int retrySeconds = DEFAULT_RETRY_SECONDS;

        @Comment("Сколько раз повторять. Ноль означает без повторов.")
        public int retries;

        @Comment({ "Сколько секунд после пропущенного момента задание ещё имеет право сработать.",
            "Ноль означает, что момент, пропущенный из-за остановки сервера, не догоняется." })
        public int catchUpSeconds;

        @Comment("Условия задания. Пустой блок означает «всегда».")
        public WhenBlock when = new WhenBlock();
    }
}
