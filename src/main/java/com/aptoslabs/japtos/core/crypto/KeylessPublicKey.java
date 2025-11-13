package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a Keyless Public Key used for OAuth/passkey authentication.
 * This key type is used in keyless authentication schemes where users authenticate
 * via OIDC providers (Google, Apple, etc.) instead of managing private keys.
 */
public class KeylessPublicKey implements PublicKey {
    
    /**
     * The required length of the id commitment in bytes
     */
    public static final int ID_COMMITMENT_LENGTH = 32;
    
    /**
     * The OIDC provider identifier (e.g., "https://accounts.google.com")
     */
    private final String iss;
    
    /**
     * A 32-byte cryptographic commitment to the user's identity
     * Calculated from aud, uidKey, uidVal, and pepper
     */
    private final byte[] idCommitment;
    
    /**
     * Creates a KeylessPublicKey with the specified issuer and id commitment.
     *
     * @param iss the OIDC provider identifier
     * @param idCommitment the 32-byte identity commitment
     * @throws IllegalArgumentException if idCommitment is not exactly 32 bytes
     */
    public KeylessPublicKey(String iss, byte[] idCommitment) {
        if (idCommitment.length != ID_COMMITMENT_LENGTH) {
            throw new IllegalArgumentException(
                "Id Commitment length in bytes should be " + ID_COMMITMENT_LENGTH + 
                " but got " + idCommitment.length
            );
        }
        this.iss = iss;
        this.idCommitment = Arrays.copyOf(idCommitment, idCommitment.length);
    }
    
    /**
     * Deserializes a KeylessPublicKey from a hex string.
     * The hex string should contain the BCS-serialized keyless public key.
     *
     * @param hexString the hex string (with or without 0x prefix)
     * @return the deserialized KeylessPublicKey
     * @throws Exception if deserialization fails
     */
    public static KeylessPublicKey fromHexString(String hexString) throws Exception {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }
        byte[] bytes = HexUtils.hexToBytes(hexString);
        
        // Create deserializer
        com.aptoslabs.japtos.bcs.Deserializer deserializer = 
            new com.aptoslabs.japtos.bcs.Deserializer(bytes);
        
        return deserialize(deserializer);
    }
    
    /**
     * Deserializes a KeylessPublicKey from a deserializer.
     *
     * @param deserializer the deserializer to read from
     * @return the deserialized KeylessPublicKey
     * @throws IOException if deserialization fails
     */
    public static KeylessPublicKey deserialize(com.aptoslabs.japtos.bcs.Deserializer deserializer) 
            throws IOException {
        String iss = deserializer.deserializeString();
        byte[] idCommitment = deserializer.deserializeBytes();
        return new KeylessPublicKey(iss, idCommitment);
    }
    
    /**
     * Serializes this KeylessPublicKey.
     *
     * @param serializer the serializer to write to
     * @throws IOException if serialization fails
     */
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeString(iss);
        serializer.serializeBytes(idCommitment);
    }
    
    @Override
    public byte[] toBytes() {
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            return serializer.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize KeylessPublicKey", e);
            throw new RuntimeException("Failed to serialize KeylessPublicKey", e);
        }
    }
    
    @Override
    public String toHexString() {
        return HexUtils.bytesToHex(toBytes());
    }
    
    @Override
    public String toString() {
        return "0x" + toHexString();
    }
    
    @Override
    public AuthenticationKey authKey() {
        // For keyless keys used in MultiKey, the auth key is derived from the MultiKey structure
        // This method creates a SingleKey auth key for standalone keyless accounts
        try {
            Serializer serializer = new Serializer();
            serializer.serializeU32AsUleb128(3); // Keyless variant = 3
            serializer.serializeFixedBytes(toBytes());
            return AuthenticationKey.fromSchemeAndBytes((byte) 0, serializer.toByteArray());
        } catch (Exception e) {
            Logger.error("Failed to derive auth key for KeylessPublicKey", e);
            throw new RuntimeException("Failed to derive auth key", e);
        }
    }
    
    @Override
    public com.aptoslabs.japtos.core.AccountAddress accountAddress() {
        return authKey().accountAddress();
    }
    
    public String getIss() {
        return iss;
    }
    
    public byte[] getIdCommitment() {
        return Arrays.copyOf(idCommitment, idCommitment.length);
    }
    
    @Override
    public boolean verifySignature(byte[] message, Signature signature) {
        // Keyless signature verification requires ZK proof verification
        // For now, not implemented as it's only needed for address derivation
        throw new UnsupportedOperationException(
            "Keyless signature verification not implemented. " +
            "This key is intended for MultiKey address derivation only."
        );
    }
}

