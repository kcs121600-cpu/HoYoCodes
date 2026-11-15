import com.privatecodes.hoyo.BroadcastRules;
import com.privatecodes.hoyo.CodeRules;

public class TestBroadcastRules {
    static void test(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        long now = 1794733200000L;
        test(BroadcastRules.isCodeEvent(CodeRules.HSR, "starrail:livestream-code:abc"), "broadcast marker");
        test(!BroadcastRules.isCodeEvent(CodeRules.HSR, "starrail:livestream-code:schedule:abc"), "schedule excluded");
        test(!BroadcastRules.isCodeEvent(CodeRules.HSR, "starrail:event:abc"), "ordinary event excluded");
        test(!BroadcastRules.isCodeEvent(CodeRules.ZZZ, "starrail:livestream-code:abc"), "wrong game excluded");
        test(BroadcastRules.isCodeEvent(CodeRules.ZZZ, "zzz:livestream-code:abc"), "ZZZ broadcast marker");
        test("ABCD1234".equals(BroadcastRules.cleanCode("abcd1234")), "broadcast token normalized");
        test(BroadcastRules.cleanCode("redeem code now").isEmpty(), "ordinary text rejected");
        test(BroadcastRules.cleanCode("AB12").isEmpty(), "short text rejected");
        test(BroadcastRules.expired("2026-11-14T23:59:59+08:00", now), "expired in UTC+8");
        test(!BroadcastRules.expired("2026-11-18T23:59:59+08:00", now), "active in UTC+8");
        test(!BroadcastRules.expired("", now), "unknown expiry not invented");
        test(!BroadcastRules.tooOld("2026-11-15T20:00:00+08:00", now), "current broadcast");
        test(BroadcastRules.tooOld("2026-10-28T20:00:00+08:00", now), "old broadcasts excluded");
        System.out.println("BroadcastRules: 13 checks PASSED");
    }
}
