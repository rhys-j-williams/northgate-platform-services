package com.meridian.platform.bedrockadapter.copybook;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** PIC X and PIC 9 helpers. Everything here is EBCDIC agnostic because the MQ channel converts. */
public final class Fixed {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private Fixed() {
    }

    public static String text(String value, int width) {
        String v = value == null ? "" : value;
        if (v.length() > width) {
            v = v.substring(0, width);
        }
        StringBuilder sb = new StringBuilder(v);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String numeric(long value, int width) {
        String v = Long.toString(value);
        if (v.length() > width) {
            throw new IllegalArgumentException("value " + value + " wider than " + width);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = v.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(v).toString();
    }

    public static String date(LocalDate d) {
        return d == null ? "        " : d.format(YYYYMMDD);
    }

    public static LocalDate date(String yyyymmdd) {
        String t = yyyymmdd.trim();
        if (t.isEmpty() || "00000000".equals(t)) {
            return null;
        }
        return LocalDate.parse(t, YYYYMMDD);
    }

    public static String slice(String record, int offset, int length) {
        return record.substring(offset, offset + length);
    }

    public static String trimmed(String record, int offset, int length) {
        return slice(record, offset, length).trim();
    }
}
