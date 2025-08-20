package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;

/**
 * Ed25519 account authenticator.

 */
public class Ed25519Authenticator implements AccountAuthenticator {
    private final Ed25519PublicKey publicKey;
    private final Signature signature;
    
    public Ed25519Authenticator(Ed25519PublicKey publicKey, Signature signature) {
        this.publicKey = publicKey;
        this.signature = signature;
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize the authenticator variant (0 for Ed25519) as U8
        serializer.serializeU8((byte) 0);
        
        // Serialize the public key
        serializer.serializePublicKey(publicKey);
        
        // Serialize the signature
        serializer.serializeSignature(signature);
    }
    
    @Override
    public byte[] getAuthenticationKey() {
        return publicKey.authKey().toBytes();
    }
    
    @Override
    public byte[] getPublicKey() {
        return publicKey.toBytes();
    }
    
    @Override
    public byte[] getSignature() {
        return signature.toBytes();
    }
    
    public Ed25519PublicKey getPublicKeyObject() {
        return publicKey;
    }
    
    public Signature getSignatureObject() {
        return signature;
    }
    
    @Override
    public String toString() {
        return "Ed25519Authenticator{" +
            "publicKey=" + publicKey.toString() +
            ", signature=" + signature.toString() +
            '}';
    }
}
