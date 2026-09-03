package com.mrleonardos.codeutils.api.message;

import java.util.Locale;

/**
 * Что делает щелчок по сообщению.
 *
 * <p>
 * Три действия, и других в 1.7.10 для сервера нет: открытие файла и переход к каналу Twitch помечены
 * запретом на отправку с сервера, а копирования в буфер обмена в этой версии не существует вовсе.
 *
 * <p>
 * В файле настроек действие пишется приставкой перед значением: {@code url:https://example.com},
 * {@code run:spawn}, {@code suggest:/pay }. Команда из {@code run:} выполняется от имени того, кто
 * щёлкнул, и с его правами.
 */
public enum ClickKind {

    /** Открыть адрес в браузере: подтверждение показывает сам клиент. */
    OPEN_URL("url"),

    /** Выполнить команду от имени щёлкнувшего. */
    RUN_COMMAND("run"),

    /** Подставить строку в поле ввода чата, не отправляя её. */
    SUGGEST_COMMAND("suggest");

    private final String prefix;

    ClickKind(String prefix) {
        this.prefix = prefix;
    }

    /** Приставка, которой действие пишется в файле настроек. */
    public String prefix() {
        return prefix;
    }

    /** Действие по приставке из файла или {@code null}, если такой приставки нет. */
    public static ClickKind byPrefix(String value) {
        if (value == null) {
            return null;
        }
        String written = value.trim()
            .toLowerCase(Locale.ROOT);
        for (ClickKind kind : values()) {
            if (kind.prefix.equals(written)) {
                return kind;
            }
        }
        return null;
    }
}
