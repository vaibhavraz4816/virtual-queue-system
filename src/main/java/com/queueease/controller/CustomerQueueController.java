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
            @CookieValue(value = "q_tokens", required = false) String cookieTokens,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        Set<String> tokenSet = new LinkedHashSet<>();
        if (tokenParam != null && !tokenParam.isBlank()) {
            tokenSet.add(tokenParam.trim());
        }
        if (cookieTokens != null && !cookieTokens.isBlank()) {
            for (String t : cookieTokens.split(",")) {
                if (!t.isBlank()) {
                    tokenSet.add(t.trim());
                }
            }
        }

        if (!tokenSet.isEmpty()) {
            Cookie cookie = new Cookie("q_tokens", String.join(",", tokenSet));
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 3600);
            cookie.setHttpOnly(false);
            response.addCookie(cookie);
        }

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
