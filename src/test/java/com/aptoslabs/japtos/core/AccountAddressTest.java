package com.aptoslabs.japtos.core;

import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AccountAddress}.
 */
class AccountAddressTest {

    private static final String ONE =
            "0000000000000000000000000000000000000000000000000000000000000001";

    @Test
    @DisplayName("fromHex accepts an 0x-prefixed 32-byte address and round-trips")
    void fromHexRoundTrip() {
        AccountAddress a = AccountAddress.fromHex("0x" + ONE);
        assertEquals(ONE, a.toHexString());
        assertEquals("0x" + ONE, a.toString());
        assertArrayEquals(HexUtils.hexToBytes(ONE), a.toBytes());
    }

    @Test
    @DisplayName("zero() is all-zero and reports isZero")
    void zeroAddress() {
        AccountAddress zero = AccountAddress.zero();
        assertTrue(zero.isZero());
        assertEquals(32, zero.toBytes().length);

        AccountAddress one = AccountAddress.fromHex("0x" + ONE);
        assertFalse(one.isZero());
    }

    @Test
    @DisplayName("fromBytes enforces a 32-byte length")
    void fromBytesLengthValidation() {
        assertThrows(IllegalArgumentException.class, () -> AccountAddress.fromBytes(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> AccountAddress.fromBytes(new byte[33]));
        assertDoesNotThrow(() -> AccountAddress.fromBytes(new byte[32]));
    }

    @Test
    @DisplayName("fromPublicKey treats the input as a raw 32-byte address")
    void fromPublicKey() {
        byte[] raw = new byte[32];
        raw[0] = 0x42;
        AccountAddress addr = AccountAddress.fromPublicKey(raw);
        assertArrayEquals(raw, addr.toBytes());
    }

    @Test
    @DisplayName("toBytes returns a defensive copy")
    void toBytesIsDefensiveCopy() {
        AccountAddress addr = AccountAddress.fromHex("0x" + ONE);
        byte[] bytes = addr.toBytes();
        bytes[0] = (byte) 0xff;
        assertEquals(ONE, addr.toHexString(), "mutating the returned array must not change the address");
    }

    @Test
    @DisplayName("equals and hashCode are value-based")
    void equalsAndHashCode() {
        AccountAddress a = AccountAddress.fromHex("0x" + ONE);
        AccountAddress b = AccountAddress.fromHex(ONE);
        AccountAddress c = AccountAddress.zero();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, "not an address");
        assertEquals(a, a);
    }
}
