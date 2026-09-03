package com.mrleonardos.codeutils.internal.broadcast;

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

@Comment({ "Наборы сообщений, которые сервер шлёт сам. Наборов сколько угодно, имя набора это имя секции.",
    "Оно же идёт в /codeutils broadcast <набор> и в /codeutils status.", "",
    "Сообщение набора это секция [[sets.<набор>.messages]] с ключами text, hover, click и parts.",
    "Порядок сообщений в файле это порядок при order = \"cycle\".",
    "click пишется как \"url:<адрес>\", \"run:<команда>\" или \"suggest:<строка>\";",
    "команда из \"run:\" выполняется от имени того, кто щёлкнул, и с его правами.",
    "parts задаёт части одной строки, у каждой части те же text, hover и click;",
    "собственный text сообщения при этом идёт первой частью строки.",
    "Цвета через амперсанд: &a, &l, &r. Два амперсанда подряд дают сам знак." })
public final class BroadcastsFile {

    public static final int VERSION = 1;

    public static final String FILE = "broadcasts";
    public static final String FILE_NAME = "utils-broadcasts.toml";

    public static final String TIPS = "tips";
    public static final String LINKS = "links";

    public static final String SINK_CHAT = "chat";
    public static final String SINK_LOG = "log";

    public static final String ORDER_CYCLE = "cycle";
    public static final String ORDER_RANDOM = "random";
    public static final String ORDER_SHUFFLE = "shuffle";

    public static final String EMPTY_HOLD = "hold";
    public static final String EMPTY_SKIP = "skip";
    public static final String EMPTY_LOG = "log";

    public static final String DEFAULT_PREFIX = "&8[&bСервер&8]&r ";

    public Map<String, SetBlock> sets = factory();

    public static ConfigSpec<BroadcastsFile> spec() {
        return ConfigSpec.of(UtilsSettings.MODID, FILE, BroadcastsFile.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(VERSION)
            .defaults(BroadcastsFile::new)
            .validator(BroadcastsFile::heal)
            .build();
    }

    private static void heal(BroadcastsFile file) {
        if (file.sets == null) {
            file.sets = new LinkedHashMap<>();
        }
        for (SetBlock set : file.sets.values()) {
            if (set.dimensions == null) {
                set.dimensions = new ArrayList<>();
            }
            if (set.messages == null) {
                set.messages = new ArrayList<>();
            }
            if (set.when == null) {
                set.when = new WhenBlock();
            }
        }
    }

    private static Map<String, SetBlock> factory() {
        Map<String, SetBlock> made = new LinkedHashMap<>();
        made.put(TIPS, tips());
        made.put(LINKS, links());
        return made;
    }

    private static SetBlock tips() {
        SetBlock set = new SetBlock();
        set.messages = new ArrayList<>(
            Arrays.asList(
                MessageBlock.of("&7Подсказка: &a/home&7 вернёт домой, &a/sethome&7 поставит дом."),
                MessageBlock.of("&7Подсказка: &a/pay <ник> <сумма>&7 переводит деньги другому игроку."),
                MessageBlock.of("&7Подсказка: &a/back&7 возвращает туда, откуда вы телепортировались.")));
        return set;
    }

    private static SetBlock links() {
        SetBlock set = new SetBlock();
        set.intervalSeconds = 1800;
        set.firstDelaySeconds = 600;
        set.messages = new ArrayList<>(
            Arrays.asList(
                MessageBlock.link(
                    "&7Правила сервера: &b&nнажмите сюда",
                    "&fОткрыть правила в браузере",
                    "url:https://example.com/rules"),
                MessageBlock.link(
                    "&7Голосование за сервер: &b&nнажмите сюда",
                    "&fОткрыть страницу голосования",
                    "url:https://example.com/vote")));
        return set;
    }

    public static final class SetBlock {

        @Comment("Ложь оставляет набор в файле, но выключает его.")
        public boolean enabled = true;

        @Comment({ "Сколько секунд между сообщениями набора.",
            "Ноль снимает расписание: набор уходит только по /codeutils broadcast <набор>." })
        public int intervalSeconds = 600;

        @Comment({ "Через сколько секунд после старта сервера уйдёт первое сообщение.",
            "Разные числа у разных наборов разводят их во времени." })
        public int firstDelaySeconds = 120;

        @Comment({ "Порядок: \"cycle\" по кругу, \"random\" случайно с повторами,",
            "\"shuffle\" случайно без повторов до конца круга." })
        public String order = ORDER_CYCLE;

        @Comment({ "Что делать, когда на сервере никого: \"hold\" не слать и не двигать указатель,",
            "\"skip\" двигать указатель без отправки, \"log\" писать строку в лог сервера." })
        public String whenEmpty = EMPTY_HOLD;

        @Comment("Куда уходит сообщение. Встроены \"chat\" и \"log\", остальные приёмники приносят моды.")
        public String sink = SINK_CHAT;

        @Comment("Строка перед каждым сообщением набора. Пустая снимает.")
        public String prefix = DEFAULT_PREFIX;

        @Comment("Кому слать: узел права. Пустой означает всех.")
        public String permission = "";

        @Comment("Кому слать: измерения по номеру. Пустой список означает все.")
        public List<Integer> dimensions = new ArrayList<>();

        @Comment("Сообщения набора. Порядок в файле это порядок при order = \"cycle\", ключи описаны в шапке файла.")
        public List<MessageBlock> messages = new ArrayList<>();

        @Comment("Условия набора. Пустой блок означает «всегда».")
        public WhenBlock when = new WhenBlock(1);
    }

    public static final class MessageBlock {

        public String text = "";

        public String hover;

        public String click;

        public List<PartBlock> parts;

        public static MessageBlock of(String text) {
            MessageBlock block = new MessageBlock();
            block.text = text;
            return block;
        }

        public static MessageBlock link(String text, String hover, String click) {
            MessageBlock block = of(text);
            block.hover = hover;
            block.click = click;
            return block;
        }
    }

    public static final class PartBlock {

        public String text = "";

        public String hover;

        public String click;

        public static PartBlock of(String text) {
            PartBlock block = new PartBlock();
            block.text = text;
            return block;
        }
    }
}
