package com.aptoslabs.japtos.utils;

import com.aptoslabs.japtos.utils.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for BIP44 hierarchical deterministic key derivation.
 *
 * <p>This class implements the BIP44 standard for deterministic key generation
 * from mnemonic seed phrases. It supports the Aptos-specific derivation path
 * format: m/44'/637'/account'/change'/address_index'</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Mnemonic to seed conversion using PBKDF2</li>
 *   <li>HMAC-SHA512 based key derivation</li>
 *   <li>BIP44 path validation for Aptos (coin type 637)</li>
 *   <li>Hardened derivation support for Ed25519</li>
 * </ul>
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki">BIP-44</a>
 * @see <a href="https://github.com/satoshilabs/slips/blob/master/slip-0010.md">SLIP-0010</a>
 * @since 1.0.0
 */
public class Bip44Utils {

    /**
     * The Ed25519 seed phrase for SLIP-0010 compatibility.
     */
    public static final String ED25519_SEED = "ed25519 seed";

    /**
     * The SLH-DSA-SHA2-128s seed phrase for SLIP-0010 HD derivation (matches TypeScript SDK).
     */
    public static final String SLH_DSA_SHA2_128S_SEED = "SLH-DSA-SHA2-128s seed";

    /**
     * Hardened derivation offset (2^31).
     */
    public static final int HARDENED_OFFSET = 0x80000000;

    /**
     * Aptos coin type as defined in SLIP-0044.
     */
    public static final int APTOS_COIN_TYPE = 637;

    /**
     * Regex pattern for validating Aptos BIP44 hardened paths.
     * Format: m/44'/637'/account'/change'/address_index'
     */
    private static final Pattern APTOS_HARDENED_PATH_PATTERN =
            Pattern.compile("^m/44'/637'/[0-9]+'/[0-9]+'/[0-9]+'?$");

    /**
     * Validates whether a derivation path follows the Aptos BIP44 hardened format.
     *
     * <p>Valid paths must follow the format: m/44'/637'/account'/change'/address_index'
     * where all components are hardened (indicated by the apostrophe).</p>
     *
     * @param path the derivation path to validate
     * @return true if the path is valid, false otherwise
     */
    public static boolean isValidHardenedPath(String path) {
        return APTOS_HARDENED_PATH_PATTERN.matcher(path).matches();
    }

    private static final Pattern GENERIC_HARDENED_PATH_PATTERN =
            Pattern.compile("^m(/[0-9]+')*$");

    /**
     * Validates whether {@code path} is a fully-hardened path such as {@code m} or {@code m/0'/1'/2'}.
     *
     * @param path the derivation path to validate
     * @return true if the path is valid, false otherwise
     */
    public static boolean isValidGenericHardenedPath(String path) {
        return path != null && GENERIC_HARDENED_PATH_PATTERN.matcher(path).matches();
    }

    /**
     * Derives the 48-byte SLH-DSA-SHA2-128s three-seeds value
     * (SK seed || PRF seed || PK seed) from a mnemonic, following the TypeScript SDK:
     * SLIP-0010 with curve seed {@value #SLH_DSA_SHA2_128S_SEED}, hardened derivation only,
     * then {@code derivedKey(32) || chainCode[0..16]}.
     *
     * @param path     a fully-hardened derivation path (e.g., {@code m/44'/637'/0'/0'/0'})
     * @param mnemonic the BIP39 mnemonic seed phrase
     * @return the 48-byte three-seeds key material
     */
    public static byte[] deriveSlhDsaSha2128sThreeSeeds(String path, String mnemonic) {
        return deriveSlhDsaSha2128sThreeSeedsFromSeed(path, mnemonicToSeed(mnemonic));
    }

    /**
     * Seed-based variant of {@link #deriveSlhDsaSha2128sThreeSeeds(String, String)}.
     *
     * @param path a fully-hardened derivation path
     * @param seed the raw master seed bytes
     * @return the 48-byte three-seeds key material
     */
    public static byte[] deriveSlhDsaSha2128sThreeSeedsFromSeed(String path, byte[] seed) {
        if (!isValidGenericHardenedPath(path)) {
            throw new IllegalArgumentException("Invalid derivation path " + path);
        }
        DerivedKeys current = deriveKey(
                SLH_DSA_SHA2_128S_SEED.getBytes(StandardCharsets.UTF_8), seed);
        for (int segment : parsePath(path)) {
            current = ckdPriv(current, segment + HARDENED_OFFSET);
        }
        byte[] threeSeeds = new byte[48];
        System.arraycopy(current.key, 0, threeSeeds, 0, 32);
        System.arraycopy(current.chainCode, 0, threeSeeds, 32, 16);
        return threeSeeds;
    }

    /**
     * Converts a mnemonic seed phrase to a seed byte array using PBKDF2.
     *
     * <p>This method normalizes the mnemonic by trimming whitespace, converting
     * to lowercase, and removing extra spaces between words.</p>
     *
     * @param mnemonic the mnemonic seed phrase
     * @return the seed bytes derived from the mnemonic
     * @throws RuntimeException if PBKDF2 generation fails
     */
    public static byte[] mnemonicToSeed(String mnemonic) {
        // Normalize the mnemonic
        String normalizedMnemonic = mnemonic.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();

        try {
            // Use PBKDF2 with HMAC-SHA512 (standard BIP39 approach)
            String passphrase = ""; // Empty passphrase for simplicity
            String saltString = "mnemonic" + passphrase;
            byte[] salt = saltString.getBytes(StandardCharsets.UTF_8);

            // Simple PBKDF2 implementation
            return pbkdf2(normalizedMnemonic.getBytes(StandardCharsets.UTF_8), salt, 2048, 64);
        } catch (Exception e) {
            Logger.error("Failed to derive seed from mnemonic", e);
            throw new RuntimeException("Failed to derive seed from mnemonic", e);
        }
    }

    /**
     * Derives a child private key from a parent key using BIP44 derivation.
     *
     * @param path     the BIP44 derivation path
     * @param mnemonic the mnemonic seed phrase
     * @return the derived private key bytes
     * @throws IllegalArgumentException if the path is invalid
     * @throws RuntimeException         if key derivation fails
     */
    public static byte[] derivePrivateKey(String path, String mnemonic) {
        if (!isValidHardenedPath(path)) {
            throw new IllegalArgumentException("Invalid derivation path: " + path);
        }

        byte[] seed = mnemonicToSeed(mnemonic);
        DerivedKeys masterKeys = deriveKey(ED25519_SEED.getBytes(StandardCharsets.UTF_8), seed);

        List<Integer> pathComponents = parsePath(path);

        DerivedKeys currentKeys = masterKeys;
        for (int component : pathComponents) {
            currentKeys = ckdPriv(currentKeys, component + HARDENED_OFFSET);
        }

        return currentKeys.key;
    }

    /**
     * Derives a key using HMAC-SHA512.
     *
     * @param hmacKey the HMAC key
     * @param data    the data to derive from
     * @return the derived keys (key + chain code)
     */
    private static DerivedKeys deriveKey(byte[] hmacKey, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(hmacKey, "HmacSHA512");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(data);

            byte[] key = Arrays.copyOfRange(digest, 0, 32);
            byte[] chainCode = Arrays.copyOfRange(digest, 32, 64);

            return new DerivedKeys(key, chainCode);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.error("Failed to derive key", e);
            throw new RuntimeException("Failed to derive key", e);
        }
    }

    /**
     * Child Key Derivation (Private) - derives a child private key.
     *
     * @param parentKeys the parent keys (key + chain code)
     * @param index      the child index (should include hardened offset if needed)
     * @return the derived child keys
     */
    private static DerivedKeys ckdPriv(DerivedKeys parentKeys, int index) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(37); // 1 + 32 + 4 bytes
            buffer.put((byte) 0); // 0x00 padding for hardened derivation
            buffer.put(parentKeys.key); // 32-byte parent private key
            buffer.putInt(index); // 4-byte index

            return deriveKey(parentKeys.chainCode, buffer.array());
        } catch (Exception e) {
            Logger.error("Failed to derive child key", e);
            throw new RuntimeException("Failed to derive child key", e);
        }
    }

    /**
     * Parses a BIP44 derivation path into its numeric components.
     *
     * @param path the derivation path (e.g., "m/44'/637'/0'/0'/0'")
     * @return list of path components as integers
     */
    private static List<Integer> parsePath(String path) {
        String[] parts = path.split("/");
        List<Integer> components = new ArrayList<>();

        // Skip the first part (should be "m")
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            // Remove apostrophe if present
            if (part.endsWith("'")) {
                part = part.substring(0, part.length() - 1);
            }
            components.add(Integer.parseInt(part));
        }

        return components;
    }

    /**
     * Simple PBKDF2 implementation using HMAC-SHA512.
     *
     * @param password   the password bytes
     * @param salt       the salt bytes
     * @param iterations the number of iterations
     * @param keyLength  the desired key length in bytes
     * @return the derived key bytes
     */
    private static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(password, "HmacSHA512");
        mac.init(secretKey);

        byte[] result = new byte[keyLength];
        int hashLength = mac.getMacLength();
        int blocks = (keyLength + hashLength - 1) / hashLength;

        for (int i = 1; i <= blocks; i++) {
            byte[] block = new byte[salt.length + 4];
            System.arraycopy(salt, 0, block, 0, salt.length);

            // Add block index in big-endian format
            block[salt.length] = (byte) (i >>> 24);
            block[salt.length + 1] = (byte) (i >>> 16);
            block[salt.length + 2] = (byte) (i >>> 8);
            block[salt.length + 3] = (byte) i;

            byte[] u = mac.doFinal(block);
            byte[] temp = Arrays.copyOf(u, u.length);

            for (int j = 1; j < iterations; j++) {
                u = mac.doFinal(u);
                for (int k = 0; k < u.length; k++) {
                    temp[k] ^= u[k];
                }
            }

            int copyLength = Math.min(temp.length, keyLength - (i - 1) * hashLength);
            System.arraycopy(temp, 0, result, (i - 1) * hashLength, copyLength);
        }

        return result;
    }

    /**
     * Represents a derived cryptographic key pair.
     */
    public static class DerivedKeys {
        public final byte[] key;
        public final byte[] chainCode;

        public DerivedKeys(byte[] key, byte[] chainCode) {
            this.key = Arrays.copyOf(key, key.length);
            this.chainCode = Arrays.copyOf(chainCode, chainCode.length);
        }
    }
}
