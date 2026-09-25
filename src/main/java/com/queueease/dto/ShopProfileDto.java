package com.queueease.dto;

import com.queueease.entity.enums.ShopStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class ShopProfileDto {

    private Long id;

    @NotBlank(message = "Shop name is required")
    private String name;

    private String slug;
    private String address;
    private String phone;
    private String description;

    @Min(value = 1, message = "Average service duration must be at least 1 minute")
    private Integer averageServiceMinutes = 10;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closingTime;

    private ShopStatus status;
    private com.queueease.entity.enums.ShopCategory category;

    public ShopProfileDto() {
    }

    public com.queueease.entity.enums.ShopCategory getCategory() {
        return category;
    }

    public void setCategory(com.queueease.entity.enums.ShopCategory category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(Integer averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public ShopStatus getStatus() {
        return status;
    }

    public void setStatus(ShopStatus status) {
        this.status = status;
    }
}
