package com.mrleonardos.codeutils.internal;

/**
 * Переводчик, которого нет: отдаёт ключ и подставленные значения одной строкой.
 *
 * <p>
 * Настоящий перевод берёт строку из {@code .lang} внутри jar силами игры, и проверять здесь нечего:
 * тесту важно, что ушёл нужный ключ с нужными числами.
 */
public final class FakeTexts implements UtilsTexts {

    @Override
    public String format(String key, Object... arguments) {
        if (arguments == null || arguments.length == 0) {
            return key;
        }
        StringBuilder text = new StringBuilder(key).append('[');
        for (int index = 0; index < arguments.length; index++) {
            if (index > 0) {
                text.append(", ");
            }
            text.append(arguments[index]);
        }
        return text.append(']')
            .toString();
    }
}
