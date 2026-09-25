package com.queueease.entity;

import com.queueease.entity.enums.ShopStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shops", indexes = {
    @Index(name = "idx_shop_slug", columnList = "slug", unique = true),
    @Index(name = "idx_shop_owner", columnList = "owner_id")
})
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer averageServiceMinutes = 10;

    @Column(name = "opening_time")
    private LocalTime openingTime = LocalTime.of(9, 0);

    @Column(name = "closing_time")
    private LocalTime closingTime = LocalTime.of(21, 0);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopStatus status = ShopStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private com.queueease.entity.enums.ShopCategory category = com.queueease.entity.enums.ShopCategory.SALON;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Shop() {
    }

    public Shop(User owner, String name, String slug, String address, String phone, String description, Integer averageServiceMinutes) {
        this.owner = owner;
        this.name = name;
        this.slug = slug;
        this.address = address;
        this.phone = phone;
        this.description = description;
        this.averageServiceMinutes = (averageServiceMinutes != null && averageServiceMinutes > 0) ? averageServiceMinutes : 10;
        this.status = ShopStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.averageServiceMinutes == null || this.averageServiceMinutes <= 0) {
            this.averageServiceMinutes = 10;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    public com.queueease.entity.enums.ShopCategory getCategory() {
        return category;
    }

    public void setCategory(com.queueease.entity.enums.ShopCategory category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
