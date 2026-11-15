package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/** Downsample the cutout once with successive filtered halves, avoiding aliasing at small header sizes. */
public final class QuotePortraitView extends ImageView {
    // One small, immutable display bitmap survives Activity recreation; the full-size source is not retained.
    private static Bitmap cached;
    private static int cachedSize;

    public QuotePortraitView(Context context, AttributeSet attributes) { super(context, attributes); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int size = Math.min(w - getPaddingLeft() - getPaddingRight(), h - getPaddingTop() - getPaddingBottom());
        if (size <= 0) return;
        if (cached == null || cachedSize != size) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.niilo_portrait, options);
            // A single bilinear 500 -> 52 reduction undersamples the thin white outline. Halving first
            // prefilters the source, then the final reduction resolves it cleanly at the physical pixel size.
            while (Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2 >= size) {
                Bitmap next = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                if (next != bitmap) bitmap.recycle();
                bitmap = next;
            }
            float scale = size / (float) Math.max(bitmap.getWidth(), bitmap.getHeight());
            Bitmap next = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)),
                    Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
            if (next != bitmap) bitmap.recycle();
            cached = next;
            cached.setHasMipMap(true);
            cachedSize = size;
        }
        BitmapDrawable drawable = new BitmapDrawable(getResources(), cached);
        drawable.setFilterBitmap(true);
        drawable.setAntiAlias(true);
        setImageDrawable(drawable);
    }
}
