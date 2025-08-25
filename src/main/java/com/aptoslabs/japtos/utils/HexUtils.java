package com.aptoslabs.japtos.utils;

/**
 * Utility class for hex string operations
 */
public class HexUtils {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * Convert bytes to hex string
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Convert hex string to bytes
     */
    public static byte[] hexToBytes(String hex) {
        // Remove 0x prefix if present
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex string");
            }
            data[i / 2] = (byte) ((high << 4) | low);
        }
        return data;
    }

    /**
     * Check if string is valid hex
     */
    public static boolean isValidHex(String hex) {
        if (hex == null) {
            return false;
        }

        // Remove 0x prefix if present
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        if (hex.length() % 2 != 0) {
            return false;
        }

        for (char c : hex.toCharArray()) {
            if (!Character.isDigit(c) && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }
} 