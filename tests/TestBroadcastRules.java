import com.privatecodes.hoyo.BroadcastRules;

public class TestBroadcastRules {
    static void test(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        long now = 1794733200000L;
        test(BroadcastRules.isCodeEvent("starrail:livestream-code:abc"), "broadcast marker");
        test(!BroadcastRules.isCodeEvent("starrail:livestream-code:schedule:abc"), "schedule excluded");
        test(!BroadcastRules.isCodeEvent("starrail:event:abc"), "ordinary event excluded");
        test(BroadcastRules.expired("2026-11-14T23:59:59+08:00", now), "expired in UTC+8");
        test(!BroadcastRules.expired("2026-11-18T23:59:59+08:00", now), "active in UTC+8");
        test(!BroadcastRules.expired("", now), "unknown expiry not invented");
        test(!BroadcastRules.tooOld("2026-11-15T20:00:00+08:00", now), "current broadcast");
        test(BroadcastRules.tooOld("2026-10-28T20:00:00+08:00", now), "old broadcasts excluded");
        System.out.println("BroadcastRules: 8 checks PASSED");
    }
}
