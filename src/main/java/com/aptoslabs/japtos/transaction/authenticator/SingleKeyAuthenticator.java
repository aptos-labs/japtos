package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.crypto.AnyPublicKey;
import com.aptoslabs.japtos.core.crypto.AnySignature;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.IOException;

/**
 * AccountAuthenticator for the SingleKey scheme (variant 2), as used by
 * SLH-DSA-SHA2-128s and other {@link AnyPublicKey}/{@link AnySignature}
 * schemes. BCS encoding is {@code ULEB128(2) || AnyPublicKey || AnySignature}.
 *
 * <p>On the wire this authenticator is wrapped in TransactionAuthenticator
 * SingleSender (variant 4); see {@link com.aptoslabs.japtos.transaction.SignedTransaction}.</p>
 */
public class SingleKeyAuthenticator implements AccountAuthenticator {
    public static final int VARIANT_SINGLE_KEY = 2;

    private final AnyPublicKey anyPublicKey;
    private final AnySignature anySignature;

    public SingleKeyAuthenticator(PublicKey publicKey, Signature signature) {
        this.anyPublicKey = publicKey instanceof AnyPublicKey
                ? (AnyPublicKey) publicKey
                : new AnyPublicKey(publicKey);
        this.anySignature = new AnySignature(signature);
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(VARIANT_SINGLE_KEY);
        anyPublicKey.serialize(serializer);
        anySignature.serialize(serializer);
    }

    public static SingleKeyAuthenticator deserialize(Deserializer deserializer) throws IOException {
        int variant = deserializer.deserializeUleb128AsU32();
        if (variant != VARIANT_SINGLE_KEY) {
            throw new IOException("Expected SingleKey authenticator variant "
                    + VARIANT_SINGLE_KEY + ", got " + variant);
        }
        AnyPublicKey publicKey = AnyPublicKey.deserialize(deserializer);
        AnySignature signature = AnySignature.deserialize(deserializer);
        return new SingleKeyAuthenticator(publicKey, signature.getSignature());
    }

    @Override
    public byte[] getAuthenticationKey() {
        return anyPublicKey.authKey().toBytes();
    }

    @Override
    public byte[] getPublicKey() {
        return anyPublicKey.toBytes();
    }

    @Override
    public byte[] getSignature() {
        return anySignature.getSignature().toBytes();
    }

    public PublicKey getPublicKeyObject() {
        return anyPublicKey.getPublicKey();
    }

    public Signature getSignatureObject() {
        return anySignature.getSignature();
    }

    @Override
    public String toString() {
        return "SingleKeyAuthenticator{"
                + "publicKey=" + anyPublicKey
                + ", signature=" + anySignature
                + '}';
    }
}
