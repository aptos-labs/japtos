package com.aptoslabs.japtos.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CryptoUtils} using published NIST/FIPS test vectors to ensure
 * the digests are computed correctly (not merely "without throwing").
 */
class CryptoUtilsTest {

    @Test
    @DisplayName("SHA3-256 matches the FIPS-202 vector for the empty input")
    void sha3EmptyVector() {
        byte[] hash = CryptoUtils.sha3_256(new byte[0]);
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
                HexUtils.bytesToHex(hash));
    }

    @Test
    @DisplayName("SHA3-256 matches the known vector for \"abc\"")
    void sha3AbcVector() {
        byte[] hash = CryptoUtils.sha3_256("abc".getBytes());
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532",
                HexUtils.bytesToHex(hash));
    }

    @Test
    @DisplayName("SHA-256 matches the known vector for \"abc\"")
    void sha256AbcVector() {
        byte[] hash = CryptoUtils.sha256("abc".getBytes());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HexUtils.bytesToHex(hash));
    }

    @Test
    @DisplayName("Digest factory methods return usable, correctly-named instances")
    void digestFactories() {
        MessageDigest sha3 = CryptoUtils.getSHA3_256Digest();
        MessageDigest sha256 = CryptoUtils.getSHA256Digest();
        assertNotNull(sha3);
        assertNotNull(sha256);
        assertEquals(32, sha3.digest(new byte[0]).length);
        assertEquals(32, sha256.digest(new byte[0]).length);
    }

    @Test
    @DisplayName("ensureCryptoProvider is idempotent and makes SHA3-256 available")
    void ensureCryptoProviderIdempotent() {
        CryptoUtils.ensureCryptoProvider();
        CryptoUtils.ensureCryptoProvider(); // second call must be a no-op
        assertDoesNotThrow(() -> CryptoUtils.sha3_256("repeat".getBytes()));
    }
}
