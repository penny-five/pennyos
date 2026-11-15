package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Passive native header panel: no focus targets, animation, networking, or periodic drawing. */
public final class StockPanelView extends LinearLayout {
    private final TextView price;
    private final TextView[] changes = new TextView[4];
    private static final String[] PERIODS = {"day", "week", "month", "year"};
    private static final String[] LABELS = {"1D", "1W", "1M", "1Y"};
    private final NumberFormat money = NumberFormat.getNumberInstance();
    private final NumberFormat percent = NumberFormat.getNumberInstance();
    private String displayed;

    public StockPanelView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setOrientation(VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(3);
        percent.setMinimumFractionDigits(1);
        percent.setMaximumFractionDigits(1);
        LinearLayout heading = new LinearLayout(context);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.vincit_logo);
        heading.addView(logo, new LayoutParams(dp(57), dp(18)));
        price = text(19);
        price.setTypeface(getResources().getFont(R.font.montserrat_semibold));
        // Android 8 ignores the variable font's weight; embolden this one small value there.
        if (Build.VERSION.SDK_INT < 28) price.getPaint().setFakeBoldText(true);
        LayoutParams priceParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        priceParams.leftMargin = dp(12);
        heading.addView(price, priceParams);
        addView(heading);
        LinearLayout row = new LinearLayout(context);
        GradientDrawable divider = new GradientDrawable();
        divider.setColor(0x33E5EEF8);
        divider.setSize(dp(1), dp(12));
        row.setDividerDrawable(divider);
        row.setShowDividers(SHOW_DIVIDER_MIDDLE);
        row.setDividerPadding(dp(2));
        LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(4);
        for (int i = 0; i < changes.length; i++) {
            changes[i] = text(14);
            changes[i].setSingleLine(true);
            if (i > 0) changes[i].setPadding(dp(8), 0, 0, 0);
            row.addView(changes[i], new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        }
        addView(row, rowParams);
        show(null);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(int size) {
        TextView view = new TextView(getContext());
        view.setTextSize(size);
        view.setTextColor(0xFFF2F5FA);
        view.setTypeface(getResources().getFont(R.font.montserrat_regular), Typeface.NORMAL);
        view.setIncludeFontPadding(false);
        view.setShadowLayer(dp(3), 0, dp(1), 0xD014102E);
        return view;
    }

    void show(JSONObject quote) {
        String key = quote == null ? "" : quote.toString();
        if (key.equals(displayed)) return;
        displayed = key;
        price.setText(quote == null ? "— €" : money.format(quote.optDouble("price")) + " €");
        for (int i = 0; i < changes.length; i++) {
            double value = quote == null ? Double.NaN : quote.optDouble("change_" + PERIODS[i], Double.NaN);
            String formatted = Double.isFinite(value) ? (value > 0 ? "+" : "") + percent.format(value).replace('-', '−') + "%" : "—";
            SpannableString cell = new SpannableString(LABELS[i] + " " + formatted);
            cell.setSpan(new ForegroundColorSpan(0xBFE5EEF8), 0, LABELS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int color = !Double.isFinite(value) || value == 0 ? 0xD9E5EEF8 : value > 0 ? 0xFF99DEC4 : 0xFFFFAFBD;
            cell.setSpan(new ForegroundColorSpan(color), LABELS[i].length() + 1, cell.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            changes[i].setText(cell);
        }
        String quoteStatus;
        if (quote == null) {
            quoteStatus = getResources().getString(R.string.stock_waiting);
        } else {
            Locale locale = getResources().getConfiguration().getLocales().get(0);
            String date = DateTimeFormatter.ofPattern("d.M. HH:mm", locale).withZone(ZoneId.of("Europe/Helsinki"))
                    .format(Instant.ofEpochMilli(quote.optLong("as_of_ms")));
            quoteStatus = getResources().getString(R.string.stock_as_of, date);
        }
        setContentDescription("Vincit. " + price.getText() + ". " + quoteStatus);
    }
}
