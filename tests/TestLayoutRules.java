package com.privatecodes.hoyo;

public class TestLayoutRules {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(LayoutRules.twoPane(true, 1150), "Y700 landscape: two panes");
        check(LayoutRules.twoCardColumns(true, 1150), "Y700 landscape: two card columns");
        check(!LayoutRules.twoPane(false, 750), "Y700 portrait: single centered pane");
        check(LayoutRules.portraitContentWidth(750) == 680, "Y700 portrait max width");
        check(!LayoutRules.twoPane(true, 700), "small devices: one pane");
        check(!LayoutRules.twoCardColumns(true, 900), "medium landscape: one card column");
        check(LayoutRules.portraitContentWidth(390) == 390, "phone keeps width");
        check(LayoutRules.listingContentWidth(1150) == 823, "right pane fits available width");
        check(LayoutRules.listingContentWidth(1600) == 1040, "large tablet capped width");
        System.out.println("LayoutRules: 9 checks PASSED");
    }
}
