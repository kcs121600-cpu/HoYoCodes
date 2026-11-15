import com.privatecodes.hoyo.CodeRules;

public class TestCodeRules {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check("HSR1234".equals(CodeRules.clean(" hsr1234 ")), "normalize");
        check(CodeRules.clean("h4/cookie").isEmpty(), "invalid chars blocked");
        check(CodeRules.clean(" ").isEmpty(), "blank rejected");
        check(CodeRules.redeemUrl(CodeRules.HSR, "A1B2C3").equals("https://hsr.hoyoverse.com/gift?code=A1B2C3"), "HSR URL");
        check(CodeRules.redeemUrl(CodeRules.ZZZ, "ZZZTEST1").equals("https://zenless.hoyoverse.com/redemption?code=ZZZTEST1"), "ZZZ URL");
        check(CodeRules.gameLabel(CodeRules.ZZZ).contains("젠레스"), "game name");
        System.out.println("PASS: 6 pure-Java tests");
    }
}
