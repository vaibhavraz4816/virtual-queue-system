package com.queueease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JoinQueueRequest {

    @NotBlank(message = "Please enter your name")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String customerName;

    @Size(max = 20, message = "Phone number too long")
    private String phone;

    public JoinQueueRequest() {
    }

    public JoinQueueRequest(String customerName, String phone) {
        this.customerName = customerName;
        this.phone = phone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
