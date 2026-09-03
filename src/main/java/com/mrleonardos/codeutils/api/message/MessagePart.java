package com.mrleonardos.codeutils.api.message;

import java.util.Objects;

/**
 * Кусок строки со своим оформлением: текст, подсказка при наведении и щелчок.
 *
 * <p>
 * Части живут одним уровнем: у части частей не бывает. Строка из нескольких частей это способ дать
 * разным словам одной строки разные щелчки, а не дерево.
 *
 * <p>
 * Цвета остаются в самом тексте служебными знаками игры. Перевод амперсанда в них делает мод при чтении
 * файла, поэтому сюда текст приходит уже готовым к отправке.
 */
public final class MessagePart {

    private final String text;
    private final String hover;
    private final ClickKind clickKind;
    private final String clickValue;

    private MessagePart(String text, String hover, ClickKind clickKind, String clickValue) {
        this.text = text;
        this.hover = hover;
        this.clickKind = clickKind;
        this.clickValue = clickValue;
    }

    /** Часть из одного текста, без подсказки и щелчка. */
    public static MessagePart of(String text) {
        return new MessagePart(Objects.requireNonNull(text, "text"), null, null, null);
    }

    /** Та же часть с подсказкой при наведении. Пустая подсказка снимает прежнюю. */
    public MessagePart hover(String value) {
        String cleaned = value == null || value.isEmpty() ? null : value;
        return new MessagePart(text, cleaned, clickKind, clickValue);
    }

    /** Та же часть со щелчком. */
    public MessagePart click(ClickKind kind, String value) {
        if (kind == null || value == null || value.isEmpty()) {
            return new MessagePart(text, hover, null, null);
        }
        return new MessagePart(text, hover, kind, value);
    }

    public String text() {
        return text;
    }

    /** Подсказка при наведении или {@code null}, если её нет. */
    public String hover() {
        return hover;
    }

    /** Что делает щелчок или {@code null}, если щелчка нет. */
    public ClickKind clickKind() {
        return clickKind;
    }

    /** Значение щелчка: адрес, команда или подставляемая строка. */
    public String clickValue() {
        return clickValue;
    }

    /** Есть ли у части подсказка при наведении. */
    public boolean hovers() {
        return hover != null;
    }

    /** Есть ли у части щелчок. */
    public boolean clicks() {
        return clickKind != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MessagePart)) {
            return false;
        }
        MessagePart part = (MessagePart) other;
        return text.equals(part.text) && Objects.equals(hover, part.hover)
            && clickKind == part.clickKind
            && Objects.equals(clickValue, part.clickValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, hover, clickKind, clickValue);
    }

    @Override
    public String toString() {
        return "part[" + text
            + (hover == null ? "" : ", hover=" + hover)
            + (clickKind == null ? "" : ", " + clickKind.prefix() + ":" + clickValue)
            + "]";
    }
}
