package com.queueease.entity.enums;

public enum ShopCategory {
    SALON("Salon & Styling"),
    BARBER("Barber Shop"),
    CLINIC("Clinic & Healthcare"),
    REPAIR("Repair Center"),
    RESTAURANT("Restaurant & Cafe"),
    CONSULTATION("Consultation & Advisory"),
    OTHER("Other Service");

    private final String displayName;

    ShopCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
