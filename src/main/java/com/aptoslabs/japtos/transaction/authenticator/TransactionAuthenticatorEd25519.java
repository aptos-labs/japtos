package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;

/**
 * Ed25519 transaction authenticator.
 */
public class TransactionAuthenticatorEd25519 implements Serializable {
    private final Ed25519PublicKey publicKey;
    private final Signature signature;

    public TransactionAuthenticatorEd25519(Ed25519PublicKey publicKey, Signature signature) {
        this.publicKey = publicKey;
        this.signature = signature;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize the transaction authenticator variant (0 for Ed25519) as ULEB128
        serializer.serializeU32AsUleb128(0);

        // Serialize the public key
        serializer.serializePublicKey(publicKey);

        // Serialize the signature
        serializer.serializeSignature(signature);
    }

    public Ed25519PublicKey getPublicKey() {
        return publicKey;
    }

    public Signature getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "TransactionAuthenticatorEd25519{" +
                "publicKey=" + publicKey.toString() +
                ", signature=" + signature.toString() +
                '}';
    }
}
