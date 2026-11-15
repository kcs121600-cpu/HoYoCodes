package com.privatecodes.hoyo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CodeStore extends SQLiteOpenHelper {
    public static final class Item {
        public String game, code, status, rewards, source;
        public long firstSeen, lastSeen, updated;
        public Item(String game, String code, String rewards, String source) {
            this.game = CodeRules.safeGame(game);
            this.code = CodeRules.clean(code);
            this.rewards = rewards == null ? "" : rewards;
            this.source = source == null ? "" : source;
            this.status = CodeRules.NEW;
        }
    }
    private final SharedPreferences prefs;

    public CodeStore(Context context) {
        super(context.getApplicationContext(), "hoyo_codes.db", null, 1);
        prefs = context.getApplicationContext().getSharedPreferences("hoyo_state", Context.MODE_PRIVATE);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE codes (game TEXT NOT NULL, code TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'new', rewards TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '', first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(game, code))");
        db.execSQL("CREATE INDEX idx_codes_status_seen ON codes (status, first_seen DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("No database migration defined");
    }
    /** Adds codes only once; never resets a user-selected status during a refresh. */
    public synchronized boolean add(Item item) {
        if (item.code.isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("game", item.game);
        values.put("code", item.code);
        values.put("status", CodeRules.NEW);
        values.put("rewards", item.rewards);
        values.put("source", item.source);
        values.put("first_seen", now);
        values.put("last_seen", now);
        values.put("updated", now);
        boolean inserted = db.insertWithOnConflict("codes", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1;
        if (!inserted) {
            ContentValues seen = new ContentValues();
            seen.put("last_seen", now);
            // The status stays intact, while a newly identified broadcast code
            // receives a visible high-priority source label.
            if (item.source.contains("방송 한정")) {
                seen.put("source", item.source);
                if (!item.rewards.isEmpty()) seen.put("rewards", item.rewards);
            }
            db.update("codes", seen, "game=? AND code=?", new String[]{item.game, item.code});
        }
        return inserted;
    }
    public synchronized void changeStatus(String game, String code, String status) {
        if (!CodeRules.NEW.equals(status) && !CodeRules.REVIEW.equals(status)
                && !CodeRules.USED.equals(status) && !CodeRules.EXPIRED.equals(status)) return;
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("updated", System.currentTimeMillis());
        getWritableDatabase().update("codes", values, "game=? AND code=?",
                new String[]{CodeRules.safeGame(game), CodeRules.clean(code)});
    }
    public synchronized List<Item> list(String game, String status) {
        List<Item> result = new ArrayList<>();
        String selection = "1=1";
        List<String> args = new ArrayList<>();
        if (game != null) { selection += " AND game=?"; args.add(game); }
        if (status != null) { selection += " AND status=?"; args.add(status); }
        try (Cursor c = getReadableDatabase().query("codes", null, selection,
                args.toArray(new String[0]), null, null, "first_seen DESC, code ASC")) {
            while (c.moveToNext()) {
                Item item = new Item(c.getString(c.getColumnIndexOrThrow("game")),
                        c.getString(c.getColumnIndexOrThrow("code")),
                        c.getString(c.getColumnIndexOrThrow("rewards")),
                        c.getString(c.getColumnIndexOrThrow("source")));
                item.status = c.getString(c.getColumnIndexOrThrow("status"));
                item.firstSeen = c.getLong(c.getColumnIndexOrThrow("first_seen"));
                item.lastSeen = c.getLong(c.getColumnIndexOrThrow("last_seen"));
                item.updated = c.getLong(c.getColumnIndexOrThrow("updated"));
                result.add(item);
            }
        }
        return result;
    }
    public synchronized int count(String game, String status) {
        String q = "SELECT COUNT(*) FROM codes WHERE status=?" + (game == null ? "" : " AND game=?");
        String[] args = game == null ? new String[]{status} : new String[]{status, game};
        try (Cursor c = getReadableDatabase().rawQuery(q, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
    public void setCheckResult(int added, String error, Map<String, String> broadcastStatuses) {
        SharedPreferences.Editor editor = prefs.edit()
                .putLong("last_checked", System.currentTimeMillis())
                .putString("last_error", error == null ? "" : error)
                .putInt("last_added", added);
        if (broadcastStatuses != null) {
            for (Map.Entry<String, String> entry : broadcastStatuses.entrySet()) {
                if (CodeRules.HSR.equals(entry.getKey()) || CodeRules.ZZZ.equals(entry.getKey())) {
                    editor.putString("broadcast_status_" + entry.getKey(), entry.getValue());
                }
            }
        }
        editor.apply();
    }
    public long lastChecked() { return prefs.getLong("last_checked", 0); }
    public String lastError() { return prefs.getString("last_error", ""); }
    public int lastAdded() { return prefs.getInt("last_added", 0); }
    public String lastBroadcastStatus(String game) {
        String safe = CodeRules.safeGame(game);
        return prefs.getString("broadcast_status_" + safe, "검사 전");
    }

    /** Backup includes user status; import preserves USED/REVIEW/EXPIRED on duplicates. */
    public synchronized String exportJson() throws Exception {
        JSONArray array = new JSONArray();
        for (Item item : list(null, null)) {
            JSONObject o = new JSONObject();
            o.put("game", item.game); o.put("code", item.code);
            o.put("status", item.status); o.put("rewards", item.rewards);
            o.put("source", item.source); array.put(o);
        }
        JSONObject root = new JSONObject();
        root.put("format", "hoyocodes-backup-v1"); root.put("codes", array);
        return root.toString(2);
    }
    private int statusPriority(String status) {
        if (CodeRules.USED.equals(status)) return 4;
        if (CodeRules.EXPIRED.equals(status)) return 3;
        if (CodeRules.REVIEW.equals(status)) return 2;
        return 1;
    }
    private String currentStatus(String game, String code) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT status FROM codes WHERE game=? AND code=?", new String[]{game, code})) {
            return c.moveToFirst() ? c.getString(0) : CodeRules.NEW;
        }
    }
    public synchronized int importJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"hoyocodes-backup-v1".equals(root.optString("format")))
            throw new IllegalArgumentException("잘못된 백업 파일");
        JSONArray a = root.getJSONArray("codes");
        if (a.length() > 10000) throw new IllegalArgumentException("백업 항목이 너무 많음");
        int count = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            String game = o.optString("game", "");
            String code = CodeRules.clean(o.optString("code", ""));
            if ((!CodeRules.HSR.equals(game) && !CodeRules.ZZZ.equals(game)) || code.isEmpty()) continue;
            String status = o.optString("status", CodeRules.NEW);
            boolean inserted = add(new Item(game, code, o.optString("rewards"), o.optString("source")));
            if (inserted) count++;
            // A stale backup must not turn a locally USED code into NEW.
            if (inserted || statusPriority(status) > statusPriority(currentStatus(game, code))) {
                changeStatus(game, code, status);
            }
        }
        return count;
    }
}
