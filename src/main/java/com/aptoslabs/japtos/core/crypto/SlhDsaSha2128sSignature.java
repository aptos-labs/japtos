package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.utils.HexUtils;

import java.io.IOException;

/**
 * SLH-DSA-SHA2-128s signature (FIPS 205 / SPHINCS+ SHA2-128s), as added by AIP-137.
 * Signatures are fixed at 7,856 bytes.
 *
 * <p>Wire format matches the TypeScript SDK ({@code SlhDsaSha2128sSignature}) and
 * aptos-core: BCS {@code serializeBytes} (ULEB128 length prefix followed by the raw
 * signature bytes), carried inside {@link AnySignature} at variant
 * {@link AnySignature#VARIANT_SLH_DSA_SHA2_128S}.</p>
 */
public class SlhDsaSha2128sSignature extends Signature implements Serializable {
    public static final int LENGTH = 7856;

    private SlhDsaSha2128sSignature(byte[] signature) {
        super(signature, LENGTH);
    }

    public static SlhDsaSha2128sSignature fromBytes(byte[] signature) {
        return new SlhDsaSha2128sSignature(signature);
    }

    public static SlhDsaSha2128sSignature fromHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Signature hex string cannot be null");
        }
        return fromBytes(HexUtils.hexToBytes(hex));
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeBytes(toBytes());
    }

    public static SlhDsaSha2128sSignature deserialize(Deserializer deserializer) throws IOException {
        return fromBytes(deserializer.deserializeBytes());
    }
}
