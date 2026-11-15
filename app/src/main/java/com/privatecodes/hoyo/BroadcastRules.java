package com.privatecodes.hoyo;

import java.time.Instant;
import java.time.OffsetDateTime;

/** Strict policy for optionally accepting livestream codes found in public calendar feeds. */
public final class BroadcastRules {
    private BroadcastRules() {}
    public static boolean isCodeEvent(String game, String id) {
        if (id == null) return false;
        String gamePrefix;
        if (CodeRules.HSR.equals(game)) gamePrefix = "starrail:";
        else if (CodeRules.ZZZ.equals(game)) gamePrefix = "zzz:";
        else return false;
        return id.startsWith(gamePrefix + "livestream-code:") && !id.contains(":schedule:");
    }
    public static String cleanCode(String raw) {
        String code = CodeRules.clean(raw);
        // Broadcast codes are compact ASCII tokens. Page text, labels, and prose are not codes.
        return code.matches("[A-Z0-9]{8,32}") ? code : "";
    }
    public static boolean expired(String endTime, long nowMillis) {
        if (endTime == null || endTime.trim().isEmpty()) return false;
        try {
            return OffsetDateTime.parse(endTime).toInstant().toEpochMilli() <= nowMillis;
        } catch (Exception ignored) {
            return false; // Unknown time -> cannot invent an expiry.
        }
    }
    public static boolean tooOld(String startTime, long nowMillis) {
        if (startTime == null || startTime.trim().isEmpty()) return false;
        try {
            Instant start = OffsetDateTime.parse(startTime).toInstant();
            return start.toEpochMilli() < nowMillis - 5L * 24 * 3600 * 1000
                || start.toEpochMilli() > nowMillis + 24L * 3600 * 1000;
        } catch (Exception ignored) {
            return false;
        }
    }
}
