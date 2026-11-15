import com.privatecodes.hoyo.ReleaseRules;

public class TestReleaseRules {
    static void test(boolean yes, String label) { if (!yes) throw new AssertionError(label); }
    public static void main(String[] args) {
        test(ReleaseRules.repo(" owner/HoYoCodes ").equals("owner/HoYoCodes"), "valid repo");
        test(ReleaseRules.repo("owner/repo?foo").isEmpty(), "query blocked");
        test(ReleaseRules.repo("https://github.com/owner/repo").isEmpty(), "URLs blocked");
        test(ReleaseRules.isNewer("v1.2.1", "1.2.0"), "patch greater");
        test(!ReleaseRules.isNewer("v1.2.0", "1.2.0"), "same version");
        test(!ReleaseRules.isNewer("v1.1.9", "1.2.0"), "older version");
        test(!ReleaseRules.isNewer("v1.3.0-rc1", "1.2.0"), "prerelease excluded");
        test(ReleaseRules.trustedApkUrl("owner/repo", "https://github.com/owner/repo/releases/download/v1.2.1/HoYoCodes.apk"), "trusted URL");
        test(!ReleaseRules.trustedApkUrl("owner/repo", "https://untrusted.io/file.apk"), "host blocked");
        test(!ReleaseRules.trustedApkUrl("owner/repo", "https://github.com/other/repo/releases/download/v1.2.1/file.apk"), "repo blocked");
        System.out.println("ReleaseRules: 10 checks PASSED");
    }
}
