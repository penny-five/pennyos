package com.pennyos.marketdata;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class YahooPriceSourceTest {
    private String feed(String symbol, String price) {
        return "{\"chart\":{\"result\":[{\"meta\":{\"symbol\":\"" + symbol
                + "\",\"currency\":\"EUR\",\"regularMarketPrice\":" + price
                + ",\"regularMarketTime\":1794578400},\"timestamp\":[1794405600,1794492000,1794578400],"
                + "\"indicators\":{\"quote\":[{\"close\":[2,null,3]}]}}]}}";
    }
    @Test public void nullClosesAreSkippedAndTimestampIsPreserved() throws Exception {
        JSONObject quote = YahooPriceSource.parse(feed("VINCIT.HE", "3"), 1794580000000L);
        assertEquals(50, quote.getDouble("change_day"), 0.0001);
        assertEquals(1794578400000L, quote.getLong("as_of_ms"));
        assertTrue(quote.isNull("change_year"));
    }
    @Test(expected = java.io.IOException.class) public void rejectsWrongSymbol() throws Exception {
        YahooPriceSource.parse(feed("OTHER", "3"), 1794580000000L);
    }
    @Test(expected = java.io.IOException.class) public void rejectsZeroPrice() throws Exception {
        YahooPriceSource.parse(feed("VINCIT.HE", "0"), 1794580000000L);
    }
    @Test(expected = java.io.IOException.class) public void rejectsFutureQuote() throws Exception {
        YahooPriceSource.parse(feed("VINCIT.HE", "3"), 1794500000000L);
    }
}
