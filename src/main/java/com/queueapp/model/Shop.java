package com.queueapp.model;

import java.sql.Timestamp;

/**
 * Represents a shop / clinic / salon that runs a queue.
 */
public class Shop {

    private int shopId;
    private String shopName;
    private String category;
    private String username;
    private String passwordHash;
    private int avgServiceTimeMins;
    private boolean open;
    private Timestamp createdAt;

    public Shop() {
    }

    public Shop(String shopName, String category, String username,
                String passwordHash, int avgServiceTimeMins) {
        this.shopName = shopName;
        this.category = category;
        this.username = username;
        this.passwordHash = passwordHash;
        this.avgServiceTimeMins = avgServiceTimeMins;
        this.open = true;
    }

    public int getShopId() {
        return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getAvgServiceTimeMins() {
        return avgServiceTimeMins;
    }

    public void setAvgServiceTimeMins(int avgServiceTimeMins) {
        this.avgServiceTimeMins = avgServiceTimeMins;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
