package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.config.Comment;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.clean.CleanupFile;
import com.mrleonardos.codeutils.internal.command.CommandRoots;
import com.mrleonardos.codeutils.internal.job.JobsFile;
import com.mrleonardos.codeutils.internal.queue.QueueFile;
import com.mrleonardos.codeutils.internal.restart.RestartFile;

class SettingsLayoutTest {

    private static final List<Class<?>> FILES = Arrays.asList(
        UtilsSettings.class,
        CommandRoots.class,
        BroadcastsFile.class,
        JobsFile.class,
        RestartFile.class,
        CleanupFile.class);

    /**
     * Элементы массива таблиц полями секции не описаны, поэтому комментарий к ним ядро поставить не
     * может. Ключи сообщения перечислены в описании поля messages, то есть в строках прямо над первым
     * сообщением набора.
     */
    private static final List<Class<?>> SILENT_TYPES = Arrays
        .asList(BroadcastsFile.MessageBlock.class, BroadcastsFile.PartBlock.class);

    /**
     * Единственная таблица своего файла: описание стоит шапкой файла двумя строками выше, и второй такой
     * же строкой над первой секцией был бы повтор.
     */
    private static final List<String> SILENT_FIELDS = Arrays.asList("sets", "jobs");

    @Test
    void theSettingsFileHoldsTheSwitchesAndTheZone() {
        assertEquals(
            new TreeSet<>(
                Arrays.asList(
                    "log.slowPassMillis",
                    "log.startupSummary",
                    "on.broadcasts",
                    "on.cleanup",
                    "on.jobs",
                    "on.queue",
                    "on.restart",
                    "timezone")),
            paths(UtilsSettings.class));
    }

    @Test
    void aBroadcastSetHoldsElevenKeys() {
        assertEquals(
            new TreeSet<>(
                Arrays.asList(
                    "dimensions",
                    "enabled",
                    "firstDelaySeconds",
                    "intervalSeconds",
                    "messages",
                    "order",
                    "permission",
                    "prefix",
                    "sink",
                    "when",
                    "whenEmpty")),
            names(BroadcastsFile.SetBlock.class));
    }

    @Test
    void aMessageAndItsPartHoldTheSameKeysApartFromNesting() {
        assertEquals(
            new TreeSet<>(Arrays.asList("click", "hover", "parts", "text")),
            names(BroadcastsFile.MessageBlock.class));
        assertEquals(new TreeSet<>(Arrays.asList("click", "hover", "text")), names(BroadcastsFile.PartBlock.class));
    }

    @Test
    void aJobHoldsElevenKeys() {
        assertEquals(
            new TreeSet<>(
                Arrays.asList(
                    "as",
                    "at",
                    "catchUpSeconds",
                    "command",
                    "enabled",
                    "everySeconds",
                    "onFailure",
                    "retries",
                    "retrySeconds",
                    "runner",
                    "when")),
            names(JobsFile.JobBlock.class));
    }

    @Test
    void theWhenBlockIsOneClassForEverySchedule() {
        assertEquals(
            new TreeSet<>(Arrays.asList("between", "custom", "days", "maxPlayers", "maxTickMillis", "minPlayers")),
            names(WhenBlock.class));
        assertEquals(WhenBlock.class, fieldType(BroadcastsFile.SetBlock.class, "when"));
        assertEquals(WhenBlock.class, fieldType(JobsFile.JobBlock.class, "when"));
    }

    @Test
    void aCleanupRuleHoldsTwelveKeys() {
        assertEquals(
            new TreeSet<>(
                Arrays.asList(
                    "chunkThreshold",
                    "dimensions",
                    "enabled",
                    "ignoreGuards",
                    "intervalSeconds",
                    "maxRemovals",
                    "minAgeSeconds",
                    "types",
                    "warnSeconds",
                    "warnSink",
                    "when",
                    "worldThreshold")),
            names(CleanupFile.RuleBlock.class));
        assertEquals(WhenBlock.class, fieldType(CleanupFile.RuleBlock.class, "when"));
    }

    @Test
    void theRestartFileHoldsFourBlocks() {
        assertEquals(
            new TreeSet<>(Arrays.asList("messages", "schedule", "steps", "warnings")),
            names(RestartFile.class));
        assertEquals(new TreeSet<>(Arrays.asList("at", "days", "enabled", "when")), names(RestartFile.Schedule.class));
        assertEquals(new TreeSet<>(Arrays.asList("prefix", "seconds", "sink")), names(RestartFile.Warnings.class));
        assertEquals(
            new TreeSet<>(Arrays.asList("closeDoorSeconds", "kickSeconds", "savePlayers", "saveWorlds", "settleTicks")),
            names(RestartFile.Steps.class));
        assertEquals(new TreeSet<>(Arrays.asList("doorKey", "kickKey")), names(RestartFile.Messages.class));
        assertEquals(WhenBlock.class, fieldType(RestartFile.Schedule.class, "when"));
    }

    @Test
    void theQueueFileHoldsSlotsAndTheQueueApart() {
        assertEquals(new TreeSet<>(Arrays.asList("queue", "slots")), names(QueueFile.class));
        assertEquals(new TreeSet<>(Arrays.asList("base", "tiers")), names(QueueFile.Slots.class));
        assertEquals(new TreeSet<>(Arrays.asList("node", "slots")), names(QueueFile.Tier.class));
        assertEquals(
            new TreeSet<>(Arrays.asList("enabled", "holdSeconds", "policy", "retryHintSeconds", "ticketSeconds")),
            names(QueueFile.Queue.class));
    }

    @Test
    void aCommandRootRecordHoldsTwoKeys() {
        assertEquals(new TreeSet<>(Arrays.asList("aliases", "enabled")), names(CommandRoots.Entry.class));
    }

    @Test
    void everyFieldOfEveryFileExplainsItself() {
        List<String> silent = new ArrayList<>();
        for (Class<?> type : FILES) {
            collectSilent(type, "", silent);
        }

        assertTrue(silent.isEmpty(), () -> "поле настроек без описания читают по исходникам: " + silent);
    }

    @Test
    void everyFileSaysWhatItIsInItsFirstLines() {
        for (Class<?> type : FILES) {
            Comment header = type.getAnnotation(Comment.class);
            assertTrue(
                header != null && header.value().length > 0,
                () -> "шапка объясняет, что это за файл: " + type.getSimpleName());
        }
    }

    private static Set<String> paths(Class<?> type) {
        Set<String> found = new TreeSet<>();
        collect(type, "", found);
        return found;
    }

    private static Set<String> names(Class<?> type) {
        Set<String> found = new TreeSet<>();
        for (Field field : fields(type)) {
            found.add(field.getName());
        }
        return found;
    }

    private static Class<?> fieldType(Class<?> owner, String name) {
        for (Field field : fields(owner)) {
            if (field.getName()
                .equals(name)) {
                return raw(field.getGenericType());
            }
        }
        return null;
    }

    private static void collect(Class<?> type, String prefix, Set<String> found) {
        for (Field field : fields(type)) {
            Class<?> raw = raw(field.getGenericType());
            String path = prefix + field.getName();
            if (own(raw)) {
                collect(raw, path + ".", found);
                continue;
            }
            found.add(path);
        }
    }

    private static void collectSilent(Class<?> type, String prefix, List<String> silent) {
        for (Field field : fields(type)) {
            Class<?> raw = raw(field.getGenericType());
            Class<?> valueType = raw != null && Map.class.isAssignableFrom(raw) ? mapValue(field.getGenericType())
                : raw;
            String path = prefix + field.getName();
            if (!SILENT_TYPES.contains(type) && !SILENT_FIELDS.contains(field.getName())
                && field.getAnnotation(Comment.class) == null
                && (raw == null || raw.getAnnotation(Comment.class) == null)) {
                silent.add(path);
            }
            if (valueType != null && own(valueType) && !SILENT_TYPES.contains(valueType)) {
                collectSilent(valueType, path + ".", silent);
            }
        }
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> found = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                continue;
            }
            found.add(field);
        }
        return found;
    }

    private static boolean own(Class<?> raw) {
        return raw != null && !Map.class.isAssignableFrom(raw)
            && !Collection.class.isAssignableFrom(raw)
            && raw.getName()
                .startsWith("com.mrleonardos.codeutils");
    }

    private static Class<?> mapValue(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
        return arguments.length == 2 ? raw(arguments[1]) : null;
    }

    private static Class<?> raw(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return raw(((ParameterizedType) type).getRawType());
        }
        return null;
    }
}
