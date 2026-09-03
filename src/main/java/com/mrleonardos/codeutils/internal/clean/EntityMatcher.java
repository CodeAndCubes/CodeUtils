package com.mrleonardos.codeutils.internal.clean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.clean.EntityKind;
import com.mrleonardos.codeutils.api.clean.EntityView;

public final class EntityMatcher {

    public static final char MINUS = '-';

    public static final char STAR = '*';

    private final List<Line> lines;

    private EntityMatcher(List<Line> lines) {
        this.lines = lines;
    }

    public static EntityMatcher of(List<String> types, String where, Logger log) {
        List<Line> lines = new ArrayList<>();
        for (String written : types == null ? new ArrayList<String>() : types) {
            Line line = line(written, where, log);
            if (line != null) {
                lines.add(line);
            }
        }
        return new EntityMatcher(lines);
    }

    public boolean empty() {
        return lines.isEmpty();
    }

    /**
     * Список читается слева направо: точное имя и хвост со звёздочкой добавляют, минус вычитает.
     */
    public boolean matches(EntityView entity) {
        boolean chosen = false;
        for (Line line : lines) {
            if (line.hits(entity)) {
                chosen = !line.minus;
            }
        }
        return chosen;
    }

    private static Line line(String written, String where, Logger log) {
        String text = written == null ? "" : written.trim();
        if (text.isEmpty()) {
            return null;
        }
        boolean minus = text.charAt(0) == MINUS;
        String body = minus ? text.substring(1)
            .trim() : text;
        if (body.isEmpty()) {
            log.warn("{}: types holds a lonely minus, it is skipped", where);
            return null;
        }
        if (TypeGroups.group(body)) {
            if (TypeGroups.all(body)) {
                return new Line(minus, null, "", true);
            }
            EntityKind kind = TypeGroups.byWord(body);
            if (kind == null) {
                log.warn(
                    "{}: types holds the group {}, which is none of {}, it is skipped",
                    where,
                    body,
                    String.join(", ", TypeGroups.words()));
                return null;
            }
            return new Line(minus, kind, "", false);
        }
        return new Line(minus, null, body, false);
    }

    private static final class Line {

        private final boolean minus;
        private final EntityKind kind;
        private final String name;
        private final boolean prefix;
        private final boolean all;

        private Line(boolean minus, EntityKind kind, String name, boolean all) {
            this.minus = minus;
            this.kind = kind;
            this.all = all;
            this.prefix = !name.isEmpty() && name.charAt(name.length() - 1) == STAR;
            this.name = (prefix ? name.substring(0, name.length() - 1) : name).toLowerCase(Locale.ROOT);
        }

        private boolean hits(EntityView entity) {
            if (all) {
                return entity.kind() != EntityKind.PLAYER;
            }
            if (kind != null) {
                return entity.kind() == kind;
            }
            String type = entity.type() == null ? ""
                : entity.type()
                    .toLowerCase(Locale.ROOT);
            return prefix ? type.startsWith(name) : type.equals(name);
        }
    }
}
