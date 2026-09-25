package com.queueease.service.impl;

import com.queueease.dto.*;
import com.queueease.entity.Queue;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.enums.QueueEntryStatus;
import com.queueease.entity.enums.QueueStatus;
import com.queueease.entity.enums.ShopStatus;
import com.queueease.exception.DuplicateQueueEntryException;
import com.queueease.exception.InvalidQueueStateException;
import com.queueease.exception.ResourceNotFoundException;
import com.queueease.repository.QueueEntryRepository;
import com.queueease.repository.QueueRepository;
import com.queueease.repository.ShopRepository;
import com.queueease.service.QueueService;
import com.queueease.util.DateTimeUtils;
import com.queueease.util.TokenFormatter;
import com.queueease.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueueServiceImpl implements QueueService {

    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ShopRepository shopRepository;

    @Value("${queueease.call-grace-period-seconds:120}")
    private long callGracePeriodSeconds;

    public QueueServiceImpl(QueueRepository queueRepository,
                            QueueEntryRepository queueEntryRepository,
                            ShopRepository shopRepository) {
        this.queueRepository = queueRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.shopRepository = shopRepository;
    }

    @Override
    @Transactional
    public Queue getOrCreateTodayQueue(Shop shop) {
        LocalDate today = LocalDate.now();
        return queueRepository.findByShopAndQueueDate(shop, today)
                .orElseGet(() -> {
                    Queue newQueue = new Queue(shop, today);
                    return queueRepository.save(newQueue);
                });
    }

    @Override
    @Transactional
    public QueueEntry joinQueue(String shopSlug, JoinQueueRequest request, String sessionId) {
        Shop shop = shopRepository.findBySlug(shopSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found for slug: " + shopSlug));

        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new InvalidQueueStateException("This shop is currently closed.");
        }
        if (shop.getStatus() == ShopStatus.QUEUE_PAUSED) {
            throw new InvalidQueueStateException("The queue is temporarily paused. Please wait for the shop to resume it.");
        }

        // Prevent duplicate: if customer already has active ticket in this shop for this session, return it
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<QueueEntry> existing = queueEntryRepository.findFirstByShopAndSessionIdAndStatusInOrderByJoinedAtDesc(
                    shop, sessionId, List.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.SERVING)
            );
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Queue queue = getOrCreateTodayQueue(shop);

        // Increment sequence token
        int nextTokenNum = queue.getCurrentTokenNumber() + 1;
        queue.setCurrentTokenNumber(nextTokenNum);
        queueRepository.save(queue);

        String token = TokenFormatter.formatToken(nextTokenNum);
        String guestToken = TokenGenerator.generateSecureGuestToken();

        QueueEntry entry = new QueueEntry(queue, shop, token, request.getCustomerName().trim(), request.getPhone(), guestToken);
        entry.setSessionId(sessionId);
        entry.setStatus(QueueEntryStatus.WAITING);
        entry.setWalkIn(false);

        return queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public QueueEntry addWalkInCustomer(Long shopId, WalkInRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        Queue queue = getOrCreateTodayQueue(shop);

        int nextTokenNum = queue.getCurrentTokenNumber() + 1;
        queue.setCurrentTokenNumber(nextTokenNum);
        queueRepository.save(queue);

        String token = TokenFormatter.formatToken(nextTokenNum);
        String guestToken = TokenGenerator.generateSecureGuestToken();

        QueueEntry entry = new QueueEntry(queue, shop, token, request.getCustomerName().trim(), request.getPhone(), guestToken);
        entry.setStatus(QueueEntryStatus.WAITING);
        entry.setWalkIn(true);

        return queueEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerQueueStatusDto getCustomerQueueStatus(String accessToken) {
        QueueEntry entry = queueEntryRepository.findByGuestAccessToken(accessToken)
                .orElseThrow(() -> new ResourceNotFoundException("Queue ticket not found or invalid token"));

        Shop shop = entry.getShop();
        Queue queue = entry.getQueue();

        CustomerQueueStatusDto dto = new CustomerQueueStatusDto();
        dto.setToken(entry.getTokenNumber());
        dto.setGuestAccessToken(entry.getGuestAccessToken());
        dto.setStatus(entry.getStatus().name());
        dto.setStatusDisplayName(entry.getStatus().getDisplayName());
        dto.setShopName(shop.getName());
        dto.setShopSlug(shop.getSlug());
        dto.setShopCategory(shop.getCategory() != null ? shop.getCategory().getDisplayName() : "Service");
        dto.setShopAddress(shop.getAddress());
        dto.setShopPhone(shop.getPhone());
        dto.setJoinedTimeFormatted(DateTimeUtils.formatTime(entry.getJoinedAt()));
        dto.setShopStatus(shop.getStatus().name());
        dto.setShopOpen(shop.getStatus() == ShopStatus.OPEN);
        dto.setCustomerName(entry.getCustomerName());
        dto.setCalledAt(entry.getCalledAt());
        dto.setCallExpiresAt(entry.getCallExpiresAt());

        // Human-friendly status text
        switch (entry.getStatus()) {
            case WAITING:
                dto.setHumanStatusMessage("You're in the queue");
                break;
            case CALLED:
                dto.setHumanStatusMessage("It's your turn");
                break;
            case SERVING:
                dto.setHumanStatusMessage("You're being served");
                break;
            case SERVED:
                dto.setHumanStatusMessage("Service complete");
                break;
            case SKIPPED:
            case EXPIRED:
                dto.setHumanStatusMessage("Your turn was missed");
                break;
            case CANCELLED:
                dto.setHumanStatusMessage("Queue entry cancelled");
                break;
            default:
                dto.setHumanStatusMessage(entry.getStatus().getDisplayName());
        }

        // Currently serving customer
        Optional<QueueEntry> currentActive = queueEntryRepository.findFirstByQueueAndStatusInOrderByCalledAtDesc(
                queue, List.of(QueueEntryStatus.SERVING, QueueEntryStatus.CALLED)
        );
        dto.setCurrentServing(currentActive.map(QueueEntry::getTokenNumber).orElse("None"));

        // Total active tickets in queue
        long totalActive = queueEntryRepository.countByQueueAndStatusIn(
                queue, List.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.SERVING)
        );
        dto.setQueueSize((int) totalActive);

        // People ahead & position calculation
        if (entry.getStatus() == QueueEntryStatus.WAITING) {
            long waitingBefore = queueEntryRepository.countWaitingBefore(queue, entry.getJoinedAt());
            int ahead = (int) waitingBefore;
            if (currentActive.isPresent() && !currentActive.get().getId().equals(entry.getId())) {
                ahead += 1;
            }
            dto.setPeopleAhead(ahead);
            dto.setPosition(ahead + 1);
            dto.setEstimatedWait(ahead * shop.getAverageServiceMinutes());
        } else if (entry.getStatus() == QueueEntryStatus.CALLED || entry.getStatus() == QueueEntryStatus.SERVING) {
            dto.setPeopleAhead(0);
            dto.setPosition(1);
            dto.setEstimatedWait(0);
        } else {
            dto.setPeopleAhead(0);
            dto.setPosition(0);
            dto.setEstimatedWait(0);
        }

        // Seconds remaining for CALLED status (2-min grace period)
        if (entry.getStatus() == QueueEntryStatus.CALLED && entry.getCallExpiresAt() != null) {
            long remaining = Duration.between(LocalDateTime.now(), entry.getCallExpiresAt()).getSeconds();
            dto.setSecondsRemaining(Math.max(0, remaining));
        } else {
            dto.setSecondsRemaining(0);
        }

        dto.setCanArrive(entry.getStatus() == QueueEntryStatus.CALLED);
        dto.setCanLeave(entry.getStatus() == QueueEntryStatus.WAITING || entry.getStatus() == QueueEntryStatus.CALLED);

        return dto;
    }

    @Override
    @Transactional
    public void markCustomerArrived(String accessToken) {
        QueueEntry entry = queueEntryRepository.findByGuestAccessToken(accessToken)
                .orElseThrow(() -> new ResourceNotFoundException("Queue ticket not found"));

        if (entry.getStatus() != QueueEntryStatus.CALLED) {
            throw new InvalidQueueStateException("Ticket is not in CALLED status. Current status: " + entry.getStatus());
        }

        if (entry.getCallExpiresAt() != null && LocalDateTime.now().isAfter(entry.getCallExpiresAt())) {
            entry.setStatus(QueueEntryStatus.SKIPPED);
            queueEntryRepository.save(entry);
            throw new InvalidQueueStateException("Response time has expired. Please contact shop staff.");
        }

        entry.setStatus(QueueEntryStatus.SERVING);
        queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public void cancelCustomer(String accessToken) {
        QueueEntry entry = queueEntryRepository.findByGuestAccessToken(accessToken)
                .orElseThrow(() -> new ResourceNotFoundException("Queue ticket not found"));

        if (entry.getStatus() != QueueEntryStatus.WAITING && entry.getStatus() != QueueEntryStatus.CALLED) {
            throw new InvalidQueueStateException("Only waiting or called tickets can be cancelled.");
        }

        entry.setStatus(QueueEntryStatus.CANCELLED);
        entry.setCancelledAt(LocalDateTime.now());
        queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public QueueEntry callNextCustomer(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() == ShopStatus.QUEUE_PAUSED) {
            throw new InvalidQueueStateException("Queue is paused. Please resume before calling next.");
        }

        Queue queue = getOrCreateTodayQueue(shop);

        // Check if there is an active SERVING customer
        long activeServing = queueEntryRepository.countByQueueAndStatus(queue, QueueEntryStatus.SERVING);
        if (activeServing > 0) {
            throw new InvalidQueueStateException("Another customer is currently being served. Please mark them served or skipped before calling next.");
        }

        // Check if there is an active CALLED customer whose time hasn't expired yet
        Optional<QueueEntry> activeCalled = queueEntryRepository.findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.CALLED);
        if (activeCalled.isPresent()) {
            QueueEntry called = activeCalled.get();
            if (called.getCallExpiresAt() != null && LocalDateTime.now().isBefore(called.getCallExpiresAt())) {
                throw new InvalidQueueStateException("Customer " + called.getTokenNumber() + " is currently called and has time remaining to respond.");
            } else {
                // Auto-skip expired called customer
                called.setStatus(QueueEntryStatus.SKIPPED);
                queueEntryRepository.save(called);
            }
        }

        // Find earliest WAITING customer (FIFO)
        QueueEntry next = queueEntryRepository.findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.WAITING)
                .orElseThrow(() -> new InvalidQueueStateException("No waiting customers in the queue."));

        next.setStatus(QueueEntryStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());
        next.setCallExpiresAt(LocalDateTime.now().plusSeconds(callGracePeriodSeconds));

        return queueEntryRepository.save(next);
    }

    @Override
    @Transactional
    public QueueEntry finishAndNextCustomer(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() == ShopStatus.QUEUE_PAUSED) {
            throw new InvalidQueueStateException("Queue is paused. Please resume before advancing queue.");
        }

        Queue queue = getOrCreateTodayQueue(shop);

        // 1. Mark current SERVING / ARRIVED / CALLED customer as SERVED
        Optional<QueueEntry> currentServing = queueEntryRepository
                .findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.SERVING);
        if (currentServing.isPresent()) {
            QueueEntry serving = currentServing.get();
            serving.setStatus(QueueEntryStatus.SERVED);
            serving.setServedAt(LocalDateTime.now());
            queueEntryRepository.save(serving);
        } else {
            Optional<QueueEntry> currentArrived = queueEntryRepository
                    .findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.ARRIVED);
            if (currentArrived.isPresent()) {
                QueueEntry arrived = currentArrived.get();
                arrived.setStatus(QueueEntryStatus.SERVED);
                arrived.setServedAt(LocalDateTime.now());
                queueEntryRepository.save(arrived);
            } else {
                Optional<QueueEntry> currentCalled = queueEntryRepository
                        .findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.CALLED);
                if (currentCalled.isPresent()) {
                    QueueEntry called = currentCalled.get();
                    called.setStatus(QueueEntryStatus.SERVED);
                    called.setServedAt(LocalDateTime.now());
                    queueEntryRepository.save(called);
                }
            }
        }

        // 2. Automatically select next WAITING customer and transition to CALLED
        Optional<QueueEntry> nextWaiting = queueEntryRepository
                .findFirstByQueueAndStatusOrderByJoinedAtAsc(queue, QueueEntryStatus.WAITING);

        if (nextWaiting.isPresent()) {
            QueueEntry next = nextWaiting.get();
            next.setStatus(QueueEntryStatus.CALLED);
            next.setCalledAt(LocalDateTime.now());
            next.setCallExpiresAt(LocalDateTime.now().plusSeconds(callGracePeriodSeconds));
            return queueEntryRepository.save(next);
        }

        return null;
    }

    @Override
    @Transactional
    public void markCustomerServed(Long entryId, Long shopId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + entryId));

        if (!entry.getShop().getId().equals(shopId)) {
            throw new InvalidQueueStateException("Unauthorized: Entry does not belong to this shop");
        }

        if (entry.getStatus() != QueueEntryStatus.SERVING && entry.getStatus() != QueueEntryStatus.CALLED) {
            throw new InvalidQueueStateException("Only serving or called customers can be marked as served.");
        }

        entry.setStatus(QueueEntryStatus.SERVED);
        entry.setServedAt(LocalDateTime.now());
        queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public void skipCustomer(Long entryId, Long shopId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + entryId));

        if (!entry.getShop().getId().equals(shopId)) {
            throw new InvalidQueueStateException("Unauthorized: Entry does not belong to this shop");
        }

        if (entry.getStatus() == QueueEntryStatus.SERVED || entry.getStatus() == QueueEntryStatus.CANCELLED) {
            throw new InvalidQueueStateException("Cannot skip already completed or cancelled entry.");
        }

        entry.setStatus(QueueEntryStatus.SKIPPED);
        queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public void cancelCustomerByOwner(Long entryId, Long shopId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + entryId));

        if (!entry.getShop().getId().equals(shopId)) {
            throw new InvalidQueueStateException("Unauthorized: Entry does not belong to this shop");
        }

        entry.setStatus(QueueEntryStatus.CANCELLED);
        entry.setCancelledAt(LocalDateTime.now());
        queueEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public void pauseQueue(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        shop.setStatus(ShopStatus.QUEUE_PAUSED);
        shopRepository.save(shop);
    }

    @Override
    @Transactional
    public void resumeQueue(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        shop.setStatus(ShopStatus.OPEN);
        shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerQueueItemDto> getLiveQueueForOwner(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        Queue queue = getOrCreateTodayQueue(shop);

        List<QueueEntry> activeEntries = queueEntryRepository.findByQueueAndStatusInOrderByJoinedAtAsc(
                queue, List.of(QueueEntryStatus.CALLED, QueueEntryStatus.SERVING, QueueEntryStatus.WAITING)
        );

        Optional<QueueEntry> currentActive = queueEntryRepository.findFirstByQueueAndStatusInOrderByCalledAtDesc(
                queue, List.of(QueueEntryStatus.SERVING, QueueEntryStatus.CALLED)
        );

        List<OwnerQueueItemDto> dtoList = new ArrayList<>();
        for (QueueEntry entry : activeEntries) {
            OwnerQueueItemDto item = new OwnerQueueItemDto();
            item.setId(entry.getId());
            item.setTokenNumber(entry.getTokenNumber());
            item.setCustomerName(entry.getCustomerName());
            item.setPhone(entry.getPhone() != null ? entry.getPhone() : "--");
            item.setJoinedTime(DateTimeUtils.formatTime(entry.getJoinedAt()));
            item.setStatus(entry.getStatus().name());
            item.setStatusDisplayName(entry.getStatus().getDisplayName());
            item.setWalkIn(entry.isWalkIn());

            if (entry.getStatus() == QueueEntryStatus.WAITING) {
                long waitingBefore = queueEntryRepository.countWaitingBefore(queue, entry.getJoinedAt());
                int ahead = (int) waitingBefore;
                if (currentActive.isPresent() && !currentActive.get().getId().equals(entry.getId())) {
                    ahead += 1;
                }
                item.setPeopleAhead(ahead);
                item.setEstimatedWaitMinutes(ahead * shop.getAverageServiceMinutes());
                item.setCanCall(true);
                item.setCanServe(false);
                item.setCanSkip(true);
                item.setCanCancel(true);
            } else if (entry.getStatus() == QueueEntryStatus.CALLED) {
                item.setPeopleAhead(0);
                item.setEstimatedWaitMinutes(0);
                item.setCanCall(false);
                item.setCanServe(true);
                item.setCanSkip(true);
                item.setCanCancel(true);
                if (entry.getCallExpiresAt() != null) {
                    long remaining = Duration.between(LocalDateTime.now(), entry.getCallExpiresAt()).getSeconds();
                    item.setSecondsRemaining(Math.max(0, remaining));
                }
            } else if (entry.getStatus() == QueueEntryStatus.SERVING) {
                item.setPeopleAhead(0);
                item.setEstimatedWaitMinutes(0);
                item.setCanCall(false);
                item.setCanServe(true);
                item.setCanSkip(true);
                item.setCanCancel(false);
            }
            dtoList.add(item);
        }

        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        Queue queue = getOrCreateTodayQueue(shop);

        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setShopStatus(shop.getStatus());
        stats.setPaused(shop.getStatus() == ShopStatus.QUEUE_PAUSED);
        stats.setClosed(shop.getStatus() == ShopStatus.CLOSED);

        long waiting = queueEntryRepository.countByQueueAndStatus(queue, QueueEntryStatus.WAITING);
        stats.setCustomersWaiting(waiting);
        stats.setAverageWaitMinutes((int) (waiting * shop.getAverageServiceMinutes()));

        stats.setShopName(shop.getName());
        stats.setCategoryName(shop.getCategory() != null ? shop.getCategory().getDisplayName() : "Service");

        // Currently serving or called customer
        Optional<QueueEntry> activeCurrent = queueEntryRepository.findFirstByQueueAndStatusInOrderByCalledAtDesc(
                queue, List.of(QueueEntryStatus.SERVING, QueueEntryStatus.CALLED)
        );
        if (activeCurrent.isPresent()) {
            QueueEntry current = activeCurrent.get();
            stats.setCurrentServingToken(current.getTokenNumber());
            stats.setCurrentCustomerName(current.getCustomerName());
            stats.setCurrentEntryId(current.getId());
            stats.setCurrentStatus(current.getStatus().name());
            stats.setServiceStartTime(DateTimeUtils.formatTime(current.getCalledAt() != null ? current.getCalledAt() : current.getJoinedAt()));
            if (current.getStatus() == QueueEntryStatus.CALLED && current.getCallExpiresAt() != null) {
                long rem = Duration.between(LocalDateTime.now(), current.getCallExpiresAt()).getSeconds();
                stats.setCurrentSecondsRemaining(Math.max(0, rem));
            } else {
                stats.setCurrentSecondsRemaining(0);
            }
        } else {
            stats.setCurrentServingToken("None");
            stats.setCurrentCustomerName("--");
            stats.setCurrentStatus("IDLE");
            stats.setServiceStartTime("--:--");
            stats.setCurrentSecondsRemaining(0);
        }

        // Today stats
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        stats.setServedToday(queueEntryRepository.countByShopAndStatusAndDateRange(shop, QueueEntryStatus.SERVED, startOfDay, endOfDay));
        stats.setSkippedToday(queueEntryRepository.countByShopAndStatusAndDateRange(shop, QueueEntryStatus.SKIPPED, startOfDay, endOfDay));
        stats.setTotalToday(queueEntryRepository.countByShopAndDateRange(shop, startOfDay, endOfDay));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerQueueStatusDto> getMyQueuesStatus(List<String> accessTokens) {
        if (accessTokens == null || accessTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<QueueEntry> entries = queueEntryRepository.findByGuestAccessTokenIn(accessTokens);
        List<CustomerQueueStatusDto> dtos = new ArrayList<>();
        for (QueueEntry entry : entries) {
            try {
                dtos.add(getCustomerQueueStatus(entry.getGuestAccessToken()));
            } catch (Exception ignored) {
            }
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntry> getQueueHistory(Long shopId, String filter) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if ("yesterday".equalsIgnoreCase(filter)) {
            start = LocalDate.now().minusDays(1).atStartOfDay();
            end = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
        } else if ("last7days".equalsIgnoreCase(filter)) {
            start = LocalDate.now().minusDays(7).atStartOfDay();
        } else {
            // default "today"
            start = LocalDate.now().atStartOfDay();
            end = LocalDate.now().atTime(LocalTime.MAX);
        }

        return queueEntryRepository.findHistoryByShopAndDateRange(shop, start, end);
    }

    /**
     * Periodic scheduled task to auto-expire CALLED customers who failed to arrive within 2 minutes.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    @Override
    public void expireCalledCustomers() {
        LocalDateTime now = LocalDateTime.now();
        List<QueueEntry> expiredEntries = queueEntryRepository.findByStatusAndCallExpiresAtBefore(QueueEntryStatus.CALLED, now);
        for (QueueEntry entry : expiredEntries) {
            entry.setStatus(QueueEntryStatus.SKIPPED);
            queueEntryRepository.save(entry);
        }
    }
}
