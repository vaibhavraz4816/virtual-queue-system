package com.queueease.util;

public final class TokenFormatter {

    private TokenFormatter() {
    }

    /**
     * Formats an integer sequence into a clean daily token format:
     * 1 -> A01, 2 -> A02 ... 27 -> A27, 99 -> A99, 100 -> A100.
     * If exceeding 999, advances prefix (B01, etc.)
     */
    public static String formatToken(int number) {
        if (number <= 0) {
            number = 1;
        }
        int letterIndex = (number - 1) / 99;
        char prefix = (char) ('A' + (letterIndex % 26));
        int num = ((number - 1) % 99) + 1;
        return String.format("%c%02d", prefix, num);
    }
}
