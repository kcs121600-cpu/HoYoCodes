package com.privatecodes.hoyo;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-device redeem-code notification preferences stored beside existing app settings. */
public final class CodeNotificationSettings {
    private static final String PREFS = "hoyo_state";
    private static final String HSR_KEY = "notify_new_codes_hkrpg";
    private static final String ZZZ_KEY = "notify_new_codes_nap";
    private CodeNotificationSettings() {}

    public static boolean isEnabled(Context context, String game) {
        String key = keyForGame(game);
        if (key == null) return false;
        return preferences(context).getBoolean(key, GameNotificationRules.DEFAULT_ENABLED);
    }

    public static void setEnabled(Context context, String game, boolean enabled) {
        String key = keyForGame(game);
        if (key == null) return;
        preferences(context).edit().putBoolean(key, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String keyForGame(String game) {
        if (CodeRules.HSR.equals(game)) return HSR_KEY;
        if (CodeRules.ZZZ.equals(game)) return ZZZ_KEY;
        return null;
    }
}
