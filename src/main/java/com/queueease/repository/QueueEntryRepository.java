package com.queueease.repository;

import com.queueease.entity.Queue;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.enums.QueueEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByGuestAccessToken(String guestAccessToken);

    List<QueueEntry> findByQueueOrderByJoinedAtAsc(Queue queue);

    List<QueueEntry> findByQueueAndStatusInOrderByJoinedAtAsc(Queue queue, Collection<QueueEntryStatus> statuses);

    List<QueueEntry> findByQueueAndStatusOrderByJoinedAtAsc(Queue queue, QueueEntryStatus status);

    long countByQueueAndStatus(Queue queue, QueueEntryStatus status);

    long countByQueueAndStatusIn(Queue queue, Collection<QueueEntryStatus> statuses);

    // Find currently active serving/called customer for shop queue
    Optional<QueueEntry> findFirstByQueueAndStatusInOrderByCalledAtDesc(Queue queue, Collection<QueueEntryStatus> statuses);

    // Find next waiting customer (FIFO)
    Optional<QueueEntry> findFirstByQueueAndStatusOrderByJoinedAtAsc(Queue queue, QueueEntryStatus status);

    // Expired CALLED entries for automatic no-show skipping
    List<QueueEntry> findByStatusAndCallExpiresAtBefore(QueueEntryStatus status, LocalDateTime now);

    // Find entries by list of guest tokens
    List<QueueEntry> findByGuestAccessTokenIn(Collection<String> guestAccessTokens);

    // Check if session already has an active entry in this shop
    boolean existsByShopAndSessionIdAndStatusIn(Shop shop, String sessionId, Collection<QueueEntryStatus> statuses);

    Optional<QueueEntry> findFirstByShopAndSessionIdAndStatusInOrderByJoinedAtDesc(Shop shop, String sessionId, Collection<QueueEntryStatus> statuses);

    // Calculate how many waiting customers joined strictly before this customer in the same queue
    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.queue = :queue AND e.status = 'WAITING' AND e.joinedAt < :joinedAt")
    long countWaitingBefore(@Param("queue") Queue queue, @Param("joinedAt") LocalDateTime joinedAt);

    // History queries by shop and date range
    @Query("SELECT e FROM QueueEntry e WHERE e.shop = :shop AND e.joinedAt BETWEEN :start AND :end ORDER BY e.joinedAt DESC")
    List<QueueEntry> findHistoryByShopAndDateRange(@Param("shop") Shop shop, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Total counts by shop and date range
    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.shop = :shop AND e.joinedAt BETWEEN :start AND :end")
    long countByShopAndDateRange(@Param("shop") Shop shop, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM QueueEntry e WHERE e.shop = :shop AND e.status = :status AND e.joinedAt BETWEEN :start AND :end")
    long countByShopAndStatusAndDateRange(@Param("shop") Shop shop, @Param("status") QueueEntryStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
