package com.pennyos.marketdata;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.net.ssl.HttpsURLConnection;

/** Replaceable transport/parser boundary. No credentials, cookies, HTML scraping or third-party SDK. */
final class YahooPriceSource {
    private static final String URL = "https://query1.finance.yahoo.com/v8/finance/chart/VINCIT.HE?range=2y&interval=1d";
    private static final ZoneId HELSINKI = ZoneId.of("Europe/Helsinki");

    JSONObject fetch() throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "PennyOS-MarketData/1.0");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Price feed HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (bytes.size() + n > 1024 * 1024) throw new IOException("Price feed too large");
                    bytes.write(buffer, 0, n);
                }
                return parse(bytes.toString(StandardCharsets.UTF_8.name()), System.currentTimeMillis());
            }
        } finally { connection.disconnect(); }
    }

    static JSONObject parse(String json, long fetchedAt) throws Exception {
        JSONObject result = new JSONObject(json).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
        JSONObject meta = result.getJSONObject("meta");
        if (!"VINCIT.HE".equals(meta.getString("symbol")) || !"EUR".equals(meta.getString("currency")))
            throw new IOException("Unexpected instrument");
        double price = meta.getDouble("regularMarketPrice");
        long asOf = Math.multiplyExact(meta.getLong("regularMarketTime"), 1000L);
        if (!Double.isFinite(price) || price <= 0 || asOf <= 0 || asOf > fetchedAt + 300000)
            throw new IOException("Invalid quote");
        LocalDate date = Instant.ofEpochMilli(asOf).atZone(HELSINKI).toLocalDate();
        JSONArray times = result.getJSONArray("timestamp");
        JSONArray closes = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
        if (times.length() != closes.length() || times.length() > 1000) throw new IOException("Invalid history");
        PriceHistory history = new PriceHistory();
        for (int i = 0; i < times.length(); i++) {
            if (times.isNull(i) || closes.isNull(i)) continue;
            long time = Math.multiplyExact(times.getLong(i), 1000L);
            if (time > asOf) continue;
            history.add(Instant.ofEpochMilli(time).atZone(HELSINKI).toLocalDate(), closes.getDouble(i));
        }
        JSONObject snapshot = new JSONObject().put("version", 1).put("symbol", "VINCIT.HE")
                .put("currency", "EUR").put("price", price).put("as_of_ms", asOf).put("fetched_at_ms", fetchedAt);
        for (String period : new String[]{"day", "week", "month", "year"}) {
            Double change = history.change(price, date, period);
            snapshot.put("change_" + period, change == null ? JSONObject.NULL : change);
        }
        return snapshot;
    }
}
