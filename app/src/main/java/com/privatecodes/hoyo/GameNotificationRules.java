package com.privatecodes.hoyo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Pure per-game filtering for notifications; it never changes collected code records. */
public final class GameNotificationRules {
    public static final boolean DEFAULT_ENABLED = true;
    private GameNotificationRules() {}

    public static boolean isEnabledForGame(String game, boolean starRailEnabled, boolean zzzEnabled) {
        if (CodeRules.HSR.equals(game)) return starRailEnabled;
        if (CodeRules.ZZZ.equals(game)) return zzzEnabled;
        return false;
    }

    /** Filters only the new rows passed by CodeChecker after successful database insertion. */
    public static <T> List<T> filterNewItems(List<T> newItems,
                                              boolean starRailEnabled,
                                              boolean zzzEnabled,
                                              Function<? super T, String> gameOf) {
        List<T> allowed = new ArrayList<>();
        if (newItems == null || gameOf == null) return allowed;
        for (T item : newItems) {
            if (item != null && isEnabledForGame(gameOf.apply(item), starRailEnabled, zzzEnabled)) {
                allowed.add(item);
            }
        }
        return allowed;
    }
}
