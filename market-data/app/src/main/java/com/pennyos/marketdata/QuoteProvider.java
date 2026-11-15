package com.pennyos.marketdata;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;

/** Public, non-sensitive cached snapshot. A query never performs network I/O. Only Home can request a refresh. */
public final class QuoteProvider extends ContentProvider {
    static final Uri URI = Uri.parse("content://com.pennyos.marketdata.quotes/quote");
    static final long INTERVAL_MS = 15 * 60 * 1000L;
    static final int JOB_ID = 1;
    static SharedPreferences cache(Context context) {
        return context.getSharedPreferences("quote", Context.MODE_PRIVATE);
    }
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!URI.equals(uri)) throw new IllegalArgumentException("Unknown quote URI");
        MatrixCursor cursor = new MatrixCursor(new String[]{"snapshot"});
        String snapshot = cache(getContext()).getString("snapshot", null);
        if (snapshot != null) cursor.addRow(new Object[]{snapshot});
        cursor.setNotificationUri(getContext().getContentResolver(), URI);
        return cursor;
    }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (!"refresh".equals(method)) throw new IllegalArgumentException("Unknown method");
        if (!"com.pennyos.home".equals(getCallingPackage()) && Binder.getCallingUid() != 2000)
            throw new SecurityException("Only PennyOS Home may request prices");
        // Schedule in the provider's identity, not the caller's. The job ends after one request.
        long identity = Binder.clearCallingIdentity();
        try {
            JobScheduler scheduler = getContext().getSystemService(JobScheduler.class);
            long elapsed = System.currentTimeMillis() - cache(getContext()).getLong("last_attempt", 0);
            if ((elapsed < 0 || elapsed >= INTERVAL_MS) && scheduler.getPendingJob(JOB_ID) == null) {
                scheduler.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(getContext(), RefreshJob.class))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build());
            }
        } finally { Binder.restoreCallingIdentity(identity); }
        return Bundle.EMPTY;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.pennyos.quote"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Read only"); }
    @Override public int update(Uri uri, ContentValues values, String s, String[] a) { throw new UnsupportedOperationException("Read only"); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException("Read only"); }
}
