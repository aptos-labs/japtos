package com.aptoslabs.japtos.bcs;

import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * BCS (Binary Canonical Serialization) serializer for Aptos blockchain types.
 * 
 * <p>This class provides methods to serialize various data types according to the
 * BCS specification used throughout the Aptos ecosystem. BCS ensures deterministic,
 * canonical binary encoding for all blockchain data structures.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li><strong>Type-safe</strong> - Dedicated methods for each primitive type</li>
 *   <li><strong>Little-endian</strong> - All multi-byte integers use little-endian encoding</li>
 *   <li><strong>ULEB128</strong> - Variable-length encoding for sequence lengths</li>
 *   <li><strong>Deterministic</strong> - Same input always produces identical output</li>
 *   <li><strong>Aptos-aware</strong> - Built-in support for AccountAddress, PublicKey, etc.</li>
 * </ul>
 * 
 * <p>Supported data types:</p>
 * <ul>
 *   <li>Primitive types: bool, u8, u16, u32, u64, u128, u256</li>
 *   <li>Collections: byte arrays, strings, sequences</li>
 *   <li>Aptos types: AccountAddress, PublicKey, Signature</li>
 *   <li>Complex types: any object implementing Serializable</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Serializer serializer = new Serializer();
 * 
 * // Serialize primitive types
 * serializer.serializeU64(42L);
 * serializer.serializeBool(true);
 * serializer.serializeString("Hello, Aptos!");
 * 
 * // Serialize Aptos types
 * AccountAddress address = AccountAddress.fromHex("0x1");
 * serializer.serializeAccountAddress(address);
 * 
 * // Get the result
 * byte[] serializedData = serializer.toByteArray();
 * }</pre>
 * 
 * @see Serializable
 * @see <a href="https://github.com/diem/bcs">BCS Specification</a>
 * @since 1.0.0
 */
public class Serializer {
    private final ByteArrayOutputStream buffer;
    
    /**
     * Creates a new BCS serializer with an internal byte buffer.
     * 
     * <p>This is the standard constructor for most use cases. The serializer
     * will write all data to an internal ByteArrayOutputStream that can be
     * accessed via {@link #toByteArray()}.</p>
     */
    public Serializer() {
        this.buffer = new ByteArrayOutputStream();
    }
    
    /**
     * Creates a new BCS serializer with an internal byte buffer.
     * 
     * <p>Note: The outputStream parameter is currently not used. This constructor
     * exists for API compatibility but behaves identically to the default constructor.</p>
     * 
     * @param outputStream unused parameter (for future compatibility)
     * @deprecated Use the default constructor instead
     */
    @Deprecated
    public Serializer(OutputStream outputStream) {
        this.buffer = new ByteArrayOutputStream();
    }
    
    /**
     * Serializes a byte array with length prefix.
     * 
     * <p>This method serializes the array length as ULEB128 followed by the
     * array contents. This is the standard BCS format for variable-length
     * byte sequences.</p>
     * 
     * @param bytes the byte array to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeBytes(byte[] bytes) throws IOException {
        serializeU32AsUleb128(bytes.length);
        buffer.write(bytes);
    }

    /**
     * Serializes raw bytes without a length prefix.
     * 
     * <p>This method writes the bytes directly to the buffer without any
     * length information. Use this for fixed-length data or when the length
     * is encoded elsewhere.</p>
     * 
     * @param bytes the raw bytes to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeFixedBytes(byte[] bytes) throws IOException {
        buffer.write(bytes);
    }

    /**
     * Serializes raw bytes with length validation.
     * 
     * <p>This method writes the bytes directly to the buffer after verifying
     * they are exactly the expected length. This is useful for fixed-size
     * fields like cryptographic keys or hashes.</p>
     * 
     * @param bytes the raw bytes to serialize
     * @param expectedLength the required exact length
     * @throws IOException if writing to the buffer fails
     * @throws IllegalArgumentException if bytes length doesn't match expected
     */
    public void serializeFixedBytesExact(byte[] bytes, int expectedLength) throws IOException {
        if (bytes.length != expectedLength) {
            throw new IllegalArgumentException("Expected fixed length " + expectedLength + ", got " + bytes.length);
        }
        buffer.write(bytes);
    }
    
    /**
     * Serializes a string as UTF-8 bytes with length prefix.
     * 
     * <p>The string is first converted to UTF-8 bytes, then serialized
     * as a byte array with ULEB128 length prefix according to BCS specification.</p>
     * 
     * @param str the string to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeString(String str) throws IOException {
        serializeBytes(str.getBytes());
    }
    
    /**
     * Serializes a boolean value as a single byte.
     * 
     * <p>Boolean values are encoded as: false = 0x00, true = 0x01.</p>
     * 
     * @param value the boolean value to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeBool(boolean value) throws IOException {
        buffer.write(value ? 1 : 0);
    }
    
    /**
     * Serializes an 8-bit unsigned integer.
     * 
     * <p>The value is written as a single byte. Input values are masked
     * to ensure proper unsigned byte representation.</p>
     * 
     * @param value the u8 value to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeU8(byte value) throws IOException {
        buffer.write(value & 0xFF);
    }
    
    /**
     * Serializes a 16-bit unsigned integer in little-endian format.
     * 
     * <p>The value is written as 2 bytes in little-endian byte order,
     * consistent with BCS specification.</p>
     * 
     * @param value the u16 value to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeU16(short value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(value);
        buffer.write(bb.array());
    }
    
    /**
     * Serializes a 32-bit unsigned integer in little-endian format.
     * 
     * <p>The value is written as 4 bytes in little-endian byte order,
     * consistent with BCS specification.</p>
     * 
     * @param value the u32 value to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeU32(int value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        buffer.write(bb.array());
    }
    
    /**
     * Serializes a 64-bit unsigned integer in little-endian format.
     * 
     * <p>The value is written as 8 bytes in little-endian byte order,
     * consistent with BCS specification.</p>
     * 
     * @param value the u64 value to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeU64(long value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(value);
        buffer.write(bb.array());
    }
    
    /**
     * Serializes a 128-bit unsigned integer from a byte array.
     * 
     * <p>The byte array must be exactly 16 bytes. The bytes are written
     * directly to the buffer in the order provided (typically little-endian).</p>
     * 
     * @param value the u128 value as a 16-byte array
     * @throws IOException if writing to the buffer fails
     * @throws IllegalArgumentException if the array is not exactly 16 bytes
     */
    public void serializeU128(byte[] value) throws IOException {
        if (value.length != 16) {
            throw new IllegalArgumentException("U128 must be exactly 16 bytes");
        }
        buffer.write(value);
    }
    
    /**
     * Serializes a 256-bit unsigned integer from a byte array.
     * 
     * <p>The byte array must be exactly 32 bytes. The bytes are written
     * directly to the buffer in the order provided (typically little-endian).</p>
     * 
     * @param value the u256 value as a 32-byte array
     * @throws IOException if writing to the buffer fails
     * @throws IllegalArgumentException if the array is not exactly 32 bytes
     */
    public void serializeU256(byte[] value) throws IOException {
        if (value.length != 32) {
            throw new IllegalArgumentException("U256 must be exactly 32 bytes");
        }
        buffer.write(value);
    }
    
    /**
     * Serializes a 32-bit unsigned integer using ULEB128 encoding.
     * 
     * <p>ULEB128 (Unsigned Little Endian Base 128) is a variable-length
     * encoding used in BCS for sequence lengths and other size fields.
     * It encodes values using 7 bits per byte with a continuation bit.</p>
     * 
     * @param value the u32 value to encode as ULEB128
     * @throws IOException if writing to the buffer fails
     */
    public void serializeU32AsUleb128(int value) throws IOException {
        while (value >= 0x80) {
            buffer.write((value & 0x7F) | 0x80);
            value >>= 7;
        }
        buffer.write(value & 0x7F);
    }
    
    /**
     * Serializes an Aptos AccountAddress.
     * 
     * <p>AccountAddress is serialized as 32 raw bytes without length prefix,
     * representing the account's unique identifier on the blockchain.</p>
     * 
     * @param address the AccountAddress to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeAccountAddress(AccountAddress address) throws IOException {
        buffer.write(address.toBytes());
    }
    
    /**
     * Serializes a cryptographic PublicKey.
     * 
     * <p>Public keys are serialized as a byte vector with ULEB128 length prefix.
     * The exact format depends on the key type (Ed25519, MultiEd25519, etc.).</p>
     * 
     * @param publicKey the PublicKey to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializePublicKey(PublicKey publicKey) throws IOException {
        // Public keys are serialized as a vector<u8>
        serializeBytes(publicKey.toBytes());
    }
    
    /**
     * Serializes a cryptographic Signature.
     * 
     * <p>Signatures are serialized as a byte vector with ULEB128 length prefix.
     * The exact format depends on the signature type (Ed25519, MultiEd25519, etc.).</p>
     * 
     * @param signature the Signature to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeSignature(Signature signature) throws IOException {
        // Signatures are serialized as a vector<u8>
        serializeBytes(signature.toBytes());
    }
    
    /**
     * Serializes a sequence of Serializable objects.
     * 
     * <p>The sequence is serialized with ULEB128 length prefix followed by
     * each object's serialized representation. All objects in the sequence
     * must implement the Serializable interface.</p>
     * 
     * @param sequence the list of Serializable objects to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeSequence(List<? extends Serializable> sequence) throws IOException {
        serializeU32AsUleb128(sequence.size());
        for (Serializable item : sequence) {
            item.serialize(this);
        }
    }
    
    /**
     * Serializes a sequence of byte arrays.
     * 
     * <p>Each byte array in the sequence is serialized with its own length
     * prefix. The sequence itself also has a ULEB128 length prefix indicating
     * the number of byte arrays.</p>
     * 
     * @param sequence the list of byte arrays to serialize
     * @throws IOException if writing to the buffer fails
     */
    public void serializeSequenceBytes(List<byte[]> sequence) throws IOException {
        serializeU32AsUleb128(sequence.size());
        for (byte[] item : sequence) {
            serializeBytes(item);
        }
    }
    
    /**
     * Returns the complete serialized data as a byte array.
     * 
     * <p>This method returns a copy of all data written to the serializer
     * since its creation. The returned array represents the complete BCS
     * serialization of all previously serialized objects.</p>
     * 
     * @return a copy of the serialized data
     */
    public byte[] toByteArray() {
        return buffer.toByteArray();
    }
    
    /**
     * Returns the current size of the serialization buffer in bytes.
     * 
     * <p>This method returns the total number of bytes that have been
     * written to the serializer so far. It can be useful for monitoring
     * serialization progress or buffer usage.</p>
     * 
     * @return the current buffer size in bytes
     */
    public int size() {
        return buffer.size();
    }
}
