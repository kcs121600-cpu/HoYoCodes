package com.privatecodes.hoyo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Optional update discovery only. No silent installs, no credentials, no
 * untrusted download hosts, no APK installer permission.
 */
public final class UpdateChecker {
    private static final String PREFS = "hoyo_state";
    private static final String CHANNEL = "app_updates";
    private static final int UPDATE_NOTIFICATION_ID = 31007;
    private static final long CHECK_MS = 24L * 60 * 60 * 1000;
    private UpdateChecker() {}

    public static final class Release {
        public final String tag;
        public final String apkUrl;
        public final String releaseUrl;
        public final boolean newer;
        Release(String tag, String apkUrl, String releaseUrl, boolean newer) {
            this.tag = tag; this.apkUrl = apkUrl; this.releaseUrl = releaseUrl; this.newer = newer;
        }
    }
    public static String configuredRepo(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("release_repo", "");
    }
    public static void setRepo(Context ctx, String value) {
        String clean = ReleaseRules.repo(value);
        if (!value.trim().isEmpty() && clean.isEmpty()) throw new IllegalArgumentException("owner/repo 형식으로 입력하세요");
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("release_repo", clean)
            .remove("update_last_checked")
            .remove("update_last_notified")
            .apply();
    }
    /** Run on a worker thread. */
    public static Release checkNow(Context ctx) throws Exception {
        String repo = configuredRepo(ctx);
        if (repo.isEmpty()) throw new IllegalStateException("설정에서 GitHub 공개 저장소를 등록하세요");
        HttpURLConnection conn = (HttpURLConnection)new URL(ReleaseRules.apiUrl(repo)).openConnection();
        conn.setConnectTimeout(7500);
        conn.setReadTimeout(7500);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "HoYoCodes-Personal-Updater/1.2");
        try {
            int status = conn.getResponseCode();
            if (status != 200) throw new IllegalStateException("GitHub 릴리스 조회 HTTP " + status);
            byte[] buffer = new byte[4096];
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                int length;
                while ((length = in.read(buffer)) >= 0) {
                    if (out.size() + length > 512 * 1024) throw new IllegalArgumentException("응답이 너무 큽니다");
                    out.write(buffer, 0, length);
                }
                JSONObject root = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
                Release release = parseRelease(repo, root, BuildConfig.VERSION_NAME);
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong("update_last_checked", System.currentTimeMillis())
                    .putString("update_latest_version", release.tag)
                    .putString("update_latest_url", release.apkUrl)
                    .apply();
                return release;
            }
        } finally { conn.disconnect(); }
    }
    public static Release parseRelease(String repo, JSONObject root, String installedVersion) throws Exception {
        String tag = root.optString("tag_name", "");
        boolean newer = ReleaseRules.isNewer(tag, installedVersion);
        JSONArray assets = root.optJSONArray("assets");
        String url = "";
        if (assets != null) for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.optJSONObject(i);
            if (a == null || !"uploaded".equals(a.optString("state", "uploaded"))) continue;
            String name = a.optString("name", "");
            String link = a.optString("browser_download_url", "");
            long size = a.optLong("size", 0L);
            if (name.endsWith(".apk") && size > 0 && size < 150L*1024*1024
                    && ReleaseRules.trustedApkUrl(repo, link)) { url = link; break; }
        }
        String html = "https://github.com/" + repo + "/releases/latest";
        return new Release(tag, url, html, newer);
    }
    public static void maybeCheck(Context ctx) {
        Context app = ctx.getApplicationContext();
        if (configuredRepo(app).isEmpty()) return;
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (System.currentTimeMillis() - prefs.getLong("update_last_checked", 0L) < CHECK_MS) return;
        try {
            Release result = checkNow(app);
            if (result.newer && !result.apkUrl.isEmpty()
                    && !result.tag.equals(prefs.getString("update_last_notified", ""))) {
                showUpdateNotification(app, result);
                prefs.edit().putString("update_last_notified", result.tag).apply();
            }
        } catch (Exception ignored) {
            // Network failures are transient. Try again at the next normal code sync.
        }
    }
    private static void showUpdateNotification(Context ctx, Release release) {
        if (!CodeNotifier.canNotify(ctx)) return;
        NotificationManager manager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "앱 업데이트", NotificationManager.IMPORTANCE_DEFAULT));
        // Only opens Chrome. OS install confirmation is not bypassed.
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl));
        open.addCategory(Intent.CATEGORY_BROWSABLE);
        PendingIntent action = PendingIntent.getActivity(ctx, 80, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("HoYo Codes 업데이트 " + release.tag)
                .setContentText("다운로드 후 Android 설치 확인이 필요합니다")
                .setAutoCancel(true).setContentIntent(action)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_save,
                        "업데이트 파일 열기", action).build())
                .build();
        manager.notify(UPDATE_NOTIFICATION_ID, notification);
    }
}
