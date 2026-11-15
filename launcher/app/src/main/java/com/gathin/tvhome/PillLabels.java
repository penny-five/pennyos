package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import java.util.List;

/**
 * labels_on_focus: a top-bar pill rests as a round icon and slides its label out while it has the focus. The
 * width, the paddings and the gap after the icon move together from the icon-only shape (makeIconOnly's 42dp
 * circle) to the full pill, and the label fades in over the last two thirds, so the neighbours glide aside
 * instead of jumping. The label stays in the TextView the whole time, so talkback still reads it and a Wi-Fi name
 * that arrives while the pill is folded is simply there the next time it opens.
 */
final class PillLabels {
    private static final int EXPAND_MS = 260;
    private static final int COLLAPSE_MS = 200;
    /** Material's emphasized easing: a quick start that settles softly. */
    private static final PathInterpolator EASE = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final TextView pill;
    private final int iconWidth;
    private final int restPad;
    private final int startPad;
    private final int endPad;
    private final int gap;
    private final ColorStateList ink;
    private final int expandMs;
    private final int collapseMs;
    private ValueAnimator animator;
    private float shown;
    private int from;
    private int to;

    /**
     * Folds each pill in PILLS and opens the one that takes the focus; call after the other focus listeners. A pill
     * with no text of its own (Reorder, the library button) shows its content description as the label; one with
     * neither is left alone.
     */
    static void apply(List<TextView> pills, float density, boolean reducedMotion) {
        for (TextView pill : pills) {
            if (pill.getText().length() == 0 && pill.getContentDescription() != null) {
                pill.setText(pill.getContentDescription());
            }
            if (pill.getText().length() > 0) new PillLabels(pill, density, reducedMotion);
        }
    }

    private PillLabels(TextView pill, float density, boolean reducedMotion) {
        this.pill = pill;
        iconWidth = Math.round(42 * density);
        Drawable icon = pill.getCompoundDrawablesRelative()[0];
        int glyph = icon == null ? 0 : icon.getBounds().width();
        restPad = Math.max(0, (iconWidth - glyph) / 2);
        // TopButtonBase's label paddings, also for the pills styled as icon-only (Reorder, the library button).
        startPad = Math.round(16 * density);
        endPad = Math.round(20 * density);
        gap = Math.round(9 * density);
        ink = pill.getTextColors();
        expandMs = reducedMotion ? Motion.REDUCED_MS : EXPAND_MS;
        collapseMs = reducedMotion ? Motion.REDUCED_MS : COLLAPSE_MS;
        pill.setSingleLine(true);
        from = to = iconWidth;
        set(0f);

        View.OnFocusChangeListener previous = pill.getOnFocusChangeListener();
        pill.setOnFocusChangeListener((v, focused) -> {
            if (previous != null) previous.onFocusChange(v, focused);
            animateTo(focused);
        });
    }

    private void animateTo(boolean open) {
        if (animator != null) animator.cancel();
        float start = shown;
        float end = open ? 1f : 0f;
        // Opening measures the label now, so a network name that changed while folded gets its room; closing
        // starts from whatever width the pill has, wrapped or mid-animation.
        from = pill.getWidth() > 0 ? pill.getWidth() : iconWidth;
        to = open ? fullWidth() : iconWidth;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(Math.round((open ? expandMs : collapseMs) * Math.abs(end - start)));
        animator.setInterpolator(EASE);
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            set(start + (end - start) * t, from + (to - from) * t);
        });
        animator.start();
    }

    /** The pill's width with its label: paddings, icon, gap and the text, capped by its maxWidth (Wi-Fi). */
    private int fullWidth() {
        CharSequence text = pill.getText();
        if (text == null || text.length() == 0) return iconWidth;
        Drawable icon = pill.getCompoundDrawablesRelative()[0];
        int glyph = icon == null ? 0 : icon.getBounds().width();
        int width = startPad + glyph + gap + (int) Math.ceil(pill.getPaint().measureText(text, 0, text.length())) + endPad;
        int max = pill.getMaxWidth();
        return max > 0 ? Math.min(width, max) : width;
    }

    private void set(float f) {
        set(f, iconWidth);
    }

    private void set(float f, float width) {
        shown = f;
        ViewGroup.LayoutParams lp = pill.getLayoutParams();
        // Fully open, the pill wraps again, so a label that changes while it has the focus reflows on its own.
        lp.width = f >= 1f ? ViewGroup.LayoutParams.WRAP_CONTENT : Math.round(width);
        pill.setLayoutParams(lp);
        pill.setPaddingRelative(lerp(restPad, startPad, f), pill.getPaddingTop(), lerp(restPad, endPad, f),
                pill.getPaddingBottom());
        pill.setCompoundDrawablePadding(lerp(0, gap, f));
        float label = Math.max(0f, Math.min(1f, (f - 0.35f) / 0.65f));
        pill.setTextColor(label >= 1f ? ink : ink.withAlpha(Math.round(255 * label)));
    }

    private static int lerp(int a, int b, float f) {
        return Math.round(a + (b - a) * f);
    }
}
