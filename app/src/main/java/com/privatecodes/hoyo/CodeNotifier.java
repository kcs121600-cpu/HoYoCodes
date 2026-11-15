package com.privatecodes.hoyo;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.List;

public final class CodeNotifier {
    private CodeNotifier() {}
    private static final String CHANNEL_ID = "new_redeem_codes";
    private static final int SUMMARY_ID = 31006;

    public static void setup(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "새 리딤코드", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("스타레일·젠존제의 새로운 리딤코드 알림");
        nm.createNotificationChannel(ch);
    }
    public static boolean canNotify(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false;
        return ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled();
    }
    public static void notifyNew(Context ctx, List<CodeStore.Item> newItems) {
        if (newItems.isEmpty() || !canNotify(ctx)) return;
        setup(ctx);
        int hsr = 0, zzz = 0;
        for (CodeStore.Item item : newItems) {
            if (CodeRules.HSR.equals(item.game)) hsr++; else zzz++;
        }
        CodeStore.Item first = newItems.get(0);
        String summary = "스타레일 " + hsr + "개 · 젠존제 " + zzz + "개";
        Intent open = new Intent(ctx, MainActivity.class).setAction(MainActivity.ACTION_OPEN_CODE);
        open.putExtra(MainActivity.EXTRA_GAME, first.game);
        open.putExtra(MainActivity.EXTRA_CODE, first.code);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openIntent = PendingIntent.getActivity(ctx, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent list = new Intent(ctx, MainActivity.class).setAction(MainActivity.ACTION_LIST);
        list.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent listIntent = PendingIntent.getActivity(ctx, 11, list,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("새 리딤코드 " + newItems.size() + "개 발견")
                .setContentText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(summary + "\n등록하기를 누르면 첫 번째 코드가 입력된 공식 사이트가 열립니다."))
                .setContentIntent(listIntent)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_view, "첫 코드 등록하기", openIntent).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_agenda, "전체 코드 보기", listIntent).build())
                .build();
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(SUMMARY_ID, notif);
    }
}
