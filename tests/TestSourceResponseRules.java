import com.privatecodes.hoyo.SourceResponseRules;

public class TestSourceResponseRules {
    private static void test(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        test(SourceResponseRules.isJsonContentType("application/json; charset=utf-8"), "JSON MIME with charset");
        test(SourceResponseRules.isJsonContentType("application/problem+json"), "structured JSON MIME");
        test(!SourceResponseRules.isJsonContentType("text/html"), "HTML MIME rejected");
        test(!SourceResponseRules.isJsonContentType("text/plain"), "plain text MIME rejected");
        test(!SourceResponseRules.isJsonContentType(null), "missing MIME rejected");
        test(SourceResponseRules.isJsonDocument(" {\"events\":[]} "), "object body accepted");
        test(SourceResponseRules.isJsonDocument(" [ ] "), "array body accepted");
        test(!SourceResponseRules.isJsonDocument("<!doctype html><html></html>"), "HTML body rejected");
        test(!SourceResponseRules.isJsonDocument("not JSON"), "text body rejected");
        test("text/html".equals(SourceResponseRules.safeContentType("text/html\r\nInjected: yes")),
                "header line breaks stripped");
        System.out.println("SourceResponseRules: 10 checks PASSED");
    }
}
