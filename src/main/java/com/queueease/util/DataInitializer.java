package com.queueease.util;

import com.queueease.entity.Queue;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.entity.enums.ShopCategory;
import com.queueease.entity.enums.QueueEntryStatus;
import com.queueease.entity.enums.Role;
import com.queueease.entity.enums.ShopStatus;
import com.queueease.repository.QueueEntryRepository;
import com.queueease.repository.QueueRepository;
import com.queueease.repository.ShopRepository;
import com.queueease.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ShopRepository shopRepository,
                           QueueRepository queueRepository,
                           QueueEntryRepository queueEntryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.queueRepository = queueRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized with local accounts.");
            return;
        }

        log.info("Initializing QueueEase local development data...");

        // 1. Classic Cuts Salon
        User salonOwner = createUser("Marcus Vance", "salon@queueease.com", "password123");
        Shop salon = createShop(salonOwner, "Classic Cuts Salon", "classic-cuts",
                "124 High Street, Downtown", "+1 555-0192",
                "Premium grooming and hair styling. Walk-in queue with real-time tracking.",
                10, LocalTime.of(9, 0), LocalTime.of(20, 0), ShopCategory.SALON);

        // 2. QuickFix Mobile Repair
        User repairOwner = createUser("Alex Rivera", "repair@queueease.com", "password123");
        createShop(repairOwner, "QuickFix Mobile Repair", "quickfix",
                "45 Tech Plaza, 2nd Floor", "+1 555-0144",
                "Fast diagnosis and mobile screen repairs while you wait.",
                15, LocalTime.of(10, 0), LocalTime.of(19, 0), ShopCategory.REPAIR);

        // 3. CarePoint Clinic
        User clinicOwner = createUser("Dr. Sarah Jenkins", "clinic@queueease.com", "password123");
        createShop(clinicOwner, "CarePoint Clinic", "carepoint",
                "88 Health Ave, Suite 300", "+1 555-0177",
                "Outpatient general consultation and primary healthcare walk-ins.",
                12, LocalTime.of(8, 30), LocalTime.of(17, 30), ShopCategory.CLINIC);

        // 4. FreshBite Restaurant
        User diningOwner = createUser("Chef Mario Rossi", "freshbite@queueease.com", "password123");
        createShop(diningOwner, "FreshBite Restaurant", "freshbite",
                "12 Riverside Walk", "+1 555-0188",
                "Casual dining and table waiting queue. Scan to hold your spot.",
                20, LocalTime.of(11, 30), LocalTime.of(22, 0), ShopCategory.RESTAURANT);

        seedSalonQueue(salon);

        log.info("Local development seed data initialized successfully.");
    }

    private User createUser(String name, String email, String rawPassword) {
        User user = new User(name, email, passwordEncoder.encode(rawPassword), Role.ROLE_OWNER);
        return userRepository.save(user);
    }

    private Shop createShop(User owner, String name, String slug, String address, String phone,
                            String description, int avgWait, LocalTime open, LocalTime close, ShopCategory category) {
        Shop shop = new Shop(owner, name, slug, address, phone, description, avgWait);
        shop.setOpeningTime(open);
        shop.setClosingTime(close);
        shop.setStatus(ShopStatus.OPEN);
        shop.setCategory(category);
        return shopRepository.save(shop);
    }

    private void seedSalonQueue(Shop shop) {
        LocalDate today = LocalDate.now();
        Queue queue = new Queue(shop, today);
        queue.setCurrentTokenNumber(5);
        queue = queueRepository.save(queue);

        // Entry 1: Served earlier
        QueueEntry e1 = new QueueEntry(queue, shop, "A01", "David Miller", "+1 555-1101", UUID.randomUUID().toString().replace("-", ""));
        e1.setStatus(QueueEntryStatus.SERVED);
        e1.setJoinedAt(LocalDateTime.now().minusMinutes(45));
        e1.setCalledAt(LocalDateTime.now().minusMinutes(35));
        e1.setServedAt(LocalDateTime.now().minusMinutes(20));
        queueEntryRepository.save(e1);

        // Entry 2: Currently CALLED (with 90s grace remaining)
        QueueEntry e2 = new QueueEntry(queue, shop, "A02", "Emma Watson", "+1 555-1102", "demo-token-emma");
        e2.setStatus(QueueEntryStatus.CALLED);
        e2.setJoinedAt(LocalDateTime.now().minusMinutes(25));
        e2.setCalledAt(LocalDateTime.now().minusSeconds(30));
        e2.setCallExpiresAt(LocalDateTime.now().plusSeconds(90));
        queueEntryRepository.save(e2);

        // Entry 3: Waiting #1
        QueueEntry e3 = new QueueEntry(queue, shop, "A03", "Michael Brown", "+1 555-1103", "demo-token-michael");
        e3.setStatus(QueueEntryStatus.WAITING);
        e3.setJoinedAt(LocalDateTime.now().minusMinutes(15));
        queueEntryRepository.save(e3);

        // Entry 4: Waiting #2
        QueueEntry e4 = new QueueEntry(queue, shop, "A04", "Sarah Connor", "+1 555-1104", "demo-token-sarah");
        e4.setStatus(QueueEntryStatus.WAITING);
        e4.setJoinedAt(LocalDateTime.now().minusMinutes(10));
        queueEntryRepository.save(e4);

        // Entry 5: Waiting #3
        QueueEntry e5 = new QueueEntry(queue, shop, "A05", "James Wilson", "+1 555-1105", "demo-token-james");
        e5.setStatus(QueueEntryStatus.WAITING);
        e5.setJoinedAt(LocalDateTime.now().minusMinutes(5));
        queueEntryRepository.save(e5);
    }
}
