package com.deanwagman.lumenmarsh.venueops.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

final class DemoTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private DemoTokens() {
    }

    static String issue() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormatHolder.format(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static final class HexFormatHolder {
        private static final char[] HEX = "0123456789abcdef".toCharArray();

        private static String format(byte[] bytes) {
            char[] output = new char[bytes.length * 2];
            for (int index = 0; index < bytes.length; index++) {
                output[index * 2] = HEX[(bytes[index] >>> 4) & 0xf];
                output[index * 2 + 1] = HEX[bytes[index] & 0xf];
            }
            return new String(output);
        }
    }
}
