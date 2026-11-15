package com.gathin.tvhome;

/**
 * PennyOS shows the weekday on a line of its own above the date, as its mockups do. The weekday line is the
 * stand-alone "cccc" (Finnish "lauantai", not the essive "lauantaina"); the date line is the language's own
 * date_format with its weekday taken out, so every translation keeps its order and its words ("d 'de' MMMM",
 * "M월 d일", "d. MMMM").
 */
final class DateLines {
    private DateLines() {}

    /**
     * PATTERN without its full weekday field (EEEE or cccc, four letters or more) and the commas and spaces
     * around it, or null when it has none, in which case the date stays on one line as written. Quoted text
     * ('de') is kept as it is.
     */
    static String withoutWeekday(String pattern) {
        StringBuilder out = new StringBuilder();
        boolean found = false;
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); ) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                out.append(c);
                i++;
                continue;
            }
            if (!quoted && (c == 'E' || c == 'c')) {
                int end = i;
                while (end < pattern.length() && pattern.charAt(end) == c) end++;
                if (end - i >= 4) {
                    found = true;
                    i = end;
                    continue;
                }
                out.append(pattern, i, end);
                i = end;
                continue;
            }
            out.append(c);
            i++;
        }
        if (!found) return null;
        // What the weekday left behind: "  d MMMM", "d MMMM ", ", d. MMMM".
        String rest = out.toString().replaceAll("^[\\s,]+|[\\s,]+$", "").replaceAll("\\s{2,}", " ");
        return rest.isEmpty() ? null : rest;
    }
}
