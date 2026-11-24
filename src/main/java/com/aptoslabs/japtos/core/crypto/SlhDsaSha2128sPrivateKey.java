package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.utils.Bip44Utils;
import com.aptoslabs.japtos.utils.HexUtils;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.SLHDSAKeyPairGenerator;
import org.bouncycastle.crypto.params.SLHDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.SLHDSAParameters;
import org.bouncycastle.crypto.params.SLHDSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.SLHDSAPublicKeyParameters;
import org.bouncycastle.crypto.signers.SLHDSASigner;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * SLH-DSA-SHA2-128s private key (AIP-137 post-quantum scheme).
 *
 * <p>The serialized private-key material is the 48-byte "three seeds"
 * (SK seed || PRF seed || PK seed, 16 bytes each), matching the TypeScript SDK
 * ({@code @noble/post-quantum}). BouncyCastle's FIPS 205 implementation derives
 * the 64-byte secret key (three seeds || PK root) and 32-byte public key
 * (PK seed || PK root) from those seeds.</p>
 *
 * <p>Signing uses FIPS 205 deterministic mode; verification accepts both
 * deterministic and hedged signatures.</p>
 *
 * <p>AIP-80 string form: {@code slh-dsa-sha2-128s-priv-0x<hex>}.</p>
 */
public class SlhDsaSha2128sPrivateKey implements Serializable {
    public static final int LENGTH = 48;
    public static final String SLIP_0010_SEED = Bip44Utils.SLH_DSA_SHA2_128S_SEED;
    public static final String AIP80_PREFIX = "slh-dsa-sha2-128s-priv-";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] threeSeeds;
    private final SLHDSAPrivateKeyParameters bcPrivateKey;
    private final SlhDsaSha2128sPublicKey publicKey;

    private SlhDsaSha2128sPrivateKey(byte[] threeSeeds) {
        if (threeSeeds == null || threeSeeds.length != LENGTH) {
            throw new IllegalArgumentException("PrivateKey length should be " + LENGTH);
        }
        this.threeSeeds = Arrays.copyOf(threeSeeds, threeSeeds.length);
        // Expand the three 16-byte seeds into the full FIPS 205 key pair (derives PK root).
        byte[] skSeed = Arrays.copyOfRange(threeSeeds, 0, 16);
        byte[] prfSeed = Arrays.copyOfRange(threeSeeds, 16, 32);
        byte[] pkSeed = Arrays.copyOfRange(threeSeeds, 32, 48);
        SLHDSAKeyPairGenerator generator = new SLHDSAKeyPairGenerator();
        generator.init(new SLHDSAKeyGenerationParameters(SECURE_RANDOM, SLHDSAParameters.sha2_128s));
        AsymmetricCipherKeyPair pair = generator.internalGenerateKeyPair(skSeed, prfSeed, pkSeed);
        this.bcPrivateKey = (SLHDSAPrivateKeyParameters) pair.getPrivate();
        this.publicKey = SlhDsaSha2128sPublicKey.fromBytes(
                ((SLHDSAPublicKeyParameters) pair.getPublic()).getEncoded());
    }

    public static SlhDsaSha2128sPrivateKey fromBytes(byte[] key) {
        return new SlhDsaSha2128sPrivateKey(key);
    }

    /**
     * Parses a private key from raw hex (with optional {@code 0x}) or AIP-80 format
     * ({@code slh-dsa-sha2-128s-priv-0x<hex>}).
     */
    public static SlhDsaSha2128sPrivateKey fromHex(String hex) {
        return fromHex(hex, false);
    }

    /**
     * Parses a private key from a hex or AIP-80 string.
     *
     * @param hex    the private key string
     * @param strict if {@code true}, requires the AIP-80 prefix ({@code slh-dsa-sha2-128s-priv-0x...})
     */
    public static SlhDsaSha2128sPrivateKey fromHex(String hex, boolean strict) {
        if (hex == null) {
            throw new IllegalArgumentException("PrivateKey hex string cannot be null");
        }
        String value = hex.trim();
        if (value.startsWith(AIP80_PREFIX)) {
            value = value.substring(AIP80_PREFIX.length());
            if (strict && !value.startsWith("0x") && !value.startsWith("0X")) {
                throw new IllegalArgumentException(
                        "Invalid HexString input while parsing private key. Must be AIP-80 compliant string.");
            }
        } else if (strict) {
            throw new IllegalArgumentException(
                    "Invalid HexString input while parsing private key. Must be AIP-80 compliant string.");
        }
        return fromBytes(HexUtils.hexToBytes(value));
    }

    public static SlhDsaSha2128sPrivateKey generate() {
        byte[] seeds = new byte[LENGTH];
        SECURE_RANDOM.nextBytes(seeds);
        return fromBytes(seeds);
    }

    /**
     * Hardened-only HD derivation from a mnemonic. Mirrors the TypeScript SDK: any
     * fully-hardened path is accepted (BIP44 Aptos paths such as
     * {@code m/44'/637'/0'/0'/0'} satisfy this).
     */
    public static SlhDsaSha2128sPrivateKey fromDerivationPath(String path, String mnemonic) {
        if (!Bip44Utils.isValidGenericHardenedPath(path)) {
            throw new IllegalArgumentException("Invalid derivation path " + path);
        }
        return fromBytes(Bip44Utils.deriveSlhDsaSha2128sThreeSeeds(path, mnemonic));
    }

    /** Seed-based derivation entry point mirroring the TypeScript SDK's {@code fromDerivationPathInner}. */
    public static SlhDsaSha2128sPrivateKey fromDerivationPathInner(String path, byte[] seed) {
        return fromBytes(Bip44Utils.deriveSlhDsaSha2128sThreeSeedsFromSeed(path, seed));
    }

    public SlhDsaSha2128sSignature sign(byte[] message) {
        if (message == null) {
            throw new IllegalArgumentException("Message to sign cannot be null");
        }
        SLHDSASigner signer = new SLHDSASigner();
        signer.init(true, bcPrivateKey);
        return SlhDsaSha2128sSignature.fromBytes(signer.generateSignature(message));
    }

    public SlhDsaSha2128sPublicKey publicKey() {
        return publicKey;
    }

    public byte[] toBytes() {
        return Arrays.copyOf(threeSeeds, threeSeeds.length);
    }

    public String toHexString() {
        return HexUtils.bytesToHex(threeSeeds);
    }

    @Override
    public String toString() {
        return toAIP80String();
    }

    /** AIP-80 form, e.g. {@code slh-dsa-sha2-128s-priv-0x<96 hex chars>}. */
    public String toAIP80String() {
        return AIP80_PREFIX + "0x" + toHexString();
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeBytes(threeSeeds);
    }

    public static SlhDsaSha2128sPrivateKey deserialize(Deserializer deserializer) throws IOException {
        return fromBytes(deserializer.deserializeBytes());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Arrays.equals(threeSeeds, ((SlhDsaSha2128sPrivateKey) obj).threeSeeds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(threeSeeds);
    }
}
