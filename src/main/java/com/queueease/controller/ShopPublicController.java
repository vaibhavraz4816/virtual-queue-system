package com.queueease.controller;

import com.queueease.dto.CustomerQueueStatusDto;
import com.queueease.dto.DashboardStatsDto;
import com.queueease.dto.JoinQueueRequest;
import java.util.Arrays;
import java.util.List;
import com.queueease.entity.QueueEntry;
import com.queueease.entity.Shop;
import com.queueease.service.QrCodeService;
import com.queueease.service.QueueService;
import com.queueease.service.ShopService;
import com.queueease.util.DateTimeUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
public class ShopPublicController {

    private final ShopService shopService;
    private final QueueService queueService;
    private final QrCodeService qrCodeService;

    public ShopPublicController(ShopService shopService,
                                QueueService queueService,
                                QrCodeService qrCodeService) {
        this.shopService = shopService;
        this.queueService = queueService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/{slug}/queue")
    public String shopQueuePage(@PathVariable("slug") String slug,
                                @CookieValue(value = "queue_access_token", required = false) String cookieToken,
                                @CookieValue(value = "q_tokens", required = false) String legacyCookieTokens,
                                jakarta.servlet.http.HttpServletResponse servletResponse,
                                Model model,
                                HttpSession session) {
        Shop shop = shopService.getShopBySlug(slug);
        DashboardStatsDto stats = queueService.getDashboardStats(shop.getId());

        // Always expire/clean up legacy broken q_tokens cookie if sent by browser
        if (legacyCookieTokens != null) {
            jakarta.servlet.http.Cookie deleteLegacy = new jakarta.servlet.http.Cookie("q_tokens", "");
            deleteLegacy.setPath("/");
            deleteLegacy.setMaxAge(0);
            servletResponse.addCookie(deleteLegacy);
        }

        CustomerQueueStatusDto existingTicket = null;

        // 1. Check single active token cookie
        if (cookieToken != null && !cookieToken.isBlank()) {
            try {
                CustomerQueueStatusDto status = queueService.getCustomerQueueStatus(cookieToken.trim());
                if (slug.equalsIgnoreCase(status.getShopSlug()) && isQueueActive(status.getStatus())) {
                    existingTicket = status;
                }
            } catch (Exception ignored) {
                // Token may be for another shop, expired, or invalid
            }
        }

        // 2. Fallback check legacy tokens safely without writing them back
        if (existingTicket == null && legacyCookieTokens != null && !legacyCookieTokens.isBlank()) {
            try {
                List<String> tokens = Arrays.stream(legacyCookieTokens.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .toList();
                List<CustomerQueueStatusDto> myQueues = queueService.getMyQueuesStatus(tokens);
                for (CustomerQueueStatusDto q : myQueues) {
                    if (slug.equalsIgnoreCase(q.getShopSlug()) && isQueueActive(q.getStatus())) {
                        existingTicket = q;
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        model.addAttribute("shop", shop);
        model.addAttribute("stats", stats);
        model.addAttribute("existingTicket", existingTicket);
        model.addAttribute("joinRequest", new JoinQueueRequest());
        model.addAttribute("queueUrl", qrCodeService.getShopQueueUrl(slug));
        model.addAttribute("openingFormatted", DateTimeUtils.formatTime(shop.getOpeningTime()));
        model.addAttribute("closingFormatted", DateTimeUtils.formatTime(shop.getClosingTime()));

        return "shop/public-page";
    }

    @PostMapping("/{slug}/join")
    public String handleJoinQueue(@PathVariable("slug") String slug,
                                  @Valid @ModelAttribute("joinRequest") JoinQueueRequest request,
                                  BindingResult result,
                                  HttpSession session,
                                  jakarta.servlet.http.HttpServletRequest servletRequest,
                                  jakarta.servlet.http.HttpServletResponse servletResponse,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid name.");
            return "redirect:/shop/" + slug + "/queue";
        }

        try {
            QueueEntry entry = queueService.joinQueue(slug, request, session.getId());

            // 1. Expire legacy broken q_tokens cookie if present
            jakarta.servlet.http.Cookie clearLegacy = new jakarta.servlet.http.Cookie("q_tokens", "");
            clearLegacy.setPath("/");
            clearLegacy.setMaxAge(0);
            servletResponse.addCookie(clearLegacy);

            // 2. Set single, cookie-safe, URL-safe queue_access_token (NO commas, NO lists)
            jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("queue_access_token", entry.getGuestAccessToken());
            accessCookie.setPath("/");
            accessCookie.setMaxAge(7 * 24 * 3600);
            accessCookie.setHttpOnly(false);
            servletResponse.addCookie(accessCookie);

            // 3. Customer is redirected directly to /my-queue
            return "redirect:/my-queue?token=" + entry.getGuestAccessToken();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/" + slug + "/queue";
        }
    }

    private boolean isQueueActive(String status) {
        if (status == null) return false;
        return !"SERVED".equalsIgnoreCase(status)
                && !"CANCELLED".equalsIgnoreCase(status)
                && !"SKIPPED".equalsIgnoreCase(status)
                && !"EXPIRED".equalsIgnoreCase(status);
    }
}
