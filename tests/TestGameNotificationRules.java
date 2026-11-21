import com.privatecodes.hoyo.CodeRules;
import com.privatecodes.hoyo.GameNotificationRules;

import java.util.Arrays;
import java.util.List;

public class TestGameNotificationRules {
    private static final class NewCode {
        final String game;
        final String code;
        NewCode(String game, String code) { this.game = game; this.code = code; }
    }

    private static void test(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }

    private static List<NewCode> filtered(List<NewCode> found, boolean hsr, boolean zzz) {
        return GameNotificationRules.filterNewItems(found, hsr, zzz, item -> item.game);
    }

    public static void main(String[] args) {
        test(GameNotificationRules.DEFAULT_ENABLED, "legacy installations default to both notifications ON");
        List<NewCode> found = Arrays.asList(
                new NewCode(CodeRules.HSR, "HSRNEW01"),
                new NewCode(CodeRules.ZZZ, "ZZZNEW01"));

        List<NewCode> bothOn = filtered(found, true, true);
        test(bothOn.size() == 2, "both ON notifies both games");

        List<NewCode> hsrOnly = filtered(found, true, false);
        test(hsrOnly.size() == 1 && CodeRules.HSR.equals(hsrOnly.get(0).game),
                "HSR ON / ZZZ OFF isolates Star Rail");

        List<NewCode> zzzOnly = filtered(found, false, true);
        test(zzzOnly.size() == 1 && CodeRules.ZZZ.equals(zzzOnly.get(0).game),
                "HSR OFF / ZZZ ON isolates Zenless Zone Zero");

        List<NewCode> bothOff = filtered(found, false, false);
        test(bothOff.isEmpty(), "both OFF sends no code notification");

        test(found.size() == 2, "filter does not remove collected codes");
        test(!GameNotificationRules.isEnabledForGame("unknown", true, true),
                "unknown game is never notified");
        System.out.println("GameNotificationRules: 7 checks PASSED");
    }
}
