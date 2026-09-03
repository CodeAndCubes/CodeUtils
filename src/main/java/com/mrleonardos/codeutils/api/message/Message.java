package com.mrleonardos.codeutils.api.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Готовое сообщение рассылки: одна строка чата, собранная из частей.
 *
 * <p>
 * Части идут подряд и склеиваются в одну строку. Простое сообщение это одна часть, сообщение с разными
 * щелчками у разных слов это несколько частей.
 */
public final class Message {

    private static final Message EMPTY = new Message(Collections.<MessagePart>emptyList());

    private final List<MessagePart> parts;

    private Message(List<MessagePart> parts) {
        this.parts = Collections.unmodifiableList(parts);
    }

    /** Сообщение из перечисленных частей. */
    public static Message of(MessagePart... parts) {
        return of(Arrays.asList(parts));
    }

    /** Сообщение из списка частей. Пустые части в него не попадают. */
    public static Message of(List<MessagePart> parts) {
        List<MessagePart> kept = new ArrayList<>();
        for (MessagePart part : parts) {
            if (part != null && !part.text()
                .isEmpty()) {
                kept.add(part);
            }
        }
        return kept.isEmpty() ? EMPTY : new Message(kept);
    }

    /** Сообщение из одного текста. */
    public static Message text(String text) {
        return of(MessagePart.of(Objects.requireNonNull(text, "text")));
    }

    /** Сообщение без единой части: такое не отправляется. */
    public static Message empty() {
        return EMPTY;
    }

    public List<MessagePart> parts() {
        return parts;
    }

    /** Нечего отправлять. */
    public boolean blank() {
        return parts.isEmpty();
    }

    /** Тот же текст одной строкой, без подсказок и щелчков. */
    public String flat() {
        StringBuilder text = new StringBuilder();
        for (MessagePart part : parts) {
            text.append(part.text());
        }
        return text.toString();
    }

    /** То же сообщение со строкой впереди. Пустая строка ничего не меняет. */
    public Message withPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || parts.isEmpty()) {
            return this;
        }
        List<MessagePart> together = new ArrayList<>();
        together.add(MessagePart.of(prefix));
        together.addAll(parts);
        return new Message(together);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Message && parts.equals(((Message) other).parts);
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

    @Override
    public String toString() {
        return "message" + parts;
    }
}
