package com.pennyos.marketdata;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** One bounded fetch, only requested while Home is visible. No periodic job, boot receiver or persistent service. */
public final class RefreshJob extends JobService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> task;
    @Override public boolean onStartJob(JobParameters parameters) {
        SharedPreferences cache = QuoteProvider.cache(this);
        long now = System.currentTimeMillis();
        long elapsed = now - cache.getLong("last_attempt", 0);
        if (elapsed >= 0 && elapsed < QuoteProvider.INTERVAL_MS) return false;
        cache.edit().putLong("last_attempt", now).apply();
        task = worker.submit(() -> {
            try {
                String snapshot = new YahooPriceSource().fetch().toString();
                if (!Thread.currentThread().isInterrupted()) {
                    cache.edit().putString("snapshot", snapshot).commit();
                    getContentResolver().notifyChange(QuoteProvider.URI, null);
                }
            } catch (Exception error) {
                // Retain the last good quote; Home displays its actual timestamp.
                Log.w("PennyOSPrices", "Price refresh failed", error);
            } finally { jobFinished(parameters, false); }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters parameters) {
        if (task != null) task.cancel(true);
        return false;
    }
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
