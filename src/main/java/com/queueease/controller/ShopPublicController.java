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
                                @CookieValue(value = "q_tokens", required = false) String cookieTokens,
                                Model model,
                                HttpSession session) {
        Shop shop = shopService.getShopBySlug(slug);
        DashboardStatsDto stats = queueService.getDashboardStats(shop.getId());

        CustomerQueueStatusDto existingTicket = null;
        if (cookieTokens != null && !cookieTokens.isBlank()) {
            List<String> tokens = Arrays.asList(cookieTokens.split(","));
            List<CustomerQueueStatusDto> myQueues = queueService.getMyQueuesStatus(tokens);
            for (CustomerQueueStatusDto q : myQueues) {
                if (slug.equalsIgnoreCase(q.getShopSlug()) && !"SERVED".equalsIgnoreCase(q.getStatus())
                        && !"CANCELLED".equalsIgnoreCase(q.getStatus()) && !"SKIPPED".equalsIgnoreCase(q.getStatus())) {
                    existingTicket = q;
                    break;
                }
            }
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

            // Sync cookie
            String existingCookie = null;
            if (servletRequest.getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : servletRequest.getCookies()) {
                    if ("q_tokens".equals(c.getName())) {
                        existingCookie = c.getValue();
                        break;
                    }
                }
            }
            java.util.Set<String> tokenSet = new java.util.LinkedHashSet<>();
            tokenSet.add(entry.getGuestAccessToken());
            if (existingCookie != null && !existingCookie.isBlank()) {
                for (String t : existingCookie.split(",")) {
                    if (!t.isBlank()) tokenSet.add(t.trim());
                }
            }
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("q_tokens", String.join(",", tokenSet));
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 3600);
            cookie.setHttpOnly(false);
            servletResponse.addCookie(cookie);

            return "redirect:/my-queue?token=" + entry.getGuestAccessToken();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/" + slug + "/queue";
        }
    }
}
