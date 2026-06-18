package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.utils.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Represents a multi-signature for Ed25519 keys.
 *
 * <p>This class implements a multi-signature scheme that combines multiple Ed25519
 * signatures with a bitmap indicating which public keys participated in the signing.
 * The bitmap is a 4-byte array where each bit represents whether a corresponding
 * public key signed the message.</p>
 *
 * <p>The multi-signature format follows the Aptos specification:
 * <ul>
 *   <li>Vector of signatures (each 64 bytes)</li>
 *   <li>4-byte bitmap indicating which keys signed</li>
 * </ul></p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<Signature> signatures = Arrays.asList(sig1, sig2, sig3);
 * List<Integer> signerIndices = Arrays.asList(0, 2, 5);
 * byte[] bitmap = MultiEd25519Signature.createBitmap(signerIndices);
 * MultiEd25519Signature multiSig = new MultiEd25519Signature(signatures, bitmap);
 * }</pre>
 *
 * @author rrigoni (rrigoni@gmail.com)
 * @see <a href="https://github.com/rrigoni">GitHub Profile</a>
 * @see Signature
 * @see Serializable
 * @since 1.0.0
 */
public class MultiEd25519Signature extends Signature implements Serializable {

    /**
     * The length of the bitmap in bytes.
     */
    public static final int BITMAP_LEN = 4;

    private final List<Signature> signatures;
    private final byte[] bitmap;

    /**
     * Constructs a new MultiEd25519Signature with the specified signatures and bitmap.
     *
     * <p>The bitmap must be exactly 4 bytes long and indicate which public keys
     * participated in the signing process.</p>
     *
     * @param signatures the list of individual signatures
     * @param bitmap     the 4-byte bitmap indicating which keys signed
     * @throws IllegalArgumentException if bitmap length is incorrect
     */
    public MultiEd25519Signature(List<Signature> signatures, byte[] bitmap) {
        super(Signature.fromBytes(new byte[LENGTH]).toBytes()); // Dummy signature for base class, actual signature is in 'signatures'
        if (bitmap.length != BITMAP_LEN) {
            throw new IllegalArgumentException("Bitmap must be exactly " + BITMAP_LEN + " bytes");
        }
        this.signatures = signatures;
        this.bitmap = bitmap;
    }

    /**
     * Creates a bitmap from a list of bit positions.
     *
     * <p>This method creates a 4-byte bitmap where each bit position in the
     * input list is set to 1. The bitmap uses MSB-first (Most Significant Bit first)
     * ordering within each byte.</p>
     *
     * <p>For example, if the input is [0, 2, 5], the bitmap will have bits 0, 2, and 5
     * set to 1, with the result being a 4-byte array where the appropriate bits are set.</p>
     *
     * @param bits the list of bit positions to set (0-31)
     * @return a 4-byte bitmap with the specified bits set
     * @throws IllegalArgumentException if any bit position is out of range
     */
    public static byte[] createBitmap(List<Integer> bits) {
        byte[] bitmap = new byte[BITMAP_LEN];
        for (int bit : bits) {
            if (bit < 0 || bit >= 32) {
                throw new IllegalArgumentException("Bit position must be between 0 and 31.");
            }
            int byteOffset = bit / 8;
            int bitOffsetInByte = 7 - (bit % 8); // MSB first
            bitmap[byteOffset] |= (1 << bitOffsetInByte);
        }
        return bitmap;
    }

    /**
     * Serializes the multi-signature to BCS format.
     *
     * <p>The serialization format is:
     * <ul>
     *   <li>ULEB128 length of signatures vector</li>
     *   <li>Fixed 64-byte signatures (one for each signature)</li>
     *   <li>4-byte bitmap (raw bytes, not length-prefixed)</li>
     * </ul></p>
     *
     * @param serializer the serializer to write to
     * @throws IOException if serialization fails
     * @see Serializer
     */
    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeU32AsUleb128(signatures.size());
        for (Signature sig : signatures) {
            serializer.serializeFixedBytes(sig.toBytes());
        }
        serializer.serializeFixedBytes(bitmap); // Bitmap is raw bytes, not length-prefixed
    }

    /**
     * Gets the list of individual signatures.
     *
     * @return the list of signatures
     * @see Signature
     */
    public List<Signature> getSignatures() {
        return signatures;
    }

    /**
     * Gets the bitmap indicating which keys signed.
     *
     * <p>The bitmap is a 4-byte array where each bit represents whether a
     * corresponding public key participated in the signing.</p>
     *
     * @return the 4-byte bitmap
     */
    public byte[] getBitmap() {
        return bitmap;
    }

    /**
     * Gets the number of signatures in this multi-signature.
     *
     * @return the number of signatures
     */
    public int getSignatureCount() {
        return signatures.size();
    }

    /**
     * Gets a specific signature by index.
     *
     * @param index the index of the signature to retrieve
     * @return the signature at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Signature getSignature(int index) {
        return signatures.get(index);
    }

    /**
     * Converts the multi-signature to a byte array.
     *
     * <p>This method serializes the multi-signature using BCS format and returns
     * the resulting byte array.</p>
     *
     * @return the serialized multi-signature as a byte array
     * @throws RuntimeException if serialization fails
     * @see #serialize(Serializer)
     */
    public byte[] toBytes() {
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            return serializer.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize MultiEd25519Signature", e);
            throw new RuntimeException("Failed to serialize MultiEd25519Signature", e);
        }
    }

    /**
     * Returns a string representation of this multi-signature.
     *
     * <p>The string includes the number of signatures and a hex representation
     * of the bitmap.</p>
     *
     * @return a string representation of this multi-signature
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MultiEd25519Signature{");
        sb.append("signatures=").append(signatures.size());
        sb.append(", bitmap=");
        for (byte b : bitmap) {
            sb.append(String.format("%02x", b));
        }
        sb.append("}");
        return sb.toString();
    }
}
