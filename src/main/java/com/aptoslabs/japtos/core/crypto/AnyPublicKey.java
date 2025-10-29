package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;

import java.io.IOException;

/**
 * Wrapper for any public key type supported by Aptos.
 * This allows MultiKey accounts to contain mixed key types (Ed25519, Keyless, etc.).
 * Matches TypeScript SDK's AnyPublicKey implementation.
 */
public class AnyPublicKey implements PublicKey {
    
    /**
     * Public key variant identifiers matching TypeScript AnyPublicKeyVariant enum
     */
    public static final int VARIANT_ED25519 = 0;
    public static final int VARIANT_SECP256K1 = 1;
    public static final int VARIANT_SECP256R1 = 2;
    public static final int VARIANT_KEYLESS = 3;
    public static final int VARIANT_FEDERATED_KEYLESS = 4;
    
    private final PublicKey publicKey;
    private final int variant;
    
    /**
     * Creates an AnyPublicKey wrapping the provided public key.
     * Automatically determines the variant based on the key type.
     *
     * @param publicKey the public key to wrap
     * @throws IllegalArgumentException if the key type is unsupported
     */
    public AnyPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        
        if (publicKey instanceof Ed25519PublicKey) {
            this.variant = VARIANT_ED25519;
        } else if (publicKey instanceof KeylessPublicKey) {
            this.variant = VARIANT_KEYLESS;
        } else {
            throw new IllegalArgumentException("Unsupported public key type: " + publicKey.getClass().getName());
        }
    }
    
    /**
     * Deserializes an AnyPublicKey from a deserializer.
     *
     * @param deserializer the deserializer to read from
     * @return the deserialized AnyPublicKey
     * @throws IOException if deserialization fails
     */
    public static AnyPublicKey deserialize(com.aptoslabs.japtos.bcs.Deserializer deserializer) 
            throws IOException {
        int variant = deserializer.deserializeUleb128AsU32();
        
        PublicKey publicKey;
        switch (variant) {
            case VARIANT_ED25519:
                publicKey = Ed25519PublicKey.deserialize(deserializer);
                break;
            case VARIANT_KEYLESS:
                publicKey = KeylessPublicKey.deserialize(deserializer);
                break;
            default:
                throw new IOException("Unknown variant index for AnyPublicKey: " + variant);
        }
        
        return new AnyPublicKey(publicKey);
    }
    
    /**
     * Serializes this AnyPublicKey.
     *
     * @param serializer the serializer to write to
     * @throws IOException if serialization fails
     */
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(variant);
        
        if (publicKey instanceof Ed25519PublicKey) {
            ((Ed25519PublicKey) publicKey).serialize(serializer);
        } else if (publicKey instanceof KeylessPublicKey) {
            ((KeylessPublicKey) publicKey).serialize(serializer);
        } else {
            throw new IOException("Cannot serialize unsupported public key type");
        }
    }
    
    @Override
    public byte[] toBytes() {
        return publicKey.toBytes();
    }
    
    @Override
    public String toHexString() {
        return publicKey.toHexString();
    }
    
    @Override
    public String toString() {
        return publicKey.toString();
    }
    
    @Override
    public AuthenticationKey authKey() {
        return publicKey.authKey();
    }
    
    @Override
    public AccountAddress accountAddress() {
        return publicKey.accountAddress();
    }
    
    public PublicKey getPublicKey() {
        return publicKey;
    }
    
    public int getVariant() {
        return variant;
    }
    
    @Override
    public boolean verifySignature(byte[] message, Signature signature) {
        // Delegate to the wrapped public key
        return publicKey.verifySignature(message, signature);
    }
}

