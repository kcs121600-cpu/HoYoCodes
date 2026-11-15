package com.privatecodes.hoyo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class CodeChecker {
    private CodeChecker() {}
    public static final class Result {
        public final int newCount;
        public final int workingGames;
        public final String errors;
        Result(int newCount, int workingGames, String errors) {
            this.newCount = newCount;
            this.workingGames = workingGames;
            this.errors = errors;
        }
    }

    /** Serializes scheduled and manually requested syncs; must be called off the main thread. */
    public static synchronized Result run(Context context) {
        Context app = context.getApplicationContext();
        CodeSources.FetchResult fetched = CodeSources.fetchAll();
        List<CodeStore.Item> added = new ArrayList<>();
        try (CodeStore store = new CodeStore(app)) {
            for (CodeStore.Item item : fetched.codes) {
                if (store.add(item)) added.add(item);
            }
            String errors = String.join(" / ", fetched.errors);
            store.setCheckResult(added.size(), errors, fetched.broadcastStatuses);
        }
        CodeNotifier.notifyNew(app, added);
        return new Result(added.size(), fetched.workingGames, String.join(" / ", fetched.errors));
    }
}
