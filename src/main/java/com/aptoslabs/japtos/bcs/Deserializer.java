package com.aptoslabs.japtos.bcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BCS (Binary Canonical Serialization) deserializer for reading Aptos blockchain types.
 * Matches the TypeScript SDK's Deserializer implementation.
 */
public class Deserializer {
    private final ByteArrayInputStream buffer;
    
    public Deserializer(byte[] data) {
        this.buffer = new ByteArrayInputStream(data);
    }
    
    /**
     * Deserializes a string (length-prefixed UTF-8 bytes).
     *
     * @return the deserialized string
     * @throws IOException if deserialization fails
     */
    public String deserializeString() throws IOException {
        byte[] bytes = deserializeBytes();
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Deserializes a byte array (length-prefixed).
     *
     * @return the deserialized byte array
     * @throws IOException if deserialization fails
     */
    public byte[] deserializeBytes() throws IOException {
        int length = deserializeUleb128AsU32();
        return deserializeFixedBytes(length);
    }
    
    /**
     * Deserializes a fixed number of bytes.
     *
     * @param length the number of bytes to read
     * @return the deserialized bytes
     * @throws IOException if not enough bytes available
     */
    public byte[] deserializeFixedBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        int read = buffer.read(bytes);
        if (read != length) {
            throw new IOException("Not enough bytes: expected " + length + ", got " + read);
        }
        return bytes;
    }
    
    /**
     * Deserializes a boolean value.
     *
     * @return the deserialized boolean
     * @throws IOException if deserialization fails
     */
    public boolean deserializeBool() throws IOException {
        int value = buffer.read();
        if (value == -1) {
            throw new IOException("Unexpected end of input");
        }
        return value != 0;
    }
    
    /**
     * Deserializes a u8 value.
     *
     * @return the deserialized u8 as a byte
     * @throws IOException if deserialization fails
     */
    public byte deserializeU8() throws IOException {
        int value = buffer.read();
        if (value == -1) {
            throw new IOException("Unexpected end of input");
        }
        return (byte) value;
    }
    
    /**
     * Deserializes a u16 value (little-endian).
     *
     * @return the deserialized u16 as a short
     * @throws IOException if deserialization fails
     */
    public short deserializeU16() throws IOException {
        byte[] bytes = deserializeFixedBytes(2);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
    
    /**
     * Deserializes a u32 value (little-endian).
     *
     * @return the deserialized u32 as an int
     * @throws IOException if deserialization fails
     */
    public int deserializeU32() throws IOException {
        byte[] bytes = deserializeFixedBytes(4);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    /**
     * Deserializes a u64 value (little-endian).
     *
     * @return the deserialized u64 as a long
     * @throws IOException if deserialization fails
     */
    public long deserializeU64() throws IOException {
        byte[] bytes = deserializeFixedBytes(8);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }
    
    /**
     * Deserializes a ULEB128-encoded u32 value.
     *
     * @return the deserialized u32 value
     * @throws IOException if deserialization fails
     */
    public int deserializeUleb128AsU32() throws IOException {
        int value = 0;
        int shift = 0;
        
        while (true) {
            int byte_ = buffer.read();
            if (byte_ == -1) {
                throw new IOException("Unexpected end of input while reading ULEB128");
            }
            
            value |= ((byte_ & 0x7F) << shift);
            
            if ((byte_ & 0x80) == 0) {
                break;
            }
            
            shift += 7;
            if (shift > 31) {
                throw new IOException("ULEB128 value exceeds u32 range");
            }
        }
        
        return value;
    }
    
    /**
     * Gets the number of bytes remaining in the buffer.
     *
     * @return number of unread bytes
     */
    public int remaining() {
        return buffer.available();
    }
}

