package com.example.mocktester.service;

import com.example.mocktester.repo.LogRepository;
import com.example.mocktester.repo.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RetentionService {

    public static final String KEY = "retention_days";
    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final SettingsRepository settings;
    private final LogRepository logs;
    private final int defaultDays;

    public RetentionService(SettingsRepository settings, LogRepository logs,
                            @Value("${app.default-retention-days:60}") int defaultDays) {
        this.settings = settings;
        this.logs = logs;
        this.defaultDays = defaultDays;
    }

    public int getRetentionDays() {
        return settings.get(KEY).map(v -> {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return defaultDays;
            }
        }).orElse(defaultDays);
    }

    public void setRetentionDays(int days) {
        settings.set(KEY, Integer.toString(days));
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    public void purge() {
        Instant cutoff = Instant.now().minus(getRetentionDays(), ChronoUnit.DAYS);
        int deleted = logs.deleteOlderThan(TimeUtil.toDb(cutoff));
        log.info("Retention job removed {} request log rows older than {}", deleted, cutoff);
    }
}
