package com.queueease.controller;

import com.queueease.entity.QueueEntry;
import com.queueease.service.QueueService;
import com.queueease.util.TokenGenerator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CustomerQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

    @Test
    @DisplayName("1. Generated guest access token is RFC 6265 cookie-safe and URL-safe")
    void testGeneratedGuestAccessTokenIsCookieSafe() {
        for (int i = 0; i < 50; i++) {
            String token = TokenGenerator.generateSecureGuestToken();
            assertNotNull(token);
            assertEquals(32, token.length());
            // Safe characters: [A-Za-z0-9_-]
            assertTrue(token.matches("^[A-Za-z0-9_-]+$"), "Token must contain only URL and cookie safe characters");
            assertFalse(token.contains(","), "Token must NOT contain comma [44]");
            assertFalse(token.contains(";"), "Token must NOT contain semicolon");
            assertFalse(token.contains(" "), "Token must NOT contain space");
            assertFalse(token.contains("\""), "Token must NOT contain quotes");
        }
    }

    @Test
    @DisplayName("2. Joining queue does not throw IllegalArgumentException and sets cookie-safe queue_access_token")
    void testJoinQueueSucceedsWithSafeCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/shop/classic-cuts/join")
                        .with(csrf())
                        .param("customerName", "Cookie Test Customer")
                        .param("phone", "+1 555-0199"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/my-queue?token=*"))
                .andReturn();

        Cookie[] cookies = result.getResponse().getCookies();
        assertNotNull(cookies);

        boolean foundAccessTokenCookie = false;
        for (Cookie c : cookies) {
            assertFalse(c.getValue().contains(","),
                    "Cookie [" + c.getName() + "] value must not contain comma: " + c.getValue());
            if ("queue_access_token".equals(c.getName())) {
                foundAccessTokenCookie = true;
                assertTrue(c.getValue().matches("^[A-Za-z0-9_-]+$"));
                assertEquals("/", c.getPath());
            }
            if ("q_tokens".equals(c.getName())) {
                assertEquals(0, c.getMaxAge(), "Legacy q_tokens cookie must be expired (maxAge=0)");
            }
        }
        assertTrue(foundAccessTokenCookie, "Should set queue_access_token cookie");
    }

    @Test
    @DisplayName("3. Joining queue when browser already has legacy comma cookie safely cleans up without error")
    void testJoinQueueWithExistingLegacyCommaCookie() throws Exception {
        // Simulate browser sending old broken q_tokens cookie with comma (character 44)
        Cookie legacyCookie = new Cookie("q_tokens", "legacy-token-1,legacy-token-2");

        MvcResult result = mockMvc.perform(post("/shop/quickfix/join")
                        .with(csrf())
                        .cookie(legacyCookie)
                        .param("customerName", "Legacy Migration Customer")
                        .param("phone", "+1 555-0188"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/my-queue?token=*"))
                .andReturn();

        // Must NOT throw "An invalid character [44] was present in the Cookie value"
        Cookie[] cookies = result.getResponse().getCookies();
        for (Cookie c : cookies) {
            assertFalse(c.getValue().contains(","),
                    "Response cookie must never contain comma character [44]: " + c.getName() + "=" + c.getValue());
        }
    }

    @Test
    @DisplayName("4. Customer can access /my-queue and retrieve queue details after joining")
    void testCustomerCanAccessMyQueue() throws Exception {
        MvcResult joinResult = mockMvc.perform(post("/shop/carepoint/join")
                        .with(csrf())
                        .param("customerName", "Dr Visitor")
                        .param("phone", "+1 555-0177"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = joinResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl);
        String token = redirectUrl.substring(redirectUrl.indexOf("token=") + 6);

        // Load /my-queue with token query param
        MvcResult myQueueResult = mockMvc.perform(get("/my-queue").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-queue"))
                .andExpect(model().attributeExists("activeTickets"))
                .andReturn();

        // Verify response cookies don't have commas
        for (Cookie c : myQueueResult.getResponse().getCookies()) {
            assertFalse(c.getValue().contains(","), "Cookie must not contain comma: " + c.getValue());
        }
    }

    @Test
    @DisplayName("5. Multiple active queues do not produce comma-separated cookies")
    void testMultipleActiveQueuesDoNotCreateCommaCookie() throws Exception {
        // Customer joins Shop A
        MvcResult resA = mockMvc.perform(post("/shop/classic-cuts/join")
                        .with(csrf())
                        .param("customerName", "Multi Customer"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String urlA = resA.getResponse().getRedirectedUrl();
        String tokenA = urlA.substring(urlA.indexOf("token=") + 6);

        // Customer joins Shop B with tokenA as existing cookie
        Cookie cookieA = new Cookie("queue_access_token", tokenA);
        MvcResult resB = mockMvc.perform(post("/shop/freshbite/join")
                        .with(csrf())
                        .cookie(cookieA)
                        .param("customerName", "Multi Customer"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String urlB = resB.getResponse().getRedirectedUrl();
        String tokenB = urlB.substring(urlB.indexOf("token=") + 6);

        // Verify Shop B response cookie has ONLY the new single token, never "tokenA,tokenB"
        for (Cookie c : resB.getResponse().getCookies()) {
            assertFalse(c.getValue().contains(","), "Cookie must never contain commas: " + c.getValue());
            if ("queue_access_token".equals(c.getName())) {
                assertEquals(tokenB, c.getValue());
            }
        }

        // Verify multi-queue retrieval endpoint via JSON works for both tokens
        mockMvc.perform(post("/api/queue/my-active-tickets")
                        .contentType("application/json")
                        .content("[\"" + tokenA + "\", \"" + tokenB + "\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("6. Invalid or missing token is handled gracefully without error")
    void testInvalidOrMissingTokenHandledGracefully() throws Exception {
        // Missing token
        mockMvc.perform(get("/my-queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-queue"))
                .andExpect(model().attribute("activeTickets", List.of()));

        // Invalid token
        mockMvc.perform(get("/my-queue").param("token", "invalid_random_token_99999"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-queue"))
                .andExpect(model().attribute("activeTickets", List.of()));
    }
}
