package com.privatecodes.hoyo;

import java.util.Locale;

/** Pure response checks shared by all remote JSON code feeds. */
public final class SourceResponseRules {
    private SourceResponseRules() {}

    public static boolean isJsonContentType(String contentType) {
        if (contentType == null) return false;
        String mime = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "application/json".equals(mime) ||
                (mime.startsWith("application/") && mime.endsWith("+json"));
    }

    public static String safeContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) return "없음";
        String value = contentType.split("[\\r\\n]", 2)[0].trim();
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    public static boolean isJsonDocument(String body) {
        if (body == null) return false;
        String trimmed = body.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
