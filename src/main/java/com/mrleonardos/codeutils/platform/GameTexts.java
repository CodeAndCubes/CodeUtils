package com.mrleonardos.codeutils.platform;

import com.mrleonardos.codecore.platform.ServerTexts;
import com.mrleonardos.codeutils.internal.UtilsTexts;

/**
 * Строку собирает сервер на языке из главного файла линейки: рассылка и отказ на входе уходят игроку
 * готовым текстом, а клиента с нашим файлом перевода у него нет.
 */
public final class GameTexts implements UtilsTexts {

    @Override
    public String format(String key, Object... arguments) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return ServerTexts.format(key, arguments);
    }
}
