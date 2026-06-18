package com.aptoslabs.japtos.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HexUtils} covering encoding, decoding, prefix handling and validation.
 */
class HexUtilsTest {

    @Test
    @DisplayName("bytesToHex produces lowercase hex with no prefix")
    void bytesToHex() {
        byte[] data = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};
        assertEquals("0123456789abcdef", HexUtils.bytesToHex(data));
        assertEquals("", HexUtils.bytesToHex(new byte[0]));
    }

    @Test
    @DisplayName("hexToBytes is the inverse of bytesToHex and handles 0x/0X prefixes")
    void hexToBytesRoundTrip() {
        byte[] expected = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        assertArrayEquals(expected, HexUtils.hexToBytes("deadbeef"));
        assertArrayEquals(expected, HexUtils.hexToBytes("0xdeadbeef"));
        assertArrayEquals(expected, HexUtils.hexToBytes("0Xdeadbeef"));
        assertEquals("deadbeef", HexUtils.bytesToHex(HexUtils.hexToBytes("0xDEADBEEF")));
    }

    @Test
    @DisplayName("hexToBytes rejects odd lengths and invalid characters")
    void hexToBytesInvalid() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.hexToBytes("abc"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.hexToBytes("zz"));
    }

    @Test
    @DisplayName("isValidHex accepts valid hex and rejects null/odd/invalid input")
    void isValidHex() {
        assertTrue(HexUtils.isValidHex("0x1a2b3c"));
        assertTrue(HexUtils.isValidHex("ABCDEF"));
        assertTrue(HexUtils.isValidHex("0X00"));
        assertFalse(HexUtils.isValidHex(null));
        assertFalse(HexUtils.isValidHex("abc"));   // odd length
        assertFalse(HexUtils.isValidHex("zz"));    // invalid chars
        assertTrue(HexUtils.isValidHex(""));       // empty is even-length and contains no bad chars
    }
}
