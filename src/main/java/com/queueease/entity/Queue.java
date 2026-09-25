package com.queueease.entity;

import com.queueease.entity.enums.QueueStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queues", indexes = {
    @Index(name = "idx_queue_shop_date", columnList = "shop_id, queue_date", unique = true)
})
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate = LocalDate.now();

    @Column(name = "current_token_number", nullable = false)
    private Integer currentTokenNumber = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status = QueueStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Queue() {
    }

    public Queue(Shop shop, LocalDate queueDate) {
        this.shop = shop;
        this.queueDate = queueDate != null ? queueDate : LocalDate.now();
        this.currentTokenNumber = 0;
        this.status = QueueStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.queueDate == null) {
            this.queueDate = LocalDate.now();
        }
        if (this.currentTokenNumber == null) {
            this.currentTokenNumber = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getCurrentTokenNumber() {
        return currentTokenNumber;
    }

    public void setCurrentTokenNumber(Integer currentTokenNumber) {
        this.currentTokenNumber = currentTokenNumber;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
