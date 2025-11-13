package com.aptoslabs.japtos.bcs;

import com.aptoslabs.japtos.utils.Logger;

import com.aptoslabs.japtos.utils.HexUtils;

import java.io.IOException;

/**
 * Base interface for all types that can be serialized using BCS (Binary Canonical Serialization).
 *
 * <p>BCS is the canonical serialization format used throughout the Aptos ecosystem
 * for encoding data structures, transactions, and other blockchain objects. It provides
 * a deterministic, compact, and efficient binary representation that ensures
 * consistency across different implementations and platforms.</p>
 *
 * <p>Key characteristics of BCS:</p>
 * <ul>
 *   <li><strong>Canonical</strong> - Unique representation for any given data</li>
 *   <li><strong>Deterministic</strong> - Same input always produces same output</li>
 *   <li><strong>Compact</strong> - Minimal overhead and efficient encoding</li>
 *   <li><strong>Type-safe</strong> - Preserves type information and structure</li>
 * </ul>
 *
 * <p>This interface provides both low-level serialization control through the
 * {@link #serialize(Serializer)} method and convenient high-level methods for
 * common use cases like converting to byte arrays or hexadecimal strings.</p>
 *
 * <p>Common usage patterns:</p>
 * <pre>{@code
 * // Direct byte serialization
 * Serializable obj = // ... create serializable object
 * byte[] bytes = obj.bcsToBytes();
 *
 * // Hex string representation
 * String hex = obj.bcsToHex();          // without '0x' prefix
 * String hexString = obj.bcsToString(); // with '0x' prefix
 *
 * // Custom serialization control
 * Serializer serializer = new Serializer();
 * obj.serialize(serializer);
 * byte[] customBytes = serializer.toByteArray();
 * }</pre>
 *
 * @see Serializer
 * @see <a href="https://github.com/diem/bcs">BCS Specification</a>
 * @since 1.0.0
 */
public interface Serializable {

    /**
     * Serializes this object using the provided BCS serializer.
     *
     * <p>This is the core serialization method that must be implemented by all
     * BCS-serializable types. It defines how the object's data is written to
     * the serializer in the canonical BCS format.</p>
     *
     * <p>Implementations should serialize fields in a deterministic order and
     * use the appropriate serializer methods for each data type to ensure
     * BCS compliance.</p>
     *
     * @param serializer the BCS serializer to write to
     * @throws IOException if serialization fails
     */
    void serialize(Serializer serializer) throws IOException;

    /**
     * Serializes this object to a BCS byte array.
     *
     * <p>This convenience method creates a new serializer, calls {@link #serialize(Serializer)},
     * and returns the resulting byte array. This is the most common way to get the
     * BCS representation of an object.</p>
     *
     * @return the BCS-serialized object as a byte array
     * @throws RuntimeException if serialization fails
     */
    default byte[] bcsToBytes() {
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            return serializer.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize object", e);
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    /**
     * Converts this object to a hexadecimal string without '0x' prefix.
     *
     * <p>This method first serializes the object to BCS bytes, then converts
     * the bytes to a lowercase hexadecimal string. This format is commonly
     * used in APIs and configuration files.</p>
     *
     * @return the BCS-serialized object as a hex string (no '0x' prefix)
     */
    default String bcsToHex() {
        return HexUtils.bytesToHex(bcsToBytes());
    }

    /**
     * Converts this object to a hexadecimal string with '0x' prefix.
     *
     * <p>This method provides a blockchain-standard representation of the object
     * with the '0x' prefix indicating hexadecimal format. This is the preferred
     * format for displaying serialized objects to users.</p>
     *
     * @return the BCS-serialized object as a hex string with '0x' prefix
     */
    default String bcsToString() {
        return "0x" + bcsToHex();
    }
}
