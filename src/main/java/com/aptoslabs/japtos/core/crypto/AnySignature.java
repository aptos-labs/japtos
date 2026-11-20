package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;
import java.util.Objects;

/**
 * Wrapper for any signature type, mirroring the TypeScript SDK's {@code AnySignature}.
 * BCS encoding is ULEB128 variant followed by the signature's own BCS encoding
 * ({@code serializeBytes} of the raw signature bytes).
 */
public class AnySignature implements Serializable {
    public static final int VARIANT_ED25519 = 0;
    public static final int VARIANT_SECP256K1 = 1;
    public static final int VARIANT_SECP256R1_WEBAUTHN = 2;
    public static final int VARIANT_KEYLESS = 3;
    public static final int VARIANT_SLH_DSA_SHA2_128S = 4;

    private final Signature signature;
    private final int variant;

    public AnySignature(Signature signature) {
        if (signature == null) {
            throw new IllegalArgumentException("Signature cannot be null");
        }
        this.signature = signature;
        if (signature instanceof SlhDsaSha2128sSignature) {
            this.variant = VARIANT_SLH_DSA_SHA2_128S;
        } else if (signature.getClass() == Signature.class) {
            this.variant = VARIANT_ED25519;
        } else {
            throw new IllegalArgumentException("Unsupported signature type: " + signature.getClass().getName());
        }
    }

    private AnySignature(Signature signature, int variant) {
        this.signature = signature;
        this.variant = variant;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(variant);
        serializer.serializeBytes(signature.toBytes());
    }

    public static AnySignature deserialize(Deserializer deserializer) throws IOException {
        int variant = deserializer.deserializeUleb128AsU32();
        switch (variant) {
            case VARIANT_ED25519:
                return new AnySignature(Signature.fromBytes(deserializer.deserializeBytes()), variant);
            case VARIANT_SLH_DSA_SHA2_128S:
                return new AnySignature(SlhDsaSha2128sSignature.deserialize(deserializer), variant);
            default:
                throw new IOException("Unknown variant index for AnySignature: " + variant
                        + ". Ensure the corresponding signature-scheme support is available.");
        }
    }

    public Signature getSignature() {
        return signature;
    }

    public int getVariant() {
        return variant;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AnySignature other = (AnySignature) obj;
        return variant == other.variant && Objects.equals(signature, other.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, signature);
    }

    @Override
    public String toString() {
        return "AnySignature{variant=" + variant + ", signature=" + signature + '}';
    }
}
