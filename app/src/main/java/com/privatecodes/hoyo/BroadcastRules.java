package com.privatecodes.hoyo;

import java.time.Instant;
import java.time.OffsetDateTime;

/** Strict policy for optionally accepting livestream codes found in public calendar feeds. */
public final class BroadcastRules {
    private BroadcastRules() {}
    public static boolean isCodeEvent(String id) {
        return id != null && id.contains(":livestream-code:") && !id.contains(":schedule:");
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
