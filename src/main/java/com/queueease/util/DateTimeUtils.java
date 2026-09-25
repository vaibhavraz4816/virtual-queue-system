package com.queueease.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    public DateTimeUtils() {
    }

    public static String formatTime(LocalTime time) {
        if (time == null) return "--:--";
        return time.format(TIME_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "--:--";
        return dateTime.format(TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "--";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "--";
        Duration duration = Duration.between(start, end);
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) {
            return "< 1 min";
        }
        return minutes + " min" + (minutes > 1 ? "s" : "");
    }
}
