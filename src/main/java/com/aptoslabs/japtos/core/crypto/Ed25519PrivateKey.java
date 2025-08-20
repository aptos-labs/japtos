package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.utils.Bip44Utils;
import com.aptoslabs.japtos.utils.HexUtils;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Represents an Ed25519 private key used for cryptographic signing in the Aptos ecosystem.
 * 
 * <p>Ed25519 is a public-key signature system that provides high security and performance.
 * This class encapsulates a 32-byte Ed25519 private key and provides methods for key
 * generation, serialization, and digital signing operations.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>32-byte private key representation</li>
 *   <li>Secure random key generation</li>
 *   <li>Public key derivation</li>
 *   <li>Message signing with Ed25519 algorithm</li>
 *   <li>Hex and binary serialization support</li>
 * </ul>
 * 
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Private keys should be generated using cryptographically secure random sources</li>
 *   <li>Private key material should be kept secret and protected from unauthorized access</li>
 *   <li>Keys should be stored securely and destroyed when no longer needed</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Generate a new private key
 * Ed25519PrivateKey privateKey = Ed25519PrivateKey.generate();
 * 
 * // Get the corresponding public key
 * Ed25519PublicKey publicKey = privateKey.publicKey();
 * 
 * // Sign a message
 * byte[] message = "Hello, Aptos!".getBytes();
 * Signature signature = privateKey.sign(message);
 * 
 * // Verify the signature
 * boolean isValid = publicKey.verifySignature(message, signature);
 * }</pre>
 * 
 * @see Ed25519PublicKey
 * @see Signature
 * @see SecureRandom
 * @since 1.0.0
 */
public class Ed25519PrivateKey {
    /** The required length of an Ed25519 private key in bytes. */
    public static final int LENGTH = 32;
    private final byte[] key;
    
    private Ed25519PrivateKey(byte[] key) {
        if (key.length != LENGTH) {
            throw new IllegalArgumentException("Ed25519 private key must be exactly " + LENGTH + " bytes");
        }
        this.key = Arrays.copyOf(key, key.length);
    }
    
    /**
     * Creates an Ed25519PrivateKey from a byte array.
     * 
     * <p>The byte array must be exactly 32 bytes long. This method creates
     * a defensive copy of the input array to prevent external modification
     * of the private key material.</p>
     * 
     * @param key the 32-byte private key data
     * @return a new Ed25519PrivateKey instance
     * @throws IllegalArgumentException if the key is not exactly 32 bytes
     */
    public static Ed25519PrivateKey fromBytes(byte[] key) {
        return new Ed25519PrivateKey(key);
    }
    
    /**
     * Creates an Ed25519PrivateKey from a hexadecimal string.
     * 
     * <p>The hex string can optionally start with '0x' prefix, which will be
     * automatically stripped. The hex string must represent exactly 32 bytes
     * (64 hex characters after prefix removal).</p>
     * 
     * @param hex the hexadecimal representation of the private key
     * @return a new Ed25519PrivateKey instance
     * @throws IllegalArgumentException if the hex string doesn't represent 32 bytes
     * @throws NumberFormatException if the string contains invalid hex characters
     */
    public static Ed25519PrivateKey fromHex(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hex);
        return fromBytes(bytes);
    }
    
    /**
     * Generates a new Ed25519PrivateKey using a cryptographically secure random source.
     * 
     * <p>This method uses Java's SecureRandom to generate 32 random bytes for the
     * private key. The generated key is suitable for production use and provides
     * strong cryptographic security.</p>
     * 
     * @return a new randomly generated Ed25519PrivateKey
     * @see SecureRandom
     */
    public static Ed25519PrivateKey generate() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[LENGTH];
        random.nextBytes(key);
        return fromBytes(key);
    }
    
    /**
     * Derives an Ed25519PrivateKey from a BIP44 derivation path and mnemonic seed phrase.
     * 
     * <p>This method implements BIP44 hierarchical deterministic key derivation
     * using the Ed25519 curve. The path must follow the Aptos standard format:
     * m/44'/637'/account'/change'/address_index' where all components are hardened.</p>
     * 
     * <p>The derivation process:</p>
     * <ol>
     *   <li>Convert mnemonic to seed using PBKDF2</li>
     *   <li>Generate master key from seed using HMAC-SHA512</li>
     *   <li>Derive child keys following the BIP44 path</li>
     *   <li>Return the final derived private key</li>
     * </ol>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
     * String path = "m/44'/637'/0'/0'/0'";
     * Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromDerivationPath(path, mnemonic);
     * }</pre>
     * 
     * @param path the BIP44 derivation path (e.g., "m/44'/637'/0'/0'/0'")
     * @param mnemonic the mnemonic seed phrase (12 or 24 words)
     * @return the derived Ed25519PrivateKey
     * @throws IllegalArgumentException if the path format is invalid
     * @throws RuntimeException if key derivation fails
     * @see Bip44Utils#derivePrivateKey(String, String)
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki">BIP-44</a>
     */
    public static Ed25519PrivateKey fromDerivationPath(String path, String mnemonic) {
        byte[] derivedKey = Bip44Utils.derivePrivateKey(path, mnemonic);
        return fromBytes(derivedKey);
    }
    
    /**
     * Returns the private key as a byte array.
     * 
     * <p>This method returns a defensive copy of the private key bytes to prevent
     * external modification. The returned array is always exactly 32 bytes long.</p>
     * 
     * @return a copy of the 32-byte private key data
     */
    public byte[] toBytes() {
        return Arrays.copyOf(key, key.length);
    }
    
    /**
     * Returns the private key as a hexadecimal string without '0x' prefix.
     * 
     * <p>The returned string is lowercase and exactly 64 characters long,
     * representing the 32 bytes of the private key in hexadecimal format.</p>
     * 
     * @return the private key as a lowercase hex string (no '0x' prefix)
     */
    public String toHexString() {
        return HexUtils.bytesToHex(key);
    }
    
    /**
     * Returns the private key as a hexadecimal string with '0x' prefix.
     * 
     * <p>This method provides a blockchain-standard representation of the
     * private key with the '0x' prefix indicating hexadecimal format.</p>
     * 
     * @return the private key as a hex string with '0x' prefix
     */
    public String toString() {
        return "0x" + toHexString();
    }
    
    /**
     * Derives the corresponding Ed25519 public key from this private key.
     * 
     * <p>This method uses the Ed25519 key derivation algorithm to compute
     * the public key from the private key. The derived public key can be
     * used for signature verification and account address generation.</p>
     * 
     * @return the Ed25519PublicKey corresponding to this private key
     * @see Ed25519PublicKey
     */
    public Ed25519PublicKey publicKey() {
        Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(key, 0);
        Ed25519PublicKeyParameters publicKeyParams = privateKeyParams.generatePublicKey();
        return Ed25519PublicKey.fromBytes(publicKeyParams.getEncoded());
    }
    
    /**
     * Signs a message using this Ed25519 private key.
     * 
     * <p>This method creates a digital signature for the given message using
     * the Ed25519 signature algorithm. The resulting signature can be verified
     * using the corresponding public key.</p>
     * 
     * @param message the message bytes to sign
     * @return the Ed25519 signature for the message
     * @see Signature
     * @see Ed25519PublicKey#verifySignature(byte[], Signature)
     */
    public Signature sign(byte[] message) {
        Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(key, 0);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKeyParams);
        signer.update(message, 0, message.length);
        byte[] signatureBytes = signer.generateSignature();
        return Signature.fromBytes(signatureBytes);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Ed25519PrivateKey that = (Ed25519PrivateKey) obj;
        return Arrays.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
