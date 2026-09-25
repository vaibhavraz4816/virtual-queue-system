package com.queueease.repository;

import com.queueease.entity.Queue;
import com.queueease.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {
    Optional<Queue> findByShopAndQueueDate(Shop shop, LocalDate queueDate);
    Optional<Queue> findByShopIdAndQueueDate(Long shopId, LocalDate queueDate);
    List<Queue> findByShopOrderByQueueDateDesc(Shop shop);
}
