package com.example.mocktester.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Exposed to Thymeleaf as the bean "fmt": renders UTC database values in the user's timezone. */
@Component("fmt")
public class DateFormatter {

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");

    public String dt(String dbValue, String timezone) {
        if (dbValue == null || dbValue.isBlank()) {
            return "";
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone == null || timezone.isBlank() ? "UTC" : timezone);
        } catch (Exception e) {
            zone = ZoneOffset.UTC;
        }
        Instant instant = Instant.parse(dbValue);
        return OUT.format(instant.atZone(zone));
    }
}
