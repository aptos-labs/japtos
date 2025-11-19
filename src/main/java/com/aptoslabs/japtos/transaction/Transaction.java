package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.TransactionPayload;
import com.aptoslabs.japtos.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents an Aptos transaction with simplified serialization interface.
 *
 * <p>This class provides a lightweight representation of an Aptos transaction
 * with basic serialization capabilities. It contains the same core transaction
 * parameters as RawTransaction but with a more direct serialization approach.</p>
 *
 * <p><strong>Note:</strong> For standard transaction signing and submission workflows,
 * prefer using {@link RawTransaction} and {@link SignedTransaction} which provide
 * more comprehensive signature handling and authentication support.</p>
 *
 * <p>Key components:</p>
 * <ul>
 *   <li><strong>Sender</strong> - The account address initiating the transaction</li>
 *   <li><strong>Sequence Number</strong> - For replay protection and ordering</li>
 *   <li><strong>Payload</strong> - The transaction content (script, module, entry function)</li>
 *   <li><strong>Gas Parameters</strong> - Maximum gas amount and unit price</li>
 *   <li><strong>Expiration</strong> - Unix timestamp when transaction expires</li>
 *   <li><strong>Chain ID</strong> - Target blockchain identifier</li>
 * </ul>
 *
 * <p>This class is primarily useful for:</p>
 * <ul>
 *   <li>Direct BCS serialization of transaction data</li>
 *   <li>Testing and development scenarios</li>
 *   <li>Custom transaction handling workflows</li>
 * </ul>
 *
 * @see RawTransaction
 * @see SignedTransaction
 * @see TransactionPayload
 * @see AccountAddress
 * @since 1.0.0
 */
public class Transaction {
    private final AccountAddress sender;
    private final long sequenceNumber;
    private final TransactionPayload payload;
    private final long maxGasAmount;
    private final long gasUnitPrice;
    private final long expirationTimestampSecs;
    private final long chainId;

    /**
     * Creates a new Transaction with the specified parameters.
     *
     * @param sender                  the account address initiating the transaction
     * @param sequenceNumber          the sequence number for replay protection and ordering
     * @param payload                 the transaction payload (script, module, or entry function)
     * @param maxGasAmount            the maximum amount of gas units to consume
     * @param gasUnitPrice            the price per gas unit in octas
     * @param expirationTimestampSecs the Unix timestamp when transaction expires
     * @param chainId                 the identifier of the target blockchain
     */
    public Transaction(
            AccountAddress sender,
            long sequenceNumber,
            TransactionPayload payload,
            long maxGasAmount,
            long gasUnitPrice,
            long expirationTimestampSecs,
            long chainId
    ) {
        this.sender = sender;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.maxGasAmount = maxGasAmount;
        this.gasUnitPrice = gasUnitPrice;
        this.expirationTimestampSecs = expirationTimestampSecs;
        this.chainId = chainId;
    }

    /**
     * Serializes this transaction to BCS (Binary Canonical Serialization) bytes.
     *
     * <p>This method provides a convenient way to get the complete BCS-serialized
     * representation of the transaction as a byte array. The serialization follows
     * the Aptos BCS specification.</p>
     *
     * @return the BCS-serialized transaction as a byte array
     * @throws RuntimeException if serialization fails
     */
    public byte[] toBcsBytes() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Serializer serializer = new Serializer(outputStream);
            serialize(serializer);
            return outputStream.toByteArray();
        } catch (IOException e) {
            Logger.error("Failed to serialize transaction", e);
            throw new RuntimeException("Failed to serialize transaction", e);
        }
    }

    /**
     * Serializes this transaction using the provided BCS serializer.
     *
     * <p>The serialization format follows the Aptos specification with the same
     * field order as RawTransaction. This method is used internally by toBcsBytes()
     * and can be used when custom serialization control is needed.</p>
     *
     * @param serializer the BCS serializer to write to
     * @throws IOException if serialization fails
     */
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeAccountAddress(sender);
        serializer.serializeU64(sequenceNumber);
        payload.serialize(serializer);
        serializer.serializeU64(maxGasAmount);
        serializer.serializeU64(gasUnitPrice);
        serializer.serializeU64(expirationTimestampSecs);
        serializer.serializeU8((byte) chainId); // Chain ID is u8 in Aptos
    }

    public AccountAddress getSender() {
        return sender;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public TransactionPayload getPayload() {
        return payload;
    }

    public long getMaxGasAmount() {
        return maxGasAmount;
    }

    public long getGasUnitPrice() {
        return gasUnitPrice;
    }

    public long getExpirationTimestampSecs() {
        return expirationTimestampSecs;
    }

    public long getChainId() {
        return chainId;
    }
} 