package com.mrleonardos.codeutils.internal.queue;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codecore.api.config.ConfigScope;
import com.mrleonardos.codecore.api.config.ConfigSpec;
import com.mrleonardos.codeutils.internal.UtilsSettings;

@Comment({ "Мягкие потолки слотов поверх max-players из server.properties.",
    "Ванильная проверка «сервер полон» идёт раньше нашей и до нас соединение не доводит, поэтому",
    "max-players ставится выше самой верхней ступени. Сервер скажет строкой при старте, если это не так." })
public final class QueueFile {

    public static final int VERSION = 1;

    public static final String FILE = "queue";
    public static final String FILE_NAME = "utils-queue.toml";

    public static final String BY_PERMISSION = "by-permission";

    public static final String VIP = "vip";
    public static final String STAFF = "staff";

    public static final int DEFAULT_BASE_SLOTS = 60;
    public static final int DEFAULT_TICKET_SECONDS = 180;
    public static final int DEFAULT_HOLD_SECONDS = 15;
    public static final int DEFAULT_RETRY_HINT_SECONDS = 20;

    public Slots slots = new Slots();

    public Queue queue = new Queue();

    public static ConfigSpec<QueueFile> spec() {
        return ConfigSpec.of(UtilsSettings.MODID, FILE, QueueFile.class)
            .role(ConfigRoles.UTILS)
            .scope(ConfigScope.SETTINGS)
            .schemaVersion(VERSION)
            .defaults(QueueFile::new)
            .validator(QueueFile::heal)
            .build();
    }

    private static void heal(QueueFile file) {
        if (file.slots == null) {
            file.slots = new Slots();
        }
        if (file.slots.tiers == null) {
            file.slots.tiers = new LinkedHashMap<>();
        }
        for (Tier tier : file.slots.tiers.values()) {
            if (tier.node == null) {
                tier.node = "";
            }
        }
        if (file.queue == null) {
            file.queue = new Queue();
        }
        if (file.queue.policy == null || file.queue.policy.isEmpty()) {
            file.queue.policy = BY_PERMISSION;
        }
    }

    private static Map<String, Tier> tiers() {
        Map<String, Tier> made = new LinkedHashMap<>();
        made.put(VIP, new Tier("codeutils.slots.vip", 70));
        made.put(STAFF, new Tier("codeutils.slots.staff", 80));
        return made;
    }

    @Comment("Потолки слотов. Ступень старше той, у которой слотов меньше.")
    public static final class Slots {

        @Comment({ "Потолок для игрока без ступени.",
            "Ключ назван base, а не public: имя поля идёт в файл как есть, а public в Java занят." })
        public int base = DEFAULT_BASE_SLOTS;

        @Comment("Ступени. Имя ступени это имя секции, старшинство задаёт число слотов.")
        public Map<String, Tier> tiers = tiers();
    }

    public static final class Tier {

        @Comment("Узел права, по которому игрок получает ступень.")
        public String node = "";

        @Comment("Потолок для этой ступени.")
        public int slots;

        public Tier() {}

        public Tier(String node, int slots) {
            this.node = node;
            this.slots = slots;
        }
    }

    @Comment("Очередь поверх потолков.")
    public static final class Queue {

        @Comment("Ложь оставляет мягкие потолки, но снимает очередь: отказ уходит без номера.")
        public boolean enabled = true;

        @Comment({ "Чем считать ступень игрока. Встроена \"by-permission\": ступень даёт узел права.",
            "Остальные политики приносят моды, и они вправе считать ступень по своей базе доноров." })
        public String policy = BY_PERMISSION;

        @Comment("Сколько секунд место в очереди живёт без повторного стука.")
        public int ticketSeconds = DEFAULT_TICKET_SECONDS;

        @Comment("Сколько секунд освободившийся слот ждёт голову очереди, прежде чем достаться любому.")
        public int holdSeconds = DEFAULT_HOLD_SECONDS;

        @Comment("Через сколько секунд просить отказанного игрока постучаться снова.")
        public int retryHintSeconds = DEFAULT_RETRY_HINT_SECONDS;
    }
}
