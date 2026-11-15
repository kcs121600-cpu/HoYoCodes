package com.privatecodes.hoyo;

/** Pure response-envelope rules for the supported GameCal API layouts. */
public final class LivestreamResponseRules {
    private LivestreamResponseRules() {}

    public enum Layout {
        TOP_LEVEL_ARRAY,
        ROOT_EVENTS,
        DATA_ARRAY,
        DATA_EVENTS,
        INVALID
    }

    public static Layout selectLayout(boolean topLevelArray, boolean rootEvents,
                                      boolean dataArray, boolean dataEvents) {
        if (topLevelArray) return Layout.TOP_LEVEL_ARRAY;
        if (rootEvents) return Layout.ROOT_EVENTS;
        if (dataArray) return Layout.DATA_ARRAY;
        if (dataEvents) return Layout.DATA_EVENTS;
        return Layout.INVALID;
    }

    /** Older wrappers may omit the API code; when present it must be numeric 200. */
    public static boolean apiCodeIsValid(boolean codePresent, Object code) {
        return !codePresent || apiCodeIsSuccessful(code);
    }

    public static boolean apiCodeIsSuccessful(Object code) {
        return code instanceof Number && ((Number) code).doubleValue() == 200.0;
    }
}
