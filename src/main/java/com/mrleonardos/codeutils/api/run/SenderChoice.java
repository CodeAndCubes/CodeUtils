package com.mrleonardos.codeutils.api.run;

import java.util.Locale;

/**
 * От чьего имени задание выполняет команду.
 *
 * <p>
 * В файле пишется одним из трёх значений: {@code console}, {@code commandblock} или
 * {@code player:<ник>}. Незнакомое значение читается как {@code console}, о чём мод говорит строкой в
 * логе: молча подменить отправителя нельзя, права у них разные.
 *
 * <p>
 * Смысл поддельного командного блока в ванильном правиле {@code commandBlockOutput}: сервер проверяет
 * его только для отправителя этого вида, и при {@code false} оповещение операторов о выполненной команде
 * не уходит. Консоль такого правила не знает, и каждое ночное задание светилось бы всем операторам.
 */
public final class SenderChoice {

    /** Как отправитель пишется в файле. */
    public static final String CONSOLE_WORD = "console";

    /** Как поддельный командный блок пишется в файле. */
    public static final String COMMAND_BLOCK_WORD = "commandblock";

    /** Приставка отправителя-игрока. */
    public static final String PLAYER_PREFIX = "player:";

    private static final SenderChoice CONSOLE = new SenderChoice(Kind.CONSOLE, "", CONSOLE_WORD, true);
    private static final SenderChoice COMMAND_BLOCK = new SenderChoice(
        Kind.COMMAND_BLOCK,
        "",
        COMMAND_BLOCK_WORD,
        true);

    /** Вид отправителя. */
    public enum Kind {

        /** Сам сервер: ответ уходит в лог, ванильное оповещение операторов остаётся. */
        CONSOLE,

        /** Поддельный командный блок на точке спавна нулевого мира. */
        COMMAND_BLOCK,

        /** Игрок, который сейчас на сервере. */
        PLAYER
    }

    private final Kind kind;
    private final String nick;
    private final String written;
    private final boolean known;

    private SenderChoice(Kind kind, String nick, String written, boolean known) {
        this.kind = kind;
        this.nick = nick;
        this.written = written;
        this.known = known;
    }

    /** Консоль сервера. */
    public static SenderChoice console() {
        return CONSOLE;
    }

    /** Поддельный командный блок. */
    public static SenderChoice commandBlock() {
        return COMMAND_BLOCK;
    }

    /** Игрок по нику. */
    public static SenderChoice player(String nick) {
        return new SenderChoice(Kind.PLAYER, nick, PLAYER_PREFIX + nick, true);
    }

    /**
     * Разобрать значение ключа {@code as}.
     *
     * <p>
     * Незнакомая строка и {@code player:} без ника дают консоль с {@link #known()} равным лжи: вызвавший
     * решает, писать ли о подмене в лог.
     */
    public static SenderChoice parse(String raw) {
        String written = raw == null ? "" : raw.trim();
        String lower = written.toLowerCase(Locale.ROOT);
        if (lower.isEmpty() || CONSOLE_WORD.equals(lower)) {
            return lower.isEmpty() ? unknown(written) : CONSOLE;
        }
        if (COMMAND_BLOCK_WORD.equals(lower)) {
            return COMMAND_BLOCK;
        }
        if (lower.startsWith(PLAYER_PREFIX)) {
            String nick = written.substring(PLAYER_PREFIX.length())
                .trim();
            return nick.isEmpty() ? unknown(written) : player(nick);
        }
        return unknown(written);
    }

    public Kind kind() {
        return kind;
    }

    /** Ник игрока; пустая строка у консоли и командного блока. */
    public String nick() {
        return nick;
    }

    /** Значение, как оно написано в файле. */
    public String written() {
        return written;
    }

    /** Понял ли мод написанное. Ложь означает подмену на консоль. */
    public boolean known() {
        return known;
    }

    @Override
    public String toString() {
        return known ? written : written + " (не понято, идёт консоль)";
    }

    private static SenderChoice unknown(String written) {
        return new SenderChoice(Kind.CONSOLE, "", written, false);
    }
}
