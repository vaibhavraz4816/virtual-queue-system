package com.queueapp.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Represents a single customer's place in a shop's queue for a given day.
 */
public class Token {

    // Mirrors the MySQL ENUM('WAITING','CALLED','SERVED','SKIPPED','CANCELLED')
    public static final String WAITING = "WAITING";
    public static final String CALLED = "CALLED";
    public static final String SERVED = "SERVED";
    public static final String SKIPPED = "SKIPPED";
    public static final String CANCELLED = "CANCELLED";

    private int tokenId;
    private int shopId;
    private int tokenNumber;
    private String customerName;
    private String customerPhone;
    private String status;
    private Date queueDate;
    private Timestamp joinedAt;
    private Timestamp calledAt;
    private Timestamp graceDeadline;
    private Timestamp servedAt;

    public Token() {
    }

    public int getTokenId() {
        return tokenId;
    }

    public void setTokenId(int tokenId) {
        this.tokenId = tokenId;
    }

    public int getShopId() {
        return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(Date queueDate) {
        this.queueDate = queueDate;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Timestamp getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Timestamp calledAt) {
        this.calledAt = calledAt;
    }

    public Timestamp getGraceDeadline() {
        return graceDeadline;
    }

    public void setGraceDeadline(Timestamp graceDeadline) {
        this.graceDeadline = graceDeadline;
    }

    public Timestamp getServedAt() {
        return servedAt;
    }

    public void setServedAt(Timestamp servedAt) {
        this.servedAt = servedAt;
    }
}
