package com.aptoslabs.japtos.utils;

import com.aptoslabs.japtos.utils.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

/**
 * Utility class for cryptographic operations with automatic crypto provider initialization.
 *
 * <p>This class provides a centralized way to handle cryptographic algorithms
 * that may not be available on all platforms (particularly Android). It includes
 * automatic crypto provider registration to ensure SHA3-256 is available.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic SpongyCastle (Android) or BouncyCastle (desktop) provider registration</li>
 *   <li>SHA3-256 support with fallback</li>
 *   <li>Platform-agnostic cryptographic operations</li>
 *   <li>Self-contained - no external setup required</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CryptoUtils {

    private static final String SHA3_256_ALGORITHM = "SHA3-256";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final Object lock = new Object();
    private static volatile boolean cryptoProviderRegistered = false;
    private static volatile String activeProviderName = null;

    /**
     * Ensures crypto provider (SpongyCastle or BouncyCastle) is registered for SHA3-256 support.
     * This is particularly important for Android applications.
     *
     * <p>This method is thread-safe and will only register the provider once.</p>
     */
    public static void ensureCryptoProvider() {
        if (!cryptoProviderRegistered) {
            synchronized (lock) {
                if (!cryptoProviderRegistered) {
                    try {
                        // First try to load SpongyCastle (Android-compatible)
                        try {
                            // Check if we're on Android by looking for Android-specific classes
                            boolean isAndroid = false;
                            try {
                                Class.forName("android.os.Build");
                                isAndroid = true;
                            } catch (ClassNotFoundException e) {
                                // Not Android
                            }

                            if (isAndroid) {
                                // On Android, don't register SpongyCastle provider to avoid DRBG issues
                                // Just check if SHA3-256 is available without provider
                                try {
                                    MessageDigest.getInstance(SHA3_256_ALGORITHM);
                                    Logger.debug("SHA3-256 available on Android without provider registration");
                                    cryptoProviderRegistered = true;
                                    return;
                                } catch (NoSuchAlgorithmException e) {
                                    // SHA3-256 not available, continue with provider registration
                                    Logger.debug("SHA3-256 not available on Android, will register provider");
                                }
                            }

                            Class<?> spongyCastleProviderClass = Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
                            if (Security.getProvider("SC") == null) {
                                Object spongyCastleProvider = spongyCastleProviderClass.getDeclaredConstructor().newInstance();
                                Security.insertProviderAt((java.security.Provider) spongyCastleProvider, 1);
                                activeProviderName = "SC";
                                Logger.debug("SpongyCastle provider registered successfully");
                            } else {
                                activeProviderName = "SC";
                                Logger.debug("SpongyCastle provider already registered");
                            }
                            cryptoProviderRegistered = true;
                            return;
                        } catch (Exception e) {
                            // SpongyCastle not available, try BouncyCastle
                            Logger.debug("SpongyCastle not available, trying BouncyCastle: %s", e.getMessage());
                        }

                        // Try to load BouncyCastle
                        try {
                            Class<?> bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                            if (Security.getProvider("BC") == null) {
                                Object bouncyCastleProvider = bouncyCastleProviderClass.getDeclaredConstructor().newInstance();
                                Security.insertProviderAt((java.security.Provider) bouncyCastleProvider, 1);
                                activeProviderName = "BC";
                                Logger.debug("BouncyCastle provider registered successfully");
                            } else {
                                activeProviderName = "BC";
                                Logger.debug("BouncyCastle provider already registered");
                            }
                            cryptoProviderRegistered = true;
                        } catch (Exception e) {
                            Logger.warn("Could not register BouncyCastle provider", e);
                        }
                    } catch (Exception e) {
                        // Log warning but don't fail - we'll handle it in getMessageDigest
                        Logger.warn("Could not register any crypto provider", e);
                    }
                }
            }
        }
    }

    /**
     * Gets a MessageDigest instance for SHA3-256 with automatic crypto provider initialization.
     *
     * <p>This method automatically ensures a crypto provider is registered and provides
     * helpful error information if SHA3-256 is still not available.</p>
     *
     * @return MessageDigest instance for SHA3-256
     * @throws RuntimeException if SHA3-256 is not available and cannot be made available
     */
    public static MessageDigest getSHA3_256Digest() {
        ensureCryptoProvider();

        try {
            // First try without specifying provider
            return MessageDigest.getInstance(SHA3_256_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Try with active provider explicitly
            if (activeProviderName != null) {
                try {
                    return MessageDigest.getInstance(SHA3_256_ALGORITHM, activeProviderName);
                } catch (NoSuchAlgorithmException | NoSuchProviderException e2) {
                    // Try to reinitialize provider and retry once
                    try {
                        cryptoProviderRegistered = false;
                        activeProviderName = null;
                        ensureCryptoProvider();
                        if (activeProviderName != null) {
                            return MessageDigest.getInstance(SHA3_256_ALGORITHM, activeProviderName);
                        }
                    } catch (Exception retryException) {
                        // Ignore retry exception, throw original
                        Logger.debug("Failed to retry provider initialization", retryException);
                    }

                    throw new RuntimeException(
                            "SHA3-256 not available with " + activeProviderName + " provider. " +
                                    "This is required for Aptos authentication key derivation. " +
                                    "Please ensure you have the appropriate crypto library in your project:\n" +
                                    "For Android: implementation 'com.madgag.spongycastle:bctls-jdk15on:1.58.0.0'\n" +
                                    "For Desktop: implementation 'org.bouncycastle:bcprov-jdk18on:1.77'",
                            e2
                    );
                }
            } else {
                throw new RuntimeException(
                        "SHA3-256 not available. No crypto provider was registered. " +
                                "This is required for Aptos authentication key derivation. " +
                                "Please ensure you have the appropriate crypto library in your project:\n" +
                                "For Android: implementation 'com.madgag.spongycastle:bctls-jdk15on:1.58.0.0'\n" +
                                "For Desktop: implementation 'org.bouncycastle:bcprov-jdk18on:1.77'",
                        e
                );
            }
        }
    }

    /**
     * Gets a MessageDigest instance for SHA-256.
     *
     * @return MessageDigest instance for SHA-256
     * @throws RuntimeException if SHA-256 is not available
     */
    public static MessageDigest getSHA256Digest() {
        try {
            return MessageDigest.getInstance(SHA256_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Logger.error("SHA-256 not available", e);
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Computes SHA3-256 hash of the given data.
     *
     * @param data the data to hash
     * @return the SHA3-256 hash bytes
     * @throws RuntimeException if SHA3-256 is not available
     */
    public static byte[] sha3_256(byte[] data) {
        MessageDigest digest = getSHA3_256Digest();
        return digest.digest(data);
    }

    /**
     * Computes SHA-256 hash of the given data.
     *
     * @param data the data to hash
     * @return the SHA-256 hash bytes
     * @throws RuntimeException if SHA-256 is not available
     */
    public static byte[] sha256(byte[] data) {
        MessageDigest digest = getSHA256Digest();
        return digest.digest(data);
    }
}
