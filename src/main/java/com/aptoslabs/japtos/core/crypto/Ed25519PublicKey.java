package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.utils.HexUtils;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.util.Arrays;

/**
 * Represents an Ed25519 public key used for signature verification in the Aptos ecosystem.
 * 
 * <p>Ed25519 is a high-performance elliptic curve signature scheme that provides strong
 * security guarantees. This class implements the PublicKey interface and encapsulates
 * a 32-byte Ed25519 public key with methods for signature verification, authentication
 * key derivation, and serialization.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>32-byte public key representation</li>
 *   <li>Ed25519 signature verification</li>
 *   <li>Authentication key derivation using SHA3-256</li>
 *   <li>Account address computation</li>
 *   <li>Hex and binary serialization support</li>
 * </ul>
 * 
 * <p>Authentication Key Derivation:</p>
 * <p>For Ed25519 public keys, the authentication key is computed as:
 * {@code SHA3-256(public_key_bytes || 0x00)}</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create from hex string
 * Ed25519PublicKey publicKey = Ed25519PublicKey.fromHex(
 *     "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
 * );
 * 
 * // Verify a signature
 * byte[] message = "Hello, Aptos!".getBytes();
 * Signature signature = // ... obtain signature
 * boolean isValid = publicKey.verifySignature(message, signature);
 * 
 * // Get account address
 * AccountAddress address = publicKey.accountAddress();
 * }</pre>
 * 
 * @see PublicKey
 * @see Ed25519PrivateKey
 * @see Signature
 * @see AuthenticationKey
 * @see AccountAddress
 * @since 1.0.0
 */
public class Ed25519PublicKey implements PublicKey {
    /** The required length of an Ed25519 public key in bytes. */
    public static final int LENGTH = 32;
    private final byte[] key;
    
    private Ed25519PublicKey(byte[] key) {
        if (key.length != LENGTH) {
            throw new IllegalArgumentException("Ed25519 public key must be exactly " + LENGTH + " bytes");
        }
        this.key = Arrays.copyOf(key, key.length);
    }
    
    /**
     * Creates an Ed25519PublicKey from a byte array.
     * 
     * <p>The byte array must be exactly 32 bytes long. This method creates
     * a defensive copy of the input array to prevent external modification
     * of the public key material.</p>
     * 
     * @param key the 32-byte public key data
     * @return a new Ed25519PublicKey instance
     * @throws IllegalArgumentException if the key is not exactly 32 bytes
     */
    public static Ed25519PublicKey fromBytes(byte[] key) {
        return new Ed25519PublicKey(key);
    }
    
    /**
     * Creates an Ed25519PublicKey from a hexadecimal string.
     * 
     * <p>The hex string can optionally start with '0x' prefix, which will be
     * automatically stripped. The hex string must represent exactly 32 bytes
     * (64 hex characters after prefix removal).</p>
     * 
     * @param hex the hexadecimal representation of the public key
     * @return a new Ed25519PublicKey instance
     * @throws IllegalArgumentException if the hex string doesn't represent 32 bytes
     * @throws NumberFormatException if the string contains invalid hex characters
     */
    public static Ed25519PublicKey fromHex(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hex);
        return fromBytes(bytes);
    }
    
    @Override
    public byte[] toBytes() {
        return Arrays.copyOf(key, key.length);
    }
    
    @Override
    public String toHexString() {
        return HexUtils.bytesToHex(key);
    }
    
    @Override
    public String toString() {
        return "0x" + toHexString();
    }
    
    @Override
    public AuthenticationKey authKey() {
        // For Ed25519, the authentication key is sha3_256(public_key || 0x00)
        return AuthenticationKey.fromPublicKey(key);
    }
    
    @Override
    public boolean verifySignature(byte[] message, Signature signature) {
        try {
            Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(key, 0);
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, publicKeyParams);
            signer.update(message, 0, message.length);
            return signer.verifySignature(signature.toBytes());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Ed25519PublicKey that = (Ed25519PublicKey) obj;
        return Arrays.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
