package com.privatecodes.hoyo;

/** Inputs from the optional public GitHub release feed stay narrowly scoped. */
public final class ReleaseRules {
    private ReleaseRules() {}

    public static String repo(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        // A personal, public, user-controlled repository only. Not arbitrary URLs.
        return trimmed.matches("[A-Za-z0-9_.-]{1,64}/[A-Za-z0-9_.-]{1,100}")
            && !trimmed.startsWith(".") && !trimmed.contains("/.") ? trimmed : "";
    }
    public static String apiUrl(String raw) {
        String parsed = repo(raw);
        if (parsed.isEmpty()) throw new IllegalArgumentException("GitHub 저장소는 owner/repo 형식이어야 합니다");
        return "https://api.github.com/repos/" + parsed + "/releases/latest";
    }
    public static boolean isNewer(String remoteTag, String installed) {
        int[] remote = components(remoteTag);
        int[] local = components(installed);
        if (remote == null || local == null) return false;
        for (int i = 0; i < 3; i++) {
            if (remote[i] > local[i]) return true;
            if (remote[i] < local[i]) return false;
        }
        return false;
    }
    private static int[] components(String tag) {
        if (tag == null) return null;
        String version = tag.replaceFirst("^[vV]", "");
        if (!version.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")) return null;
        String[] parts = version.split("\\.");
        return new int[]{Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])};
    }
    public static boolean trustedApkUrl(String repo, String url) {
        String r = repo(repo);
        if (r.isEmpty() || url == null) return false;
        String prefix = "https://github.com/" + r + "/releases/download/";
        return url.startsWith(prefix) && url.toLowerCase(java.util.Locale.ROOT).endsWith(".apk")
            && !url.contains("?") && !url.contains("#") && !url.contains("\\");
    }
}
