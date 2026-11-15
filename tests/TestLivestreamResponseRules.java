import com.privatecodes.hoyo.BroadcastRules;
import com.privatecodes.hoyo.CodeRules;
import com.privatecodes.hoyo.LivestreamResponseRules;

public class TestLivestreamResponseRules {
    private static void test(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        test(LivestreamResponseRules.selectLayout(true, false, false, false)
                == LivestreamResponseRules.Layout.TOP_LEVEL_ARRAY, "top-level array remains supported");
        test(LivestreamResponseRules.selectLayout(false, true, false, false)
                == LivestreamResponseRules.Layout.ROOT_EVENTS, "events array remains supported");
        test(LivestreamResponseRules.selectLayout(false, false, true, false)
                == LivestreamResponseRules.Layout.DATA_ARRAY, "direct data array supported");
        test(LivestreamResponseRules.selectLayout(false, false, false, true)
                == LivestreamResponseRules.Layout.DATA_EVENTS, "data.events array remains supported");
        test(LivestreamResponseRules.selectLayout(false, false, false, false)
                == LivestreamResponseRules.Layout.INVALID, "missing events array rejected");
        test(LivestreamResponseRules.apiCodeIsValid(false, null), "legacy response without code accepted");
        test(LivestreamResponseRules.apiCodeIsValid(true, 200), "API code 200 accepted");
        test(!LivestreamResponseRules.apiCodeIsValid(true, 0), "API code 0 rejected");
        test(!LivestreamResponseRules.apiCodeIsValid(true, 500), "API error code rejected");
        test(!LivestreamResponseRules.apiCodeIsValid(true, "200"), "string API code rejected");
        test(BroadcastRules.isCodeEvent(CodeRules.HSR, "starrail:livestream-code:abc"),
                "Star Rail livestream event accepted");
        test(BroadcastRules.isCodeEvent(CodeRules.ZZZ, "zzz:livestream-code:abc"),
                "Zenless Zone Zero livestream event accepted");
        test(!BroadcastRules.isCodeEvent(CodeRules.HSR, "zzz:livestream-code:abc"),
                "Zenless event excluded from Star Rail");
        test(!BroadcastRules.isCodeEvent(CodeRules.ZZZ, "starrail:livestream-code:abc"),
                "Star Rail event excluded from Zenless Zone Zero");
        test(BroadcastRules.cleanCode("redeem code now").isEmpty(),
                "ordinary text is not treated as a redeem code");
        System.out.println("LivestreamResponseRules: 15 checks PASSED");
    }
}
