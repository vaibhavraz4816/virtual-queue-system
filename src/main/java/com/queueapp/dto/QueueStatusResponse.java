package com.queueapp.dto;

import java.util.List;

/**
 * Shape of the JSON returned by /api/queueStatus - polled by the public
 * "Now Serving" display screen and the shop owner's dashboard.
 * Deliberately only exposes token numbers, never customer names/phones,
 * since this endpoint is public and unauthenticated.
 */
public class QueueStatusResponse {

    private final String shopName;
    private final boolean open;
    private final Integer currentTokenNumber; // null when nobody is being served
    private final List<Integer> waitingTokenNumbers;
    private final int avgServiceTimeMins;

    public QueueStatusResponse(String shopName, boolean open, Integer currentTokenNumber,
                                List<Integer> waitingTokenNumbers, int avgServiceTimeMins) {
        this.shopName = shopName;
        this.open = open;
        this.currentTokenNumber = currentTokenNumber;
        this.waitingTokenNumbers = waitingTokenNumbers;
        this.avgServiceTimeMins = avgServiceTimeMins;
    }

    public String getShopName() {
        return shopName;
    }

    public boolean isOpen() {
        return open;
    }

    public Integer getCurrentTokenNumber() {
        return currentTokenNumber;
    }

    public List<Integer> getWaitingTokenNumbers() {
        return waitingTokenNumbers;
    }

    public int getAvgServiceTimeMins() {
        return avgServiceTimeMins;
    }
}
