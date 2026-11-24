package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.utils.HexUtils;
import com.aptoslabs.japtos.utils.Logger;
import org.bouncycastle.crypto.params.SLHDSAParameters;
import org.bouncycastle.crypto.params.SLHDSAPublicKeyParameters;
import org.bouncycastle.crypto.signers.SLHDSASigner;

import java.io.IOException;
import java.util.Arrays;

/**
 * SLH-DSA-SHA2-128s public key (32 bytes: PK seed || PK root), the post-quantum
 * signature scheme from AIP-137.
 *
 * <p>Matches the TypeScript SDK's {@code SlhDsaSha2128sPublicKey}: BCS serialization
 * is {@code serializeBytes} (length-prefixed), the {@link AnyPublicKey} variant is
 * {@link AnyPublicKey#VARIANT_SLH_DSA_SHA2_128S} (5), and the account authentication
 * key is the SingleKey derivation {@code SHA3-256(BCS(AnyPublicKey) || 0x02)}.</p>
 *
 * <p>Verification uses BouncyCastle's FIPS 205 SLH-DSA implementation
 * ({@code SLHDSAParameters.sha2_128s}) and accepts both deterministic and hedged
 * signatures (such as those produced by {@code @noble/post-quantum}).</p>
 */
public class SlhDsaSha2128sPublicKey implements PublicKey, Serializable {
    public static final int LENGTH = 32;
    /** Signing-scheme byte for SingleKey authentication keys. */
    static final byte SINGLE_KEY_SCHEME = 2;

    private final byte[] key;

    private SlhDsaSha2128sPublicKey(byte[] key) {
        if (key == null || key.length != LENGTH) {
            int received = key == null ? 0 : key.length;
            throw new IllegalArgumentException(
                    "PublicKey length should be " + LENGTH + ", received " + received);
        }
        this.key = Arrays.copyOf(key, key.length);
    }

    public static SlhDsaSha2128sPublicKey fromBytes(byte[] key) {
        return new SlhDsaSha2128sPublicKey(key);
    }

    public static SlhDsaSha2128sPublicKey fromHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("PublicKey hex string cannot be null");
        }
        return fromBytes(HexUtils.hexToBytes(hex));
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeBytes(key);
    }

    public static SlhDsaSha2128sPublicKey deserialize(Deserializer deserializer) throws IOException {
        return fromBytes(deserializer.deserializeBytes());
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
        // SingleKey: SHA3-256(BCS(AnyPublicKey(this)) || 0x02)
        return AuthenticationKey.fromSchemeAndBytes(SINGLE_KEY_SCHEME, new AnyPublicKey(this).bcsToBytes());
    }

    @Override
    public AccountAddress accountAddress() {
        return authKey().accountAddress();
    }

    @Override
    public boolean verifySignature(byte[] message, Signature signature) {
        if (message == null || !(signature instanceof SlhDsaSha2128sSignature)) {
            return false;
        }
        try {
            SLHDSAPublicKeyParameters params =
                    new SLHDSAPublicKeyParameters(SLHDSAParameters.sha2_128s, key);
            SLHDSASigner verifier = new SLHDSASigner();
            verifier.init(false, params);
            return verifier.verifySignature(message, signature.toBytes());
        } catch (Exception e) {
            Logger.warn("SLH-DSA signature verification failed", e);
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Arrays.equals(key, ((SlhDsaSha2128sPublicKey) obj).key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
