package com.privatecodes.hoyo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CodeSources {
    private CodeSources() {}
    private static final int MAX_BYTES = 1024 * 1024;

    public static final class FetchResult {
        public final List<CodeStore.Item> codes = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public int workingGames = 0;
    }

    public static FetchResult fetchAll() {
        FetchResult result = new FetchResult();
        for (String game : new String[]{CodeRules.HSR, CodeRules.ZZZ}) {
            Map<String, CodeStore.Item> merged = new LinkedHashMap<>();
            boolean success = false;
            String primary = "https://hoyo-codes.seria.moe/codes?game=" + game;
            String fallback = "https://api.ennead.cc/mihoyo/"
                    + (CodeRules.HSR.equals(game) ? "starrail" : "zenless") + "/codes";
            for (String endpoint : new String[]{primary, fallback}) {
                try {
                    String source = endpoint.contains("seria.moe") ? "hoyo-codes" : "Ennead";
                    List<CodeStore.Item> items = parseResponse(game, source, read(endpoint));
                    for (CodeStore.Item item : items) merged.putIfAbsent(item.code, item);
                    success = true;
                } catch (Exception ex) {
                    result.errors.add(CodeRules.gameLabel(game) + " "
                            + (endpoint.equals(primary) ? "주 출처" : "보조 출처") + " 오류");
                }
            }
            // A third, livestream-specific feed derives its events from official
            // MiYouShe livestream/post endpoints. This is an independent third-party
            // mirror, not a guaranteed real-time first-party API.
            try {
                String gameCalName = CodeRules.HSR.equals(game) ? "starrail" : "zzz";
                List<CodeStore.Item> streamCodes = parseLivestreamEvents(game,
                        read("https://gamecal.nv5.me/api/events?game=" + gameCalName));
                for (CodeStore.Item item : streamCodes) {
                    // Preserve the livestream label when the general sources have
                    // already discovered the same code.
                    merged.put(item.code, item);
                }
                success = true;
            } catch (Exception ex) {
                result.errors.add(CodeRules.gameLabel(game) + " 공식 방송 피드 오류");
            }
            if (success) {
                result.workingGames++;
                result.codes.addAll(merged.values());
            }
        }
        return result;
    }

    private static String read(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HoYoCodes-Personal/1.0");
        conn.setInstanceFollowRedirects(true);
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status);
            try (InputStream input = conn.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (output.size() + n > MAX_BYTES) throw new IllegalStateException("Response too large");
                    output.write(buffer, 0, n);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally { conn.disconnect(); }
    }

    /**
     * GameCal API: explicitly accept ONLY livestream redemption events.
     * Never extract random uppercase text from a web page, which would cause
     * false-positive notifications. Reject officially expired code events.
     */
    public static List<CodeStore.Item> parseLivestreamEvents(String game, String json) throws Exception {
        String trimmed = json.trim();
        JSONArray events;
        if (trimmed.startsWith("[")) {
            events = new JSONArray(trimmed);
        } else {
            JSONObject root = new JSONObject(trimmed);
            events = root.optJSONArray("events");
            if (events == null) {
                JSONObject data = root.optJSONObject("data");
                if (data != null) events = data.optJSONArray("events");
            }
            if (events == null) throw new IllegalArgumentException("No events array");
        }
        if (events.length() > 3000) throw new IllegalArgumentException("Too many events");
        long now = System.currentTimeMillis();
        Map<String, CodeStore.Item> found = new LinkedHashMap<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String id = event.optString("id", "");
            if (!BroadcastRules.isCodeEvent(id)) continue;
            if (BroadcastRules.expired(event.optString("end_time", ""), now)) continue;
            if (BroadcastRules.tooOld(event.optString("start_time", ""), now)) continue;
            JSONArray codes = event.optJSONArray("redeem_codes");
            if (codes == null || codes.length() > 20) continue;
            for (int c = 0; c < codes.length(); c++) {
                Object raw = codes.opt(c);
                String value = raw instanceof JSONObject
                    ? ((JSONObject) raw).optString("code", "")
                    : raw instanceof String ? (String) raw : "";
                String code = CodeRules.clean(value);
                if (!code.isEmpty()) {
                    String reward = "공식 방송 한정 코드 · 만료 시각은 공식 안내 확인";
                    String expiry = event.optString("end_time", "");
                    if (!expiry.isEmpty()) reward = "방송 한정 · 만료 " + expiry;
                    found.putIfAbsent(code, new CodeStore.Item(game, code, reward, "방송 한정 · GameCal"));
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** Accept either a raw JSON array or the documented {active:[...]} API layout. */
    public static List<CodeStore.Item> parseResponse(String game, String source, String json) throws Exception {
        String trimmed = json.trim();
        JSONArray a;
        if (trimmed.startsWith("[")) {
            a = new JSONArray(trimmed);
        } else {
            JSONObject root = new JSONObject(trimmed);
            a = root.optJSONArray("active");
            if (a == null) a = root.optJSONArray("codes");
            if (a == null) a = root.optJSONArray("data");
            if (a == null) throw new IllegalArgumentException("Missing array");
        }
        Map<String, CodeStore.Item> unique = new LinkedHashMap<>();
        if (a.length() > 5000) throw new IllegalArgumentException("Too many codes");
        for (int i = 0; i < a.length(); i++) {
            Object obj = a.opt(i);
            String code = "";
            String rewards = "";
            if (obj instanceof JSONObject) {
                JSONObject o = (JSONObject) obj;
                code = CodeRules.clean(o.optString("code", o.optString("cdkey", "")));
                Object rewardField = o.opt("rewards");
                if (rewardField == null || rewardField == JSONObject.NULL) rewardField = o.opt("reward");
                if (rewardField instanceof JSONArray) {
                    JSONArray r = (JSONArray) rewardField;
                    StringBuilder b = new StringBuilder();
                    for (int j = 0; j < Math.min(6, r.length()); j++) {
                        if (j != 0) b.append(" · ");
                        b.append(r.optString(j));
                    }
                    rewards = b.toString();
                } else if (rewardField != null && rewardField != JSONObject.NULL) {
                    rewards = String.valueOf(rewardField);
                }
            } else if (obj instanceof String) {
                code = CodeRules.clean((String) obj);
            }
            if (!code.isEmpty()) {
                if (rewards.length() > 200) rewards = rewards.substring(0, 200);
                unique.putIfAbsent(code, new CodeStore.Item(game, code, rewards, source));
            }
        }
        return new ArrayList<>(unique.values());
    }
}
