package com.queueease;

import com.queueease.dto.CustomerQueueStatusDto;
import com.queueease.dto.JoinQueueRequest;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.enums.QueueEntryStatus;
import com.queueease.service.QrCodeService;
import com.queueease.service.QueueService;
import com.queueease.service.ShopService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
class QueueEaseApplicationTests {

    @Autowired
    private ShopService shopService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private QrCodeService qrCodeService;

    @Test
    @DisplayName("Context Loads and Demo Shops are initialized")
    void contextLoads() {
        Shop salon = shopService.getShopBySlug("classic-cuts");
        assertNotNull(salon);
        assertEquals("Classic Cuts Salon", salon.getName());
    }

    @Test
    @DisplayName("Customer can join queue, receive token, and track live status")
    void testJoinQueueAndStatus() {
        JoinQueueRequest req = new JoinQueueRequest("Integration Test Customer", "+1 555-9999");
        QueueEntry entry = queueService.joinQueue("classic-cuts", req, "test-session-unique-123");

        assertNotNull(entry);
        assertNotNull(entry.getTokenNumber());
        assertNotNull(entry.getGuestAccessToken());
        assertEquals(QueueEntryStatus.WAITING, entry.getStatus());

        CustomerQueueStatusDto status = queueService.getCustomerQueueStatus(entry.getGuestAccessToken());
        assertNotNull(status);
        assertEquals(entry.getTokenNumber(), status.getToken());
        assertTrue(status.getPosition() >= 1);
    }

    @Test
    @DisplayName("QR Code generates valid PNG byte array")
    void testQrCodeGeneration() {
        byte[] qr = qrCodeService.generateQrCodeImage("http://localhost:8080/shop/classic-cuts/queue", 250, 250);
        assertNotNull(qr);
        assertTrue(qr.length > 100);
        // Verify PNG magic header bytes (0x89 0x50 0x4E 0x47)
        assertEquals((byte) 0x89, qr[0]);
        assertEquals((byte) 0x50, qr[1]);
        assertEquals((byte) 0x4E, qr[2]);
        assertEquals((byte) 0x47, qr[3]);
    }
}
