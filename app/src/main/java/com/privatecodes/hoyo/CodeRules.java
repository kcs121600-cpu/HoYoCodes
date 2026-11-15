package com.privatecodes.hoyo;

import java.util.Locale;

/** Pure-Java rules; no login/session/HoYo account access. */
public final class CodeRules {
    private CodeRules() {}
    public static final String HSR = "hkrpg";
    public static final String ZZZ = "nap";
    public static final String NEW = "new";
    public static final String REVIEW = "review";
    public static final String USED = "used";
    public static final String EXPIRED = "expired";

    public static String clean(String value) {
        if (value == null) return "";
        String result = value.trim().toUpperCase(Locale.ROOT);
        if (!result.matches("[A-Z0-9]{4,32}")) return "";
        return result;
    }
    public static String safeGame(String game) {
        return ZZZ.equals(game) ? ZZZ : HSR;
    }
    public static String redeemUrl(String game, String rawCode) {
        String code = clean(rawCode);
        if (code.isEmpty()) throw new IllegalArgumentException("Invalid code");
        String base = ZZZ.equals(game)
                ? "https://zenless.hoyoverse.com/redemption?code="
                : "https://hsr.hoyoverse.com/gift?code=";
        return base + code;
    }
    public static String gameLabel(String game) {
        return ZZZ.equals(game) ? "젠레스 존 제로" : "붕괴: 스타레일";
    }
}
