package com.queueease.service;

import com.queueease.dto.*;
import com.queueease.entity.Queue;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;

import java.util.List;
import java.util.Map;

public interface QueueService {

    Queue getOrCreateTodayQueue(Shop shop);

    QueueEntry joinQueue(String shopSlug, JoinQueueRequest request, String sessionId);

    QueueEntry addWalkInCustomer(Long shopId, WalkInRequest request);

    CustomerQueueStatusDto getCustomerQueueStatus(String accessToken);

    void markCustomerArrived(String accessToken);

    void cancelCustomer(String accessToken);

    QueueEntry callNextCustomer(Long shopId);

    void markCustomerServed(Long entryId, Long shopId);

    void skipCustomer(Long entryId, Long shopId);

    void cancelCustomerByOwner(Long entryId, Long shopId);

    void pauseQueue(Long shopId);

    void resumeQueue(Long shopId);

    List<OwnerQueueItemDto> getLiveQueueForOwner(Long shopId);

    DashboardStatsDto getDashboardStats(Long shopId);

    List<QueueEntry> getQueueHistory(Long shopId, String filter);

    QueueEntry finishAndNextCustomer(Long shopId);

    List<CustomerQueueStatusDto> getMyQueuesStatus(List<String> accessTokens);

    void expireCalledCustomers();
}
