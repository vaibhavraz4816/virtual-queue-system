package com.queueapp.dto;

/**
 * Shape of the JSON returned by /api/tokenStatus - polled every few
 * seconds by my_token.jsp to update a customer's place in line.
 */
public class TokenStatusResponse {

    private final int tokenNumber;
    private final String status;
    private final int peopleAhead;
    private final int estimatedWaitMinutes;
    private final String shopName;

    public TokenStatusResponse(int tokenNumber, String status, int peopleAhead,
                                int estimatedWaitMinutes, String shopName) {
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.peopleAhead = peopleAhead;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.shopName = shopName;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public String getStatus() {
        return status;
    }

    public int getPeopleAhead() {
        return peopleAhead;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public String getShopName() {
        return shopName;
    }
}
