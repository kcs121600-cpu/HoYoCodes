package com.privatecodes.hoyo;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class CodeCheckJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            boolean retry = false;
            try {
                CodeChecker.Result result = CodeChecker.run(getApplicationContext());
                retry = result.workingGames == 0;
                UpdateChecker.maybeCheck(getApplicationContext());
            } catch (Exception ex) { retry = true; }
            // JobService callbacks must be executed on main thread.
            final boolean reschedule = retry;
            new android.os.Handler(getMainLooper()).post(() -> jobFinished(params, reschedule));
        }, "hoyocodes-job").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
