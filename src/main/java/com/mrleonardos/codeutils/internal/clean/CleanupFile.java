package com.mrleonardos.codeutils.internal.clean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigScope;
import com.mrleonardos.codecore.api.config.ConfigSpec;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;

@Comment({ "Очистка сущностей. Правил сколько угодно, имя правила это имя секции.",
    "Имя идёт в /codeutils clean <правило>." })
public final class CleanupFile {

    public static final int VERSION = 1;

    public static final String FILE = "cleanup";
    public static final String FILE_NAME = "utils-cleanup.toml";

    public static final String DROPS = "drops";
    public static final String MOBS = "mobs";

    @Comment({ "Правила очистки. Имя правила это имя секции, и оно же идёт в команду." })
    public Map<String, RuleBlock> rules = factory();

    public static ConfigSpec<CleanupFile> spec() {
        return ConfigSpec.of(UtilsSettings.MODID, FILE, CleanupFile.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(VERSION)
            .defaults(CleanupFile::new)
            .validator(CleanupFile::heal)
            .build();
    }

    private static void heal(CleanupFile file) {
        if (file.rules == null) {
            file.rules = new LinkedHashMap<>();
        }
        for (RuleBlock rule : file.rules.values()) {
            if (rule.types == null) {
                rule.types = new ArrayList<>();
            }
            if (rule.dimensions == null) {
                rule.dimensions = new ArrayList<>();
            }
            if (rule.warnSeconds == null) {
                rule.warnSeconds = new ArrayList<>();
            }
            if (rule.ignoreGuards == null) {
                rule.ignoreGuards = new ArrayList<>();
            }
            if (rule.warnSink == null) {
                rule.warnSink = BroadcastsFile.SINK_CHAT;
            }
            if (rule.when == null) {
                rule.when = new WhenBlock();
            }
        }
    }

    private static Map<String, RuleBlock> factory() {
        Map<String, RuleBlock> made = new LinkedHashMap<>();
        made.put(DROPS, drops());
        made.put(MOBS, mobs());
        return made;
    }

    private static RuleBlock drops() {
        RuleBlock rule = new RuleBlock();
        rule.types = new ArrayList<>(Arrays.asList("@item", "@xp"));
        rule.minAgeSeconds = 60;
        rule.intervalSeconds = 900;
        rule.warnSeconds = new ArrayList<>(Arrays.asList(30, 10));
        return rule;
    }

    private static RuleBlock mobs() {
        RuleBlock rule = new RuleBlock();
        rule.enabled = false;
        rule.types = new ArrayList<>(Arrays.asList("@hostile"));
        rule.minAgeSeconds = 300;
        rule.intervalSeconds = 0;
        rule.worldThreshold = 600;
        rule.chunkThreshold = 40;
        rule.warnSeconds = new ArrayList<>(Arrays.asList(30));
        return rule;
    }

    public static final class RuleBlock {

        @Comment("Ложь оставляет правило в файле, но выключает его.")
        public boolean enabled = true;

        @Comment({ "Кого убирать. Значения списка:", "  точное имя из игры: \"Item\", \"XPOrb\", \"Zombie\", \"Boat\";",
            "  группа с собачкой: @item, @xp, @hostile, @passive, @projectile, @vehicle, @all;",
            "  хвост со звёздочкой: \"Thermal*\";", "  минус впереди вычитает из уже набранного: \"-Wither\".",
            "Список читается слева направо." })
        public List<String> types = new ArrayList<>();

        @Comment("Измерения по номеру. Пустой список означает все загруженные.")
        public List<Integer> dimensions = new ArrayList<>();

        @Comment("Не трогать сущность моложе этого числа секунд.")
        public int minAgeSeconds = 60;

        @Comment("Прогон по расписанию, раз в столько секунд. Ноль снимает расписание.")
        public int intervalSeconds = 900;

        @Comment("Убирать, только когда подходящих сущностей в мире больше этого числа. Ноль снимает порог.")
        public int worldThreshold;

        @Comment("Убирать только в тех чанках, где подходящих сущностей больше этого числа. Ноль снимает порог.")
        public int chunkThreshold;

        @Comment("За сколько секунд предупреждать. Пустой список снимает предупреждение.")
        public List<Integer> warnSeconds = new ArrayList<>();

        @Comment("Куда уходит предупреждение. Встроены \"chat\" и \"log\", остальные приёмники приносят моды.")
        public String warnSink = BroadcastsFile.SINK_CHAT;

        @Comment("Потолок удалений за один проход. Ноль снимает.")
        public int maxRemovals;

        @Comment({ "Снять защиту по имени: named, tamed, leashed, boss, persistent, carrying, ridden, hanging.",
            "Снятая защита означает, что правило удалит и названную бирку, и чужого прирученного волка.",
            "Пустой список оставляет все защиты, и это правильное значение почти всегда." })
        public List<String> ignoreGuards = new ArrayList<>();

        @Comment("Условия правила. Пустой блок означает «всегда».")
        public WhenBlock when = new WhenBlock();
    }
}
