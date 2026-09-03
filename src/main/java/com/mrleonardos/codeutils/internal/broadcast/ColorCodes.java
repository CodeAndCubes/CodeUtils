package com.mrleonardos.codeutils.internal.broadcast;

public final class ColorCodes {

    public static final char AMPERSAND = '&';

    public static final char SECTION = '§';

    private static final String CODES = "0123456789abcdefklmnor";

    private ColorCodes() {}

    public static String paint(String text) {
        if (text == null || text.indexOf(AMPERSAND) < 0) {
            return text == null ? "" : text;
        }
        StringBuilder painted = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            char symbol = text.charAt(index);
            if (symbol != AMPERSAND || index + 1 >= text.length()) {
                painted.append(symbol);
                index++;
                continue;
            }
            char next = text.charAt(index + 1);
            if (next == AMPERSAND) {
                painted.append(AMPERSAND);
                index += 2;
                continue;
            }
            if (code(next)) {
                painted.append(SECTION)
                    .append(next);
                index += 2;
                continue;
            }
            painted.append(symbol);
            index++;
        }
        return painted.toString();
    }

    public static String strip(String text) {
        if (text == null || text.indexOf(SECTION) < 0) {
            return text == null ? "" : text;
        }
        StringBuilder plain = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            char symbol = text.charAt(index);
            if (symbol == SECTION && index + 1 < text.length() && code(text.charAt(index + 1))) {
                index += 2;
                continue;
            }
            plain.append(symbol);
            index++;
        }
        return plain.toString();
    }

    private static boolean code(char symbol) {
        return CODES.indexOf(Character.toLowerCase(symbol)) >= 0;
    }
}
