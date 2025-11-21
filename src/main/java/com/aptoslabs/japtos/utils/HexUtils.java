package com.aptoslabs.japtos.utils;

/**
 * Utility class for hexadecimal string encoding and decoding operations.
 *
 * <p>This class provides methods to convert between byte arrays and hexadecimal
 * string representations, which are commonly used in blockchain and cryptographic
 * operations. All methods handle the optional "0x" prefix that is often used
 * in blockchain addresses and transaction hashes.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Convert byte arrays to lowercase hex strings</li>
 *   <li>Convert hex strings (with or without "0x" prefix) to byte arrays</li>
 *   <li>Validate hex string format</li>
 *   <li>Automatic handling of "0x" prefix</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Convert bytes to hex
 * byte[] data = {0x01, 0x23, 0x45, 0x67};
 * String hex = HexUtils.bytesToHex(data);
 * // Result: "01234567"
 *
 * // Convert hex to bytes (with or without 0x prefix)
 * byte[] bytes1 = HexUtils.hexToBytes("0x01234567");
 * byte[] bytes2 = HexUtils.hexToBytes("01234567");
 * // Both produce the same result
 *
 * // Validate hex string
 * boolean valid = HexUtils.isValidHex("0x1a2b3c");
 * // Result: true
 * }</pre>
 *
 * @since 1.0.0
 */
public class HexUtils {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * Converts a byte array to a lowercase hexadecimal string.
     *
     * <p>The resulting string does not include the "0x" prefix. Each byte
     * is represented as two hexadecimal characters (00-ff).</p>
     *
     * @param bytes the byte array to convert (may be null or empty)
     * @return a lowercase hexadecimal string representation (without "0x" prefix)
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
     * Converts a hexadecimal string to a byte array.
     *
     * <p>This method automatically handles the optional "0x" or "0X" prefix.
     * The hex string must have an even number of characters (after removing
     * the prefix) since each byte is represented by two hex characters.</p>
     *
     * @param hex the hexadecimal string to convert (with or without "0x" prefix)
     * @return the byte array representation of the hex string
     * @throws IllegalArgumentException if the hex string has odd length or contains invalid characters
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
     * Validates whether a string is a valid hexadecimal representation.
     *
     * <p>This method checks:</p>
     * <ul>
     *   <li>The string is not null</li>
     *   <li>After removing optional "0x" or "0X" prefix, the length is even</li>
     *   <li>All characters are valid hexadecimal digits (0-9, a-f, A-F)</li>
     * </ul>
     *
     * @param hex the string to validate
     * @return true if the string is a valid hex string, false otherwise
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