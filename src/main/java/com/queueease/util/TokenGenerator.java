package com.queueease.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically secure, URL-safe, and cookie-safe tokens.
 * Values contain only characters in [A-Za-z0-9_-], with no padding, commas,
 * semicolons, spaces, quotes, or JSON characters.
 */
public final class TokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {
        // Utility class
    }

    /**
     * Generates a 32-character cryptographically secure token (192 bits of entropy).
     * Guaranteed safe for RFC 6265 cookies and URLs.
     */
    public static String generateSecureGuestToken() {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        return URL_ENCODER.encodeToString(randomBytes);
    }
}
