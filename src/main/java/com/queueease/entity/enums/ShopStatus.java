package com.queueease.entity.enums;

public enum ShopStatus {
    OPEN("Open"),
    CLOSED("Closed"),
    QUEUE_PAUSED("Queue Paused");

    private final String displayName;

    ShopStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
