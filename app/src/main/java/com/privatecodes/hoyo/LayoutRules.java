package com.privatecodes.hoyo;

/** Pure dp-based responsive rules; isolated so the Y700 layout can be regression-tested. */
final class LayoutRules {
    static final int SIDEBAR_DP = 326;
    static final int DIVIDER_DP = 1;
    static final int LIST_PADDING_DP = 44;

    static boolean twoPane(boolean landscape, int screenWidthDp) {
        return landscape && screenWidthDp >= 840;
    }
    static boolean twoCardColumns(boolean twoPane, int screenWidthDp) {
        // Two 330dp cards plus 14dp gap, 44dp internal padding and sidebar/divider.
        return twoPane && screenWidthDp >= SIDEBAR_DP + DIVIDER_DP
                + LIST_PADDING_DP + 330 * 2 + 14;
    }
    static int portraitContentWidth(int screenWidthDp) {
        return Math.max(1, Math.min(screenWidthDp, 680));
    }
    static int listingContentWidth(int screenWidthDp) {
        return Math.max(1, Math.min(screenWidthDp - SIDEBAR_DP - DIVIDER_DP, 1040));
    }
    private LayoutRules() {}
}
