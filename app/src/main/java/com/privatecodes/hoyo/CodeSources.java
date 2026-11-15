package com.privatecodes.hoyo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
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
        public final Map<String, String> broadcastStatuses = new LinkedHashMap<>();
        public int workingGames = 0;
    }

    private static final class SourceResponse {
        final String body;
        SourceResponse(String body) { this.body = body; }
    }

    private static final class SourceException extends Exception {
        SourceException(String message) { super(message); }
    }

    public static FetchResult fetchAll() {
        FetchResult result = new FetchResult();
        for (String game : new String[]{CodeRules.HSR, CodeRules.ZZZ}) {
            Map<String, CodeStore.Item> merged = new LinkedHashMap<>();
            boolean generalSuccess = false;

            String primary = "https://hoyo-codes.seria.moe/codes?game=" + game;
            String fallback = "https://api.ennead.cc/mihoyo/"
                    + (CodeRules.HSR.equals(game) ? "starrail" : "zenless") + "/codes";
            for (String endpoint : new String[]{primary, fallback}) {
                String source = endpoint.contains("seria.moe") ? "hoyo-codes" : "Ennead";
                try {
                    List<CodeStore.Item> items = parseResponse(game, source, read(endpoint));
                    for (CodeStore.Item item : items) merged.putIfAbsent(item.code, item);
                    generalSuccess = true;
                } catch (Exception ex) {
                    result.errors.add(sourceError(game, source, ex));
                }
            }

            boolean broadcastSuccess = false;
            String gameCalName = CodeRules.HSR.equals(game) ? "starrail" : "zzz";
            try {
                List<CodeStore.Item> streamCodes = parseLivestreamEvents(game,
                        readJson("https://gamecal.nv5.me/api/events?game=" + gameCalName).body);
                for (CodeStore.Item item : streamCodes) {
                    // Preserve the livestream label when a general source found the same code.
                    merged.put(item.code, item);
                }
                broadcastSuccess = true;
                result.broadcastStatuses.put(game, streamCodes.isEmpty()
                        ? "연결됨 · 현재 유효 방송 코드 없음"
                        : "연결됨 · 유효 방송 코드 " + streamCodes.size() + "개");
            } catch (Exception ex) {
                String detail = describe(ex);
                result.broadcastStatuses.put(game, "연결 안 됨 · " + detail);
                result.errors.add(CodeRules.gameLabel(game) + " · 방송(GameCal): " + detail);
            }

            if (generalSuccess || broadcastSuccess) {
                result.workingGames++;
                result.codes.addAll(merged.values());
            }
        }
        return result;
    }

    /** Keep the two existing general-code feeds' response handling unchanged. */
    private static String read(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HoYoCodes-Personal/1.0");
        conn.setInstanceFollowRedirects(true);
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) throw new SourceException("HTTP " + status + " 오류");
            try (InputStream input = conn.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (output.size() + n > MAX_BYTES) throw new SourceException("응답 크기 한도 초과");
                    output.write(buffer, 0, n);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Fetches a JSON API response without following redirects to a website fallback. */
    private static SourceResponse readJson(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HoYoCodes-Personal/1.2.2");
        conn.setInstanceFollowRedirects(false);
        try {
            int status = conn.getResponseCode();
            String contentType = conn.getHeaderField("Content-Type");
            if (status >= 300 && status < 400) {
                throw new SourceException("HTTP " + status + " 리디렉션 응답");
            }
            if (status < 200 || status >= 300) {
                throw new SourceException("HTTP " + status + " 오류");
            }
            if (!SourceResponseRules.isJsonContentType(contentType)) {
                throw new SourceException("HTTP " + status + " · JSON 대신 Content-Type="
                        + SourceResponseRules.safeContentType(contentType));
            }
            try (InputStream input = conn.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (output.size() + n > MAX_BYTES) throw new SourceException("응답 크기 한도 초과");
                    output.write(buffer, 0, n);
                }
                String body = output.toString(StandardCharsets.UTF_8.name());
                if (!SourceResponseRules.isJsonDocument(body)) {
                    throw new SourceException("HTTP " + status + " · JSON 본문이 아님");
                }
                return new SourceResponse(body);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String sourceError(String game, String source, Exception ex) {
        return CodeRules.gameLabel(game) + " · " + source + ": " + describe(ex);
    }

    private static String describe(Exception ex) {
        if (ex instanceof SourceException) return ex.getMessage();
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        if (cause instanceof UnknownHostException) return "DNS 호스트 조회 실패";
        if (cause instanceof SocketTimeoutException) return "연결 시간 초과";
        if (cause instanceof org.json.JSONException) return "JSON 구문 오류";
        if (ex instanceof IllegalArgumentException) return "JSON 구조 오류";
        return "네트워크 연결 실패";
    }

    /**
     * Accept only explicitly typed livestream events, their redeem_codes field,
     * and a game-matching event ID. Never extract code-like text from page content.
     */
    public static List<CodeStore.Item> parseLivestreamEvents(String game, String json) throws Exception {
        String trimmed = json.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Empty response");
        JSONArray events = null;
        if (trimmed.startsWith("[")) {
            events = new JSONArray(trimmed);
        } else {
            JSONObject root = new JSONObject(trimmed);
            if (!LivestreamResponseRules.apiCodeIsValid(root.has("code"), root.opt("code"))) {
                throw new IllegalArgumentException("API code is not successful");
            }

            if (root.has("events") && !(root.opt("events") instanceof JSONArray)) {
                throw new IllegalArgumentException("events must be an array");
            }
            JSONArray rootEvents = root.optJSONArray("events");
            Object rawData = root.opt("data");
            JSONArray dataArray = rawData instanceof JSONArray ? (JSONArray) rawData : null;
            JSONObject dataObject = rawData instanceof JSONObject ? (JSONObject) rawData : null;
            if (root.has("data") && dataArray == null && dataObject == null) {
                throw new IllegalArgumentException("data must be an array or object");
            }
            if (dataObject != null && dataObject.has("events")
                    && !(dataObject.opt("events") instanceof JSONArray)) {
                throw new IllegalArgumentException("data.events must be an array");
            }
            JSONArray dataEvents = dataObject == null ? null : dataObject.optJSONArray("events");

            LivestreamResponseRules.Layout layout = LivestreamResponseRules.selectLayout(
                    false, rootEvents != null, dataArray != null, dataEvents != null);
            switch (layout) {
                case ROOT_EVENTS:
                    events = rootEvents;
                    break;
                case DATA_ARRAY:
                    events = dataArray;
                    break;
                case DATA_EVENTS:
                    events = dataEvents;
                    break;
                default:
                    throw new IllegalArgumentException("No supported events array");
            }
        }
        if (events.length() > 3000) throw new IllegalArgumentException("Too many events");
        long now = System.currentTimeMillis();
        Map<String, CodeStore.Item> found = new LinkedHashMap<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null || event.optString("id", "").trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid event object");
            }
            String id = event.optString("id", "");
            if (!BroadcastRules.isCodeEvent(game, id)) continue;
            if (BroadcastRules.expired(event.optString("end_time", ""), now)) continue;
            if (BroadcastRules.tooOld(event.optString("start_time", ""), now)) continue;
            JSONArray codes = event.optJSONArray("redeem_codes");
            if (codes == null || codes.length() > 20) continue;
            for (int c = 0; c < codes.length(); c++) {
                Object raw = codes.opt(c);
                String value = raw instanceof JSONObject
                    ? ((JSONObject) raw).optString("code", "")
                    : raw instanceof String ? (String) raw : "";
                String code = BroadcastRules.cleanCode(value);
                if (!code.isEmpty()) {
                    String reward = "방송 보상은 게임 내 공지에서 확인하세요";
                    String expiry = event.optString("end_time", "");
                    if (!expiry.isEmpty()) reward = "방송 코드 · 만료 " + expiry;
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
