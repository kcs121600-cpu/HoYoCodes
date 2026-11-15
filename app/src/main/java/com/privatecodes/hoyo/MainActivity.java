package com.privatecodes.hoyo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class MainActivity extends Activity {
    public static final String ACTION_OPEN_CODE = "com.privatecodes.hoyo.OPEN_CODE";
    public static final String ACTION_LIST = "com.privatecodes.hoyo.OPEN_LIST";
    public static final String EXTRA_GAME = "game";
    public static final String EXTRA_CODE = "code";
    private static final int NOTIFICATION_PERMISSION = 50;
    private static final int EXPORT_REQUEST = 51;
    private static final int IMPORT_REQUEST = 52;
    private static final int BG = Color.rgb(247, 249, 253);
    private static final int WHITE = Color.WHITE;
    private static final int INK = Color.rgb(24, 35, 58);
    private static final int MUTED = Color.rgb(105, 116, 133);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int GREEN = Color.rgb(18, 137, 93);
    private static final int LINE = Color.rgb(224, 231, 241);
    private static final int PALE = Color.rgb(239, 244, 251);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String selectedGame = null;
    private String selectedStatus = CodeRules.NEW;
    private boolean scanning = false;
    private boolean tabletLandscape = false;
    private boolean useTwoCardColumns = false;
    private LinearLayout root, content;

    private int dp(float d) { return (int)(d * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int borderColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (borderColor != 0) drawable.setStroke(dp(1), borderColor);
        return drawable;
    }
    private TextView text(String s, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        return tv;
    }
    private LinearLayout column() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); return c; }
    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private void padding(View view, int left, int top, int right, int bottom) { view.setPadding(dp(left),dp(top),dp(right),dp(bottom)); }
    private LinearLayout.LayoutParams vertical(int width, int height, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        p.bottomMargin = dp(bottom);
        return p;
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(primary ? WHITE : BLUE);
        b.setBackground(shape(primary ? BLUE : PALE, 0, 11));
        b.setMinHeight(dp(48));
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }
    private TextView tab(String label, boolean active, Runnable click) {
        TextView tv = text(label, 13, active ? WHITE : MUTED, active);
        padding(tv, 14, 11, 14, 11);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(shape(active ? BLUE : WHITE, active ? 0 : LINE, 12));
        tv.setOnClickListener(v -> click.run());
        return tv;
    }
    private void notice(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            String g = state.getString("g", "all");
            selectedGame = "all".equals(g) ? null : g;
            selectedStatus = state.getString("s", CodeRules.NEW);
        }
        CodeNotifier.setup(this);
        CodeScheduler.initialize(this);
        render();
        handleOpenIntent(getIntent());
        requestNotificationPermission();
        try (CodeStore store = new CodeStore(this)) {
            if (store.lastChecked() == 0) checkNow();
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("g", selectedGame == null ? "all" : selectedGame);
        out.putString("s", selectedStatus);
        super.onSaveInstanceState(out);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenIntent(intent);
    }
    @Override protected void onResume() {
        super.onResume();
        if (root != null) render();
    }
    private void handleOpenIntent(Intent intent) {
        if (intent == null) return;
        if (ACTION_OPEN_CODE.equals(intent.getAction())) {
            String code = CodeRules.clean(intent.getStringExtra(EXTRA_CODE));
            String game = intent.getStringExtra(EXTRA_GAME);
            if (!code.isEmpty() && (CodeRules.HSR.equals(game) || CodeRules.ZZZ.equals(game))) {
                selectedGame = game;
                selectedStatus = CodeRules.REVIEW;
                openInChrome(game, code);
            }
            intent.setAction(ACTION_LIST); // do not launch again on rotation / reentry
        } else if (ACTION_LIST.equals(intent.getAction())) {
            selectedStatus = CodeRules.NEW;
            render();
        }
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                !getPreferences(MODE_PRIVATE).getBoolean("asked_notifications", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("asked_notifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
    }
    private void checkNow() {
        if (scanning) return;
        scanning = true;
        render();
        new Thread(() -> {
            String message;
            try {
                CodeChecker.Result result = CodeChecker.run(getApplicationContext());
                message = result.workingGames == 0 ? "코드 출처에 연결하지 못했어요. 나중에 다시 확인하세요." :
                        "확인 완료 · 새 코드 " + result.newCount + "개"
                        + (result.errors.isEmpty() ? "" : " (일부 출처 연결 실패)");
            } catch (Exception ex) {
                message = "확인 실패: 네트워크 연결을 확인하세요.";
            }
            String finalMessage = message;
            mainHandler.post(() -> {
                scanning = false;
                render();
                notice(finalMessage);
            });
        }, "hoyocodes-manual-sync").start();
    }

    private void openInChrome(String game, String rawCode) {
        String code = CodeRules.clean(rawCode);
        if (code.isEmpty()) return;
        try {
            // Clipboard fallback if a future website update stops URL prefill.
            ClipboardManager cb = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("HoYo redeem code", code));
            Intent chrome = new Intent(Intent.ACTION_VIEW, Uri.parse(CodeRules.redeemUrl(game, code)));
            chrome.addCategory(Intent.CATEGORY_BROWSABLE);
            chrome.setPackage("com.android.chrome");
            try {
                startActivity(chrome);
            } catch (ActivityNotFoundException missingChrome) {
                chrome.setPackage(null); // Browser fallback on devices without Chrome.
                startActivity(chrome);
            }
            try (CodeStore store = new CodeStore(this)) {
                store.changeStatus(game, code, CodeRules.REVIEW);
            }
            render();
        } catch (Exception ex) {
            notice("브라우저를 열 수 없어요. 코드를 복사하여 공식 사이트에 입력하세요.");
        }
    }

    /**
     * Y700 3세대의 16:10 디스플레이에서 가로/세로 UI를 독립적으로 구성한다.
     * 실제 픽셀 수에 고정하지 않고 Android dp/화면 폭을 사용하므로 글꼴 배율에도 대응한다.
     */
    private void render() {
        Configuration config = getResources().getConfiguration();
        int widthDp = config.screenWidthDp;
        if (widthDp <= 0) {
            widthDp = (int)(getResources().getDisplayMetrics().widthPixels
                    / getResources().getDisplayMetrics().density + 0.5f);
        }
        tabletLandscape = LayoutRules.twoPane(config.orientation == Configuration.ORIENTATION_LANDSCAPE, widthDp);
        // 충분한 dp 너비가 확보될 때만 두 열 목록을 사용한다.
        useTwoCardColumns = LayoutRules.twoCardColumns(tabletLandscape, widthDp);

        root = column();
        root.setBackgroundColor(BG);
        setContentView(root);
        if (tabletLandscape) {
            renderTabletLandscape();
        } else {
            renderPortrait(widthDp);
        }
    }

    private ScrollView scrollPane() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        return scroll;
    }

    private void renderPortrait(int widthDp) {
        ScrollView scroll = scrollPane();
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout centering = column();
        centering.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(centering, new ScrollView.LayoutParams(-1, -2));

        content = column();
        // 작은 화면은 가득 사용하고 Y700 세로 화면은 양쪽에 여백을 둔다.
        int maxWidth = LayoutRules.portraitContentWidth(widthDp);
        padding(content, 18, 20, 18, 24);
        centering.addView(content, new LinearLayout.LayoutParams(dp(maxWidth), -2));
        drawHeader();
        drawStatus();
        drawGameTabs();
        drawCounters();
        drawStatusTabs();
        drawCards();
        drawFooter();
    }

    private void renderTabletLandscape() {
        LinearLayout panes = row();
        panes.setGravity(Gravity.TOP);
        root.addView(panes, new LinearLayout.LayoutParams(-1, -1));

        ScrollView sidebarScroll = scrollPane();
        sidebarScroll.setBackgroundColor(WHITE);
        panes.addView(sidebarScroll, new LinearLayout.LayoutParams(dp(326), -1));
        LinearLayout sidebar = column();
        padding(sidebar, 20, 24, 20, 24);
        sidebarScroll.addView(sidebar, new ScrollView.LayoutParams(-1, -2));
        content = sidebar;
        drawHeader();
        drawStatus();
        drawGameTabs();
        drawCounters();
        drawTabletQuickActions();

        View separator = new View(this);
        separator.setBackgroundColor(LINE);
        panes.addView(separator, new LinearLayout.LayoutParams(dp(1), -1));

        ScrollView listingScroll = scrollPane();
        panes.addView(listingScroll, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout centering = column();
        centering.setGravity(Gravity.CENTER_HORIZONTAL);
        listingScroll.addView(centering, new ScrollView.LayoutParams(-1, -2));
        LinearLayout listing = column();
        padding(listing, 22, 24, 22, 24);
        // 여백이 너무 넓게 퍼지지 않도록 코드 영역의 최대 폭을 제한.
        int listingWidth = LayoutRules.listingContentWidth(getResources().getConfiguration().screenWidthDp);
        centering.addView(listing, new LinearLayout.LayoutParams(dp(listingWidth), -2));
        content = listing;
        TextView heading = text("리딤코드 목록", 24, INK, true);
        content.addView(heading, vertical(-1, -2, 4));
        TextView description = text("게임을 선택하고 등록 상태별로 코드를 확인하세요.", 13, MUTED, false);
        content.addView(description, vertical(-1, -2, 18));
        drawStatusTabs();
        drawCards();
        drawFooter();
    }

    private void drawTabletQuickActions() {
        TextView label = text("빠른 이동", 13, MUTED, true);
        content.addView(label, vertical(-1, -2, 10));
        Button pending = button("확인 필요한 코드 보기", false);
        pending.setOnClickListener(v -> { selectedStatus = CodeRules.REVIEW; render(); });
        content.addView(pending, vertical(-1, 48, 8));
        Button history = button("사용한 코드 보기", false);
        history.setOnClickListener(v -> { selectedStatus = CodeRules.USED; render(); });
        content.addView(history, vertical(-1, 48, 8));
        Button add = button("＋ 코드 직접 추가", false);
        add.setOnClickListener(v -> showAddDialog());
        content.addView(add, vertical(-1, 48, 0));
    }
    private void drawHeader() {
        LinearLayout r = row();
        LinearLayout names = column();
        names.addView(text("HoYo Codes", 26, INK, true));
        TextView sub = text("스타레일 · 젠레스 존 제로 리딤코드", 12, MUTED, false);
        names.addView(sub);
        r.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = button("⚙ 설정", false);
        r.addView(settings, new LinearLayout.LayoutParams(dp(76), dp(48)));
        settings.setOnClickListener(v -> showSettings());
        content.addView(r, vertical(-1, -2, 18));
    }
    private void drawStatus() {
        LinearLayout panel = column();
        padding(panel, 16, 14, 16, 14);
        panel.setBackground(shape(WHITE, LINE, 16));
        LinearLayout head = row();
        TextView left = text("●  자동 코드 확인 " + (scanning ? "진행 중" : "활성화"), 14, GREEN, true);
        head.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        Button sync = button(scanning ? "확인 중" : "↻ 확인", false);
        sync.setEnabled(!scanning);
        sync.setOnClickListener(v -> checkNow());
        head.addView(sync, new LinearLayout.LayoutParams(dp(88), dp(48)));
        panel.addView(head);
        int interval = CodeScheduler.intervalHours(this);
        panel.addView(text(interval + "시간 간격 · 앱을 열지 않아도 백그라운드 검사", 12, MUTED, false));
        try (CodeStore store = new CodeStore(this)) {
            String hsrBroadcast = store.lastBroadcastStatus(CodeRules.HSR);
            String zzzBroadcast = store.lastBroadcastStatus(CodeRules.ZZZ);
            panel.addView(text("방송 코드 · 스타레일: " + hsrBroadcast, 12,
                    hsrBroadcast.startsWith("연결됨") ? GREEN : Color.rgb(180, 95, 30), false));
            panel.addView(text("방송 코드 · 젠존제: " + zzzBroadcast, 12,
                    zzzBroadcast.startsWith("연결됨") ? GREEN : Color.rgb(180, 95, 30), false));
            if (store.lastChecked() > 0) {
                String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(store.lastChecked()));
                panel.addView(text("최근 검사: " + time + " / 발견 " + store.lastAdded() + "개", 12, MUTED, false));
                if (!store.lastError().isEmpty()) panel.addView(text("일부 코드 출처 연결 실패 · 설정에서 확인 가능", 12, Color.rgb(180, 95, 30), false));
            }
        }
        if (!CodeNotifier.canNotify(this)) panel.addView(text("알림 권한이 꺼져 있습니다 · 설정에서 허용해 주세요", 12, Color.rgb(180, 95, 30), false));
        content.addView(panel, vertical(-1, -2, 14));
    }
    private void drawGameTabs() {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER);
        String[] labels = {"전체", "스타레일", "젠존제"};
        String[] values = {null, CodeRules.HSR, CodeRules.ZZZ};
        for (int i = 0; i < labels.length; i++) {
            final String choice = values[i];
            TextView b = tab(labels[i], choice == null ? selectedGame == null : choice.equals(selectedGame), () -> {
                selectedGame = choice; render();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
            if (i < 2) p.rightMargin = dp(7);
            r.addView(b, p);
        }
        content.addView(r, vertical(-1, -2, 16));
    }
    private void drawCounters() {
        LinearLayout r = row();
        String[] statuses = {CodeRules.NEW, CodeRules.REVIEW, CodeRules.USED};
        String[] titles = {"새 코드", "확인 필요", "사용 완료"};
        try (CodeStore store = new CodeStore(this)) {
            for (int i = 0; i < statuses.length; i++) {
                final String target = statuses[i];
                LinearLayout card = column();
                card.setGravity(Gravity.CENTER);
                padding(card, 5, 10, 5, 10);
                card.setBackground(shape(WHITE, LINE, 12));
                TextView num = text(String.valueOf(store.count(selectedGame, target)), 24,
                        i == 0 ? BLUE : INK, true);
                num.setGravity(Gravity.CENTER);
                TextView lbl = text(titles[i], 11, MUTED, false);
                lbl.setGravity(Gravity.CENTER);
                card.addView(num); card.addView(lbl);
                card.setOnClickListener(v -> { selectedStatus = target; render(); });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(88), 1);
                if (i < statuses.length - 1) p.rightMargin = dp(8);
                r.addView(card, p);
            }
        }
        content.addView(r, vertical(-1, -2, 16));
    }
    private void drawStatusTabs() {
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        LinearLayout r = row();
        String[] statuses = {CodeRules.NEW, CodeRules.REVIEW, CodeRules.USED, CodeRules.EXPIRED};
        String[] labels = {"새 코드", "확인 필요", "사용한 코드", "만료"};
        for (int i = 0; i < statuses.length; i++) {
            final String status = statuses[i];
            TextView b = tab(labels[i], selectedStatus.equals(status), () -> {
                selectedStatus = status; render();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(48));
            if (i < statuses.length - 1) p.rightMargin = dp(6);
            r.addView(b, p);
        }
        h.addView(r);
        content.addView(h, vertical(-1, 52, 14));
    }
    private void drawCards() {
        List<CodeStore.Item> items;
        try (CodeStore store = new CodeStore(this)) { items = store.list(selectedGame, selectedStatus); }
        if (items.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            padding(empty, 12, 30, 12, 30);
            empty.setBackground(shape(WHITE, LINE, 15));
            TextView icon = text("✓", 32, BLUE, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon);
            TextView hint = text("이 목록에 코드가 없습니다", 15, INK, true);
            hint.setGravity(Gravity.CENTER);
            empty.addView(hint);
            TextView desc = text("새 코드가 발견되면 여기에 표시됩니다", 12, MUTED, false);
            desc.setGravity(Gravity.CENTER);
            empty.addView(desc);
            content.addView(empty, vertical(-1, -2, 12));
        } else if (useTwoCardColumns) {
            // 가로 모드: 코드 카드 2열. 각 열은 330dp 이상 확보한 상태에서만 활성화.
            for (int i = 0; i < items.size(); i += 2) {
                LinearLayout pair = row();
                pair.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1);
                first.rightMargin = dp(7);
                pair.addView(makeCard(items.get(i)), first);
                if (i + 1 < items.size()) {
                    LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
                    second.leftMargin = dp(7);
                    pair.addView(makeCard(items.get(i + 1)), second);
                } else {
                    View blank = new View(this);
                    pair.addView(blank, new LinearLayout.LayoutParams(0, dp(1), 1));
                }
                content.addView(pair, vertical(-1, -2, 14));
            }
        } else {
            for (CodeStore.Item item : items) drawCard(item);
        }
    }
    private void drawCard(CodeStore.Item item) {
        content.addView(makeCard(item), vertical(-1, -2, 10));
    }
    private LinearLayout makeCard(CodeStore.Item item) {
        LinearLayout card = column();
        padding(card, 15, 14, 15, 14);
        card.setBackground(shape(WHITE, LINE, 15));
        LinearLayout header = row();
        TextView game = text((CodeRules.ZZZ.equals(item.game) ? "◆ " : "✦ ") + CodeRules.gameLabel(item.game), 14, INK, true);
        header.addView(game, new LinearLayout.LayoutParams(0, -2, 1));
        String state = CodeRules.NEW.equals(item.status) ? "미사용" : CodeRules.REVIEW.equals(item.status) ? "확인 필요" : CodeRules.USED.equals(item.status) ? "사용 완료" : "만료";
        TextView stateView = text(state, 11, CodeRules.USED.equals(item.status) ? GREEN : BLUE, true);
        header.addView(stateView);
        card.addView(header, vertical(-1, -2, 10));
        LinearLayout codeRow = row();
        TextView code = text(item.code, 17, INK, true);
        code.setSingleLine(true);
        code.setEllipsize(android.text.TextUtils.TruncateAt.END);
        codeRow.addView(code, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button copy = button("복사", false);
        codeRow.addView(copy, new LinearLayout.LayoutParams(dp(66), dp(48)));
        copy.setOnClickListener(v -> {
            ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("HoYo redeem code", item.code));
            notice("코드가 복사되었습니다");
        });
        card.addView(codeRow, vertical(-1, -2, 4));
        if (item.rewards != null && !item.rewards.isEmpty()) {
            TextView reward = text(item.rewards, 12, MUTED, false);
            card.addView(reward, vertical(-1, -2, 4));
        }
        String time = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(item.firstSeen));
        card.addView(text("수집 " + time + " · " + (item.source.isEmpty() ? "직접 추가" : item.source), 11, MUTED, false), vertical(-1,-2,10));
        if (CodeRules.NEW.equals(item.status) || CodeRules.REVIEW.equals(item.status)) {
            Button open = button("Chrome에서 코드 자동 입력 ↗", true);
            open.setOnClickListener(v -> openInChrome(item.game, item.code));
            card.addView(open, vertical(-1, 48, 7));
            LinearLayout actions = row();
            Button used = button("✓ 사용 완료", false);
            used.setOnClickListener(v -> setStatus(item, CodeRules.USED));
            actions.addView(used, new LinearLayout.LayoutParams(0, dp(48), 1));
            Button expired = button("만료 표시", false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
            p.leftMargin = dp(7);
            actions.addView(expired, p);
            expired.setOnClickListener(v -> setStatus(item, CodeRules.EXPIRED));
            card.addView(actions);
        } else {
            Button restore = button("미사용으로 되돌리기", false);
            restore.setOnClickListener(v -> setStatus(item, CodeRules.NEW));
            card.addView(restore, vertical(-1, 48, 0));
        }
        return card;
    }
    private void setStatus(CodeStore.Item item, String status) {
        try (CodeStore store = new CodeStore(this)) { store.changeStatus(item.game, item.code, status); }
        render();
    }
    private void drawFooter() {
        TextView hint = text("사용한 코드는 다시 수집해도 사용 완료 상태가 유지됩니다.\n최종 등록 성공 여부는 공식 사이트에서 확인해 주세요.", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        padding(hint, 0, 10, 0, 16);
        content.addView(hint, vertical(-1, -2, 10));
        LinearLayout nav = row();
        Button home = button("⌂ 새 코드", false);
        home.setOnClickListener(v -> { selectedStatus = CodeRules.NEW; render(); });
        Button records = button("▤ 사용 기록", false);
        records.setOnClickListener(v -> { selectedStatus = CodeRules.USED; render(); });
        Button add = button("＋ 코드 추가", false);
        add.setOnClickListener(v -> showAddDialog());
        for (Button b : new Button[]{home, records, add}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
            p.rightMargin = dp(5);
            nav.addView(b, p);
        }
        content.addView(nav);
    }
    private void showAddDialog() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setHint("리딤코드 입력");
        padding(edit, 18, 6, 18, 6);
        String[] games = {"붕괴: 스타레일", "젠레스 존 제로"};
        LinearLayout inputs = column();
        padding(inputs, 16, 4, 16, 4);
        Spinner gamePicker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, games);
        gamePicker.setAdapter(adapter);
        gamePicker.setSelection(CodeRules.ZZZ.equals(selectedGame) ? 1 : 0);
        inputs.addView(gamePicker, vertical(-1, 52, 8));
        inputs.addView(edit, vertical(-1, -2, 0));
        new AlertDialog.Builder(this).setTitle("코드 직접 추가")
                .setView(inputs)
                .setNegativeButton("취소", null)
                .setPositiveButton("추가", (d, which) -> {
                    String code = CodeRules.clean(edit.getText().toString());
                    if (code.isEmpty()) { notice("영문과 숫자 4~32자로 입력해 주세요."); return; }
                    String game = gamePicker.getSelectedItemPosition() == 1 ? CodeRules.ZZZ : CodeRules.HSR;
                    try (CodeStore store = new CodeStore(this)) {
                        boolean inserted = store.add(new CodeStore.Item(game, code, "", "직접 추가"));
                        notice(inserted ? "새 코드가 추가되었습니다" : "이미 저장된 코드입니다");
                    }
                    selectedStatus = CodeRules.NEW;
                    render();
                }).show();
    }
    private void showSettings() {
        int hours = CodeScheduler.intervalHours(this);
        String lastError;
        String hsrBroadcast;
        String zzzBroadcast;
        try (CodeStore store = new CodeStore(this)) {
            lastError = store.lastError();
            hsrBroadcast = store.lastBroadcastStatus(CodeRules.HSR);
            zzzBroadcast = store.lastBroadcastStatus(CodeRules.ZZZ);
        }
        String repo = UpdateChecker.configuredRepo(this);
        String msg = "검사 주기: " + hours + "시간 (절전 정책에 따라 지연될 수 있음)\n"
                + "일반 코드 출처: hoyo-codes · Ennead\n"
                + "방송 출처(GameCal): HTTP·JSON·이벤트 ID·코드 형식 검사\n"
                + "스타레일 방송 피드: " + hsrBroadcast + "\n"
                + "젠레스 방송 피드: " + zzzBroadcast + "\n"
                + "업데이트 저장소: " + (repo.isEmpty() ? "미설정" : repo) + "\n"
                + "버전: " + BuildConfig.VERSION_NAME + "\n"
                + "계정 비밀번호·쿠키 저장 안 함\n\n"
                + "최근 수집 오류: " + (lastError.isEmpty() ? "없음" : lastError);
        String[] menu = {"검사 상태 및 출처 보기", "검사 주기 변경", "앱 업데이트 확인", "GitHub 업데이트 저장소 설정", "알림 권한 요청", "JSON 백업 내보내기", "JSON 백업 가져오기", "코드 직접 추가"};
        new AlertDialog.Builder(this).setTitle("설정")
                .setItems(menu, (d, choice) -> {
                    switch (choice) {
                        case 0: new AlertDialog.Builder(this).setTitle("자동 검사 상태")
                                .setMessage(msg).setPositiveButton("확인", null).show(); break;
                        case 1: showIntervalDialog(); break;
                        case 2: checkUpdateNow(); break;
                        case 3: showUpdateRepositoryDialog(); break;
                        case 4: showNotificationSettings(); break;
                        case 5: exportBackup(); break;
                        case 6: importBackup(); break;
                        case 7: showAddDialog(); break;
                        default: break;
                    }
                }).setNegativeButton("닫기", null).show();
    }
    private void showIntervalDialog() {
        String[] labels = {"1시간 (방송 코드 우선)", "3시간", "6시간", "12시간", "24시간"};
        int[] hours = {1,3,6,12,24};
        new AlertDialog.Builder(this).setTitle("자동 검사 간격")
                .setItems(labels, (d, choice) -> {
                    CodeScheduler.schedule(this, hours[choice]);
                    notice("검사 주기가 " + hours[choice] + "시간으로 변경되었습니다.");
                    render();
                }).show();
    }
    private void showUpdateRepositoryDialog() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setHint("GitHub사용자/저장소 (예: myname/HoYoCodes)");
        edit.setText(UpdateChecker.configuredRepo(this));
        LinearLayout wrap = column();
        padding(wrap, 16, 10, 16, 6);
        wrap.addView(text("본인이 관리하는 공개 GitHub 저장소만 연결하세요.\n해당 저장소에 서명된 APK Release를 배포하면 자동 감지합니다.", 13, MUTED, false));
        wrap.addView(edit, vertical(-1, -2, 0));
        new AlertDialog.Builder(this).setTitle("업데이트 배포 저장소")
                .setView(wrap).setNegativeButton("취소", null)
                .setNeutralButton("연결 해제", (d, which) -> {
                    UpdateChecker.setRepo(this, ""); notice("업데이트 확인 기능이 꺼졌습니다");
                })
                .setPositiveButton("저장", (d, which) -> {
                    try { UpdateChecker.setRepo(this, edit.getText().toString());
                        notice("저장 완료. 업데이트 확인을 실행해 주세요"); }
                    catch (Exception ex) { notice(ex.getMessage()); }
                }).show();
    }
    private void checkUpdateNow() {
        if (UpdateChecker.configuredRepo(this).isEmpty()) {
            showUpdateRepositoryDialog(); return;
        }
        notice("GitHub 최신 버전을 확인하는 중입니다");
        new Thread(() -> {
            try {
                UpdateChecker.Release release = UpdateChecker.checkNow(getApplicationContext());
                mainHandler.post(() -> {
                    String message = release.newer
                        ? "새 버전 " + release.tag + "을 찾았습니다.\n현재 버전 " + BuildConfig.VERSION_NAME
                           + "\n\n다운로드 후 Android 설치 확인이 필요합니다."
                        : "현재 버전 " + BuildConfig.VERSION_NAME + "\n배포 최신 버전 " + release.tag + "\n업데이트가 필요하지 않습니다.";
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle("앱 업데이트 확인").setMessage(message)
                        .setNegativeButton("닫기", null);
                    if (release.newer && !release.apkUrl.isEmpty()) {
                        dialog.setPositiveButton("APK 다운로드 페이지 열기", (d, which) -> {
                            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl));
                            open.addCategory(Intent.CATEGORY_BROWSABLE);
                            try { startActivity(open); }
                            catch (Exception ex) { notice("브라우저를 열 수 없습니다"); }
                        });
                    } else if (release.newer) {
                        dialog.setMessage(message + "\n릴리스에 APK 파일이 없어 다운로드할 수 없습니다.");
                    }
                    dialog.show();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> new AlertDialog.Builder(this)
                    .setTitle("업데이트 확인 실패")
                    .setMessage("GitHub 공개 릴리스를 확인할 수 없습니다.\n" + ex.getMessage())
                    .setPositiveButton("확인", null).show());
            }
        }, "hoyocodes-update-check").start();
    }

    private void showNotificationSettings() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        } else {
            Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            try { startActivity(intent); } catch (Exception ex) { notice("설정에서 앱 알림을 허용해 주세요"); }
        }
    }
    private void exportBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "hoyo-codes-backup.json");
        startActivityForResult(intent, EXPORT_REQUEST);
    }
    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_REQUEST);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == EXPORT_REQUEST) {
                String json;
                try (CodeStore store = new CodeStore(this)) { json = store.exportJson(); }
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IllegalStateException("백업 파일을 열 수 없음");
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
                notice("백업이 저장되었습니다");
            } else if (requestCode == IMPORT_REQUEST) {
                String json;
                try (InputStream input = getContentResolver().openInputStream(uri);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    if (input == null) throw new IllegalStateException("백업 파일을 열 수 없음");
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = input.read(buffer)) != -1) {
                        if (out.size() + n > 2 * 1024 * 1024) throw new IllegalArgumentException("백업 파일이 너무 큽니다");
                        out.write(buffer, 0, n);
                    }
                    json = out.toString(StandardCharsets.UTF_8.name());
                }
                try (CodeStore store = new CodeStore(this)) {
                    int count = store.importJson(json);
                    notice("백업을 불러왔습니다. 신규 " + count + "개");
                }
                render();
            }
        } catch (Exception ex) { notice("백업 처리 실패: " + ex.getMessage()); }
    }
}
