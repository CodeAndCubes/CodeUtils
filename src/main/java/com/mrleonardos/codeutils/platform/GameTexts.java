package com.mrleonardos.codeutils.platform;

import net.minecraft.util.StatCollector;

import com.mrleonardos.codeutils.internal.UtilsTexts;

public final class GameTexts implements UtilsTexts {

    @Override
    public String format(String key, Object... arguments) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return arguments == null || arguments.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, arguments);
    }
}
