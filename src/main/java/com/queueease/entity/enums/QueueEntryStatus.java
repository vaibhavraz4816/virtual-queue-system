package com.queueease.entity.enums;

public enum QueueEntryStatus {
    WAITING("Waiting"),
    CALLED("Called"),
    ARRIVED("Arrived"),
    SERVING("Serving"),
    SERVED("Served"),
    SKIPPED("Skipped"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String displayName;

    QueueEntryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == WAITING || this == CALLED || this == ARRIVED || this == SERVING;
    }
}
