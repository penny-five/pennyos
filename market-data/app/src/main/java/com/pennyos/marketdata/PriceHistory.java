package com.pennyos.marketdata;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Price returns, excluding cash dividends. Calendar periods use the last available close on/before the target. */
final class PriceHistory {
    private final NavigableMap<LocalDate, Double> closes = new TreeMap<>();

    void add(LocalDate date, double price) {
        if (Double.isFinite(price) && price > 0) closes.put(date, price);
    }

    Double change(double price, LocalDate date, String period) {
        Map.Entry<LocalDate, Double> base;
        switch (period) {
            case "day": base = closes.lowerEntry(date); break;
            case "week": base = closes.floorEntry(date.minusWeeks(1)); break;
            case "month": base = closes.floorEntry(date.minusMonths(1)); break;
            case "year": base = closes.floorEntry(date.minusYears(1)); break;
            default: throw new IllegalArgumentException("Unknown period: " + period);
        }
        if (base == null || !Double.isFinite(price) || price <= 0) return null;
        return (price / base.getValue() - 1) * 100;
    }
}
