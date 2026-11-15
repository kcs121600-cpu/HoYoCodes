package com.privatecodes.hoyo;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public final class CodeScheduler {
    private static final int JOB_ID = 81055;
    private CodeScheduler() {}
    public static int intervalHours(Context ctx) {
        return ctx.getSharedPreferences("hoyo_state", Context.MODE_PRIVATE).getInt("interval_hours", 1);
    }
    public static void schedule(Context ctx, int hours) {
        if (hours != 1 && hours != 3 && hours != 6 && hours != 12 && hours != 24) hours = 1;
        ctx.getSharedPreferences("hoyo_state", Context.MODE_PRIVATE).edit().putInt("interval_hours", hours).apply();
        JobScheduler jobs = (JobScheduler)ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(ctx, CodeCheckJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.HOURS.toMillis(hours))
                .setPersisted(true)
                .build();
        jobs.schedule(info);
    }
    public static void initialize(Context ctx) {
        JobScheduler jobs = (JobScheduler)ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobs.getPendingJob(JOB_ID) == null) schedule(ctx, intervalHours(ctx));
    }
}
