package com.queueease.dto;

import java.time.LocalDateTime;

public class CustomerQueueStatusDto {

    private String token;
    private String guestAccessToken;
    private String status;
    private String statusDisplayName;
    private String humanStatusMessage;
    private int position;
    private int peopleAhead;
    private int estimatedWait; // in minutes
    private String currentServing;
    private int queueSize;
    private boolean shopOpen;
    private String shopStatus;
    private String shopName;
    private String shopSlug;
    private String shopCategory;
    private String shopAddress;
    private String shopPhone;
    private String customerName;
    private String joinedTimeFormatted;
    private LocalDateTime calledAt;
    private LocalDateTime callExpiresAt;
    private long secondsRemaining;
    private boolean canArrive;
    private boolean canLeave;

    public CustomerQueueStatusDto() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getGuestAccessToken() {
        return guestAccessToken;
    }

    public void setGuestAccessToken(String guestAccessToken) {
        this.guestAccessToken = guestAccessToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDisplayName() {
        return statusDisplayName;
    }

    public void setStatusDisplayName(String statusDisplayName) {
        this.statusDisplayName = statusDisplayName;
    }

    public String getHumanStatusMessage() {
        return humanStatusMessage;
    }

    public void setHumanStatusMessage(String humanStatusMessage) {
        this.humanStatusMessage = humanStatusMessage;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPeopleAhead() {
        return peopleAhead;
    }

    public void setPeopleAhead(int peopleAhead) {
        this.peopleAhead = peopleAhead;
    }

    public int getEstimatedWait() {
        return estimatedWait;
    }

    public void setEstimatedWait(int estimatedWait) {
        this.estimatedWait = estimatedWait;
    }

    public String getCurrentServing() {
        return currentServing;
    }

    public void setCurrentServing(String currentServing) {
        this.currentServing = currentServing;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public boolean isShopOpen() {
        return shopOpen;
    }

    public void setShopOpen(boolean shopOpen) {
        this.shopOpen = shopOpen;
    }

    public String getShopStatus() {
        return shopStatus;
    }

    public void setShopStatus(String shopStatus) {
        this.shopStatus = shopStatus;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopSlug() {
        return shopSlug;
    }

    public void setShopSlug(String shopSlug) {
        this.shopSlug = shopSlug;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String shopCategory) {
        this.shopCategory = shopCategory;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public String getShopPhone() {
        return shopPhone;
    }

    public void setShopPhone(String shopPhone) {
        this.shopPhone = shopPhone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getJoinedTimeFormatted() {
        return joinedTimeFormatted;
    }

    public void setJoinedTimeFormatted(String joinedTimeFormatted) {
        this.joinedTimeFormatted = joinedTimeFormatted;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getCallExpiresAt() {
        return callExpiresAt;
    }

    public void setCallExpiresAt(LocalDateTime callExpiresAt) {
        this.callExpiresAt = callExpiresAt;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    public boolean isCanArrive() {
        return canArrive;
    }

    public void setCanArrive(boolean canArrive) {
        this.canArrive = canArrive;
    }

    public boolean isCanLeave() {
        return canLeave;
    }

    public void setCanLeave(boolean canLeave) {
        this.canLeave = canLeave;
    }
}
