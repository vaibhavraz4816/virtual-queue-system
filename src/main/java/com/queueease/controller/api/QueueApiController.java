package com.queueease.controller.api;

import com.queueease.dto.*;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.service.QueueService;
import com.queueease.service.ShopService;
import com.queueease.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueueApiController {

    private final QueueService queueService;
    private final ShopService shopService;
    private final UserService userService;

    public QueueApiController(QueueService queueService,
                              ShopService shopService,
                              UserService userService) {
        this.queueService = queueService;
        this.shopService = shopService;
        this.userService = userService;
    }

    private Shop getCurrentOwnerShop() {
        User owner = userService.getCurrentOwner();
        return shopService.getShopByOwner(owner);
    }

    // ==========================================
    // Customer Endpoints (Public)
    // ==========================================

    @GetMapping("/queue/{accessToken}/status")
    public ResponseEntity<ApiResponse<CustomerQueueStatusDto>> getCustomerQueueStatus(
            @PathVariable("accessToken") String accessToken) {
        CustomerQueueStatusDto status = queueService.getCustomerQueueStatus(accessToken);
        return ResponseEntity.ok(ApiResponse.ok("Queue status retrieved", status));
    }

    @PostMapping("/queue/{accessToken}/arrive")
    public ResponseEntity<ApiResponse<Void>> customerArrive(
            @PathVariable("accessToken") String accessToken) {
        queueService.markCustomerArrived(accessToken);
        return ResponseEntity.ok(ApiResponse.ok("Arrival confirmed! You are now being served."));
    }

    @PostMapping("/queue/{accessToken}/leave")
    public ResponseEntity<ApiResponse<Void>> customerLeave(
            @PathVariable("accessToken") String accessToken) {
        queueService.cancelCustomer(accessToken);
        return ResponseEntity.ok(ApiResponse.ok("You have left the queue."));
    }

    @PostMapping("/queue/my-active-tickets")
    public ResponseEntity<ApiResponse<List<CustomerQueueStatusDto>>> getMyActiveTickets(
            @RequestBody(required = false) List<String> accessTokens) {
        if (accessTokens == null || accessTokens.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No tokens provided", List.of()));
        }
        List<CustomerQueueStatusDto> activeStatuses = queueService.getMyQueuesStatus(accessTokens);
        return ResponseEntity.ok(ApiResponse.ok("Active tickets retrieved", activeStatuses));
    }

    @PostMapping("/queue/{shopSlug}/join")
    public ResponseEntity<ApiResponse<Map<String, String>>> customerJoin(
            @PathVariable("shopSlug") String shopSlug,
            @Valid @RequestBody JoinQueueRequest request,
            HttpSession session) {
        QueueEntry entry = queueService.joinQueue(shopSlug, request, session.getId());
        Map<String, String> data = Map.of(
                "token", entry.getTokenNumber(),
                "accessToken", entry.getGuestAccessToken(),
                "redirectUrl", "/my-queue?token=" + entry.getGuestAccessToken()
        );
        return ResponseEntity.ok(ApiResponse.ok("Joined queue successfully!", data));
    }

    // ==========================================
    // Owner Endpoints (Authenticated)
    // ==========================================

    @GetMapping("/shop/queue/live")
    public ResponseEntity<ApiResponse<List<OwnerQueueItemDto>>> getLiveQueue() {
        Shop shop = getCurrentOwnerShop();
        List<OwnerQueueItemDto> queue = queueService.getLiveQueueForOwner(shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Live queue retrieved", queue));
    }

    @GetMapping("/shop/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        Shop shop = getCurrentOwnerShop();
        DashboardStatsDto stats = queueService.getDashboardStats(shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats retrieved", stats));
    }

    @PostMapping("/shop/queue/finish-and-next")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finishAndNext() {
        Shop shop = getCurrentOwnerShop();
        QueueEntry nextEntry = queueService.finishAndNextCustomer(shop.getId());
        if (nextEntry != null) {
            Map<String, Object> data = Map.of(
                    "token", nextEntry.getTokenNumber(),
                    "customerName", nextEntry.getCustomerName(),
                    "entryId", nextEntry.getId()
            );
            return ResponseEntity.ok(ApiResponse.ok("Customer served! Called next customer " + nextEntry.getTokenNumber(), data));
        } else {
            return ResponseEntity.ok(ApiResponse.ok("Customer served! Queue is now empty."));
        }
    }

    @PostMapping("/shop/queue/next")
    public ResponseEntity<ApiResponse<Map<String, Object>>> callNext() {
        Shop shop = getCurrentOwnerShop();
        QueueEntry entry = queueService.callNextCustomer(shop.getId());
        Map<String, Object> data = Map.of(
                "token", entry.getTokenNumber(),
                "customerName", entry.getCustomerName(),
                "entryId", entry.getId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Called customer " + entry.getTokenNumber(), data));
    }

    @PostMapping("/shop/queue/{entryId}/serve")
    public ResponseEntity<ApiResponse<Void>> markServed(@PathVariable("entryId") Long entryId) {
        Shop shop = getCurrentOwnerShop();
        queueService.markCustomerServed(entryId, shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Customer marked as served."));
    }

    @PostMapping("/shop/queue/{entryId}/skip")
    public ResponseEntity<ApiResponse<Void>> skipCustomer(@PathVariable("entryId") Long entryId) {
        Shop shop = getCurrentOwnerShop();
        queueService.skipCustomer(entryId, shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Customer skipped."));
    }

    @PostMapping("/shop/queue/{entryId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCustomer(@PathVariable("entryId") Long entryId) {
        Shop shop = getCurrentOwnerShop();
        queueService.cancelCustomerByOwner(entryId, shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Queue entry cancelled."));
    }

    @PostMapping("/shop/queue/pause")
    public ResponseEntity<ApiResponse<Void>> pauseQueue() {
        Shop shop = getCurrentOwnerShop();
        queueService.pauseQueue(shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Queue is now paused."));
    }

    @PostMapping("/shop/queue/resume")
    public ResponseEntity<ApiResponse<Void>> resumeQueue() {
        Shop shop = getCurrentOwnerShop();
        queueService.resumeQueue(shop.getId());
        return ResponseEntity.ok(ApiResponse.ok("Queue resumed and open."));
    }

    @PostMapping("/shop/queue/walk-in")
    public ResponseEntity<ApiResponse<Map<String, String>>> addWalkIn(
            @Valid @RequestBody WalkInRequest request) {
        Shop shop = getCurrentOwnerShop();
        QueueEntry entry = queueService.addWalkInCustomer(shop.getId(), request);
        Map<String, String> data = Map.of(
                "token", entry.getTokenNumber(),
                "customerName", entry.getCustomerName()
        );
        return ResponseEntity.ok(ApiResponse.ok("Walk-in token generated: " + entry.getTokenNumber(), data));
    }
}
