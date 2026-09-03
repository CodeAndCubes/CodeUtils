package com.mrleonardos.codeutils.internal.clean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mrleonardos.codeutils.api.clean.EntityKind;

public final class TypeGroups {

    public static final char MARK = '@';

    public static final String ALL = "@all";

    private static final Map<String, EntityKind> BY_WORD = table();

    private TypeGroups() {}

    public static List<String> words() {
        List<String> all = new ArrayList<>(BY_WORD.keySet());
        all.add(ALL);
        return Collections.unmodifiableList(all);
    }

    public static EntityKind byWord(String written) {
        return BY_WORD.get(word(written));
    }

    public static boolean all(String written) {
        return ALL.equals(word(written));
    }

    public static boolean group(String written) {
        return written != null && !written.isEmpty() && written.charAt(0) == MARK;
    }

    private static String word(String written) {
        return written == null ? ""
            : written.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static Map<String, EntityKind> table() {
        Map<String, EntityKind> made = new LinkedHashMap<>();
        made.put("@item", EntityKind.ITEM);
        made.put("@xp", EntityKind.XP);
        made.put("@hostile", EntityKind.HOSTILE);
        made.put("@passive", EntityKind.PASSIVE);
        made.put("@projectile", EntityKind.PROJECTILE);
        made.put("@vehicle", EntityKind.VEHICLE);
        return Collections.unmodifiableMap(made);
    }
}
