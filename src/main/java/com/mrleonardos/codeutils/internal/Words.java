package com.mrleonardos.codeutils.internal;

import java.util.Locale;

public final class Words {

    private Words() {}

    public static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
