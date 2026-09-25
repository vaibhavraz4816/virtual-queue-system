package com.queueease.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueease.dto.CustomerQueueStatusDto;
import com.queueease.service.QueueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class CustomerQueueController {

    private final QueueService queueService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomerQueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/my-queue")
    public String myQueue(
            @RequestParam(value = "token", required = false) String tokenParam,
            @CookieValue(value = "queue_access_token", required = false) String cookieToken,
            @CookieValue(value = "q_tokens", required = false) String legacyTokens,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        // 1. Expire legacy broken q_tokens cookie if sent by browser
        if (legacyTokens != null) {
            Cookie clearLegacy = new Cookie("q_tokens", "");
            clearLegacy.setPath("/");
            clearLegacy.setMaxAge(0);
            response.addCookie(clearLegacy);
        }

        // 2. Gather tokens for initial SSR render
        Set<String> tokenSet = new LinkedHashSet<>();
        if (tokenParam != null && !tokenParam.isBlank()) {
            tokenSet.add(tokenParam.trim());
        }
        if (cookieToken != null && !cookieToken.isBlank()) {
            tokenSet.add(cookieToken.trim());
        }
        if (legacyTokens != null && !legacyTokens.isBlank()) {
            for (String t : legacyTokens.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    tokenSet.add(trimmed);
                }
            }
        }

        // 3. Set ONLY a single URL-safe cookie for the active token (NO commas, NO lists)
        String activeToken = (tokenParam != null && !tokenParam.isBlank())
                ? tokenParam.trim()
                : (cookieToken != null && !cookieToken.isBlank()
                    ? cookieToken.trim()
                    : (!tokenSet.isEmpty() ? tokenSet.iterator().next() : null));

        if (activeToken != null) {
            Cookie safeCookie = new Cookie("queue_access_token", activeToken);
            safeCookie.setPath("/");
            safeCookie.setMaxAge(7 * 24 * 3600);
            safeCookie.setHttpOnly(false);
            response.addCookie(safeCookie);
        }

        // 4. Fetch status for SSR
        List<CustomerQueueStatusDto> activeTickets = new ArrayList<>();
        if (!tokenSet.isEmpty()) {
            activeTickets = queueService.getMyQueuesStatus(new ArrayList<>(tokenSet));
        }

        model.addAttribute("activeTickets", activeTickets);
        model.addAttribute("primaryTicket", activeTickets.isEmpty() ? null : activeTickets.get(0));
        try {
            model.addAttribute("initialTokensJson", objectMapper.writeValueAsString(tokenSet));
        } catch (Exception e) {
            model.addAttribute("initialTokensJson", "[]");
        }

        return "customer/my-queue";
    }

    @GetMapping("/queue/{accessToken}")
    public String viewQueueStatus(@PathVariable("accessToken") String accessToken) {
        return "redirect:/my-queue?token=" + accessToken;
    }
}
