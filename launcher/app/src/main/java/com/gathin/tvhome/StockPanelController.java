package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Lifecycle boundary to the optional companion. All IPC/parsing is off the UI thread. */
final class StockPanelController {
    private static final String AUTHORITY = "com.pennyos.marketdata.quotes";
    private static final Uri URI = Uri.parse("content://" + AUTHORITY + "/quote");
    private static final long REFRESH_MS = 15 * 60 * 1000L;
    private final Context context;
    private final StockPanelView view;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile int generation;
    private volatile boolean started;
    private final ContentObserver observer = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) { if (started) read(false); }
    };
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!started) return;
            read(true);
            main.postDelayed(this, REFRESH_MS);
        }
    };

    StockPanelController(Context context, StockPanelView view) {
        this.context = context.getApplicationContext();
        this.view = view;
    }
    void start() {
        if (started) return;
        started = true;
        generation++;
        context.getContentResolver().registerContentObserver(URI, false, observer);
        refresh.run();
    }
    void stop() {
        if (!started) return;
        started = false;
        generation++;
        main.removeCallbacks(refresh);
        context.getContentResolver().unregisterContentObserver(observer);
    }
    void close() { stop(); worker.shutdownNow(); }

    private void read(boolean requestRefresh) {
        int requestGeneration = generation;
        worker.execute(() -> {
            boolean available = false;
            JSONObject snapshot = null;
            try {
                ProviderInfo info = context.getPackageManager().resolveContentProvider(AUTHORITY, 0);
                available = info != null && "com.pennyos.marketdata".equals(info.packageName);
                if (available) {
                    ContentResolver resolver = context.getContentResolver();
                    try (Cursor cursor = resolver.query(URI, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            JSONObject candidate = new JSONObject(cursor.getString(0));
                            double price = candidate.optDouble("price", Double.NaN);
                            if (candidate.optInt("version") == 1 && "VINCIT.HE".equals(candidate.optString("symbol"))
                                    && "EUR".equals(candidate.optString("currency")) && Double.isFinite(price) && price > 0
                                    && candidate.optLong("as_of_ms") > 0) snapshot = candidate;
                        }
                    }
                    if (requestRefresh && started && generation == requestGeneration)
                        resolver.call(URI, "refresh", null, null);
                }
            } catch (Exception ignored) {
                // Companion missing, updating, or unavailable: do not disturb Home or discard an already rendered quote.
            }
            boolean show = available;
            JSONObject quote = snapshot;
            main.post(() -> {
                if (!started || generation != requestGeneration) return;
                view.setVisibility(show ? View.VISIBLE : View.GONE);
                if (quote != null) view.show(quote);
            });
        });
    }
}
