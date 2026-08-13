package com.queueapp.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Wraps BCrypt so shop passwords are never stored or compared in plain text.
 */
public final class PasswordUtil {

    private static final int WORKLOAD = 12;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORKLOAD));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
