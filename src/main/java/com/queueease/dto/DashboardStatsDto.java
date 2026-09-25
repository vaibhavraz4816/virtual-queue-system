package com.queueease.dto;

import com.queueease.entity.enums.ShopStatus;

public class DashboardStatsDto {

    private long customersWaiting;
    private String currentServingToken;
    private String currentCustomerName;
    private Long currentEntryId;
    private String currentStatus;
    private long currentSecondsRemaining;
    private int averageWaitMinutes;
    private long servedToday;
    private long skippedToday;
    private long totalToday;
    private String serviceStartTime;
    private String shopName;
    private String categoryName;
    private ShopStatus shopStatus;
    private boolean isPaused;
    private boolean isClosed;

    public String getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(String serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public DashboardStatsDto() {
    }

    public long getCustomersWaiting() {
        return customersWaiting;
    }

    public void setCustomersWaiting(long customersWaiting) {
        this.customersWaiting = customersWaiting;
    }

    public String getCurrentServingToken() {
        return currentServingToken;
    }

    public void setCurrentServingToken(String currentServingToken) {
        this.currentServingToken = currentServingToken;
    }

    public String getCurrentCustomerName() {
        return currentCustomerName;
    }

    public void setCurrentCustomerName(String currentCustomerName) {
        this.currentCustomerName = currentCustomerName;
    }

    public Long getCurrentEntryId() {
        return currentEntryId;
    }

    public void setCurrentEntryId(Long currentEntryId) {
        this.currentEntryId = currentEntryId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public long getCurrentSecondsRemaining() {
        return currentSecondsRemaining;
    }

    public void setCurrentSecondsRemaining(long currentSecondsRemaining) {
        this.currentSecondsRemaining = currentSecondsRemaining;
    }

    public int getAverageWaitMinutes() {
        return averageWaitMinutes;
    }

    public void setAverageWaitMinutes(int averageWaitMinutes) {
        this.averageWaitMinutes = averageWaitMinutes;
    }

    public long getServedToday() {
        return servedToday;
    }

    public void setServedToday(long servedToday) {
        this.servedToday = servedToday;
    }

    public long getSkippedToday() {
        return skippedToday;
    }

    public void setSkippedToday(long skippedToday) {
        this.skippedToday = skippedToday;
    }

    public long getTotalToday() {
        return totalToday;
    }

    public void setTotalToday(long totalToday) {
        this.totalToday = totalToday;
    }

    public ShopStatus getShopStatus() {
        return shopStatus;
    }

    public void setShopStatus(ShopStatus shopStatus) {
        this.shopStatus = shopStatus;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }
}
