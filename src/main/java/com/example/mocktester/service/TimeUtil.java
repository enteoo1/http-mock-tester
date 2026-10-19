package com.example.mocktester.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** All timestamps are stored in UTC using a fixed-width ISO format (sortable as text). */
public final class TimeUtil {

    private static final DateTimeFormatter DB =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private TimeUtil() {
    }

    public static String toDb(Instant instant) {
        return DB.format(instant);
    }

    public static String nowDb() {
        return toDb(Instant.now());
    }

    public static Instant fromDb(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
