package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.TransactionPayload;

import java.io.IOException;

/**
 * Represents a raw (unsigned) Aptos transaction before signature application.
 *
 * <p>A RawTransaction contains all the essential information needed to execute
 * a transaction on the Aptos blockchain, including the sender, sequence number,
 * payload, gas parameters, expiration time, and target chain. This class
 * implements BCS serialization to generate the canonical byte representation
 * that is signed by the sender's private key.</p>
 *
 * <p>Key components:</p>
 * <ul>
 *   <li><strong>Sender</strong> - The account address initiating the transaction</li>
 *   <li><strong>Sequence Number</strong> - Prevents replay attacks and ensures ordering</li>
 *   <li><strong>Payload</strong> - The transaction content (script, module, entry function)</li>
 *   <li><strong>Gas Parameters</strong> - Maximum gas amount and unit price</li>
 *   <li><strong>Expiration</strong> - Unix timestamp when transaction expires</li>
 *   <li><strong>Chain ID</strong> - Target blockchain identifier</li>
 * </ul>
 *
 * <p>Transaction Lifecycle:</p>
 * <ol>
 *   <li>Create RawTransaction with all parameters</li>
 *   <li>Serialize to canonical byte representation</li>
 *   <li>Sign the serialized bytes with sender's private key</li>
 *   <li>Create SignedTransaction with raw transaction + signature</li>
 *   <li>Submit SignedTransaction to the network</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a raw transaction
 * RawTransaction rawTx = new RawTransaction(
 *     AccountAddress.fromHex("0x1234..."),  // sender
 *     42,                                   // sequence number
 *     payload,                              // transaction payload
 *     100_000,                              // max gas amount
 *     100,                                  // gas unit price
 *     System.currentTimeMillis() / 1000 + 600,  // expires in 10 minutes
 *     1                                     // mainnet chain ID
 * );
 *
 * // Sign the transaction
 * Ed25519PrivateKey privateKey = // ... obtain private key
 * Signature signature = privateKey.signTransaction(rawTx);
 * SignedTransaction signedTx = new SignedTransaction(rawTx, signature);
 * }</pre>
 *
 * @see SignedTransaction
 * @see TransactionPayload
 * @see AccountAddress
 * @see Serializable
 * @since 1.0.0
 */
public class RawTransaction implements Serializable {

    public static final long DEFAULT_MAX_GAS = 1000000L;
    public static final long DEFAULT_GAS_PRICE = 100L;

    private final AccountAddress sender;
    private final long sequenceNumber;
    private final TransactionPayload payload;
    private final long maxGasAmount;
    private final long gasUnitPrice;
    private final long expirationTimestampSecs;
    private final long chainId;

    /**
     * Creates a new RawTransaction with the specified parameters.
     *
     * @param sender                  the account address initiating the transaction
     * @param sequenceNumber          the sequence number for replay protection and ordering
     * @param payload                 the transaction payload (script, module, or entry function)
     * @param maxGasAmount            the maximum amount of gas units to consume
     * @param gasUnitPrice            the price per gas unit in octas
     * @param expirationTimestampSecs the Unix timestamp when transaction expires
     * @param chainId                 the identifier of the target blockchain
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public RawTransaction(
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
     * Creates a new RawTransaction with the specified parameters.
     *
     * @param sender                  the account address initiating the transaction
     * @param sequenceNumber          the sequence number for replay protection and ordering
     * @param payload                 the transaction payload (script, module, or entry function)
     * @param expirationTimestampSecs the Unix timestamp when transaction expires
     * @param chainId                 the identifier of the target blockchain
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public RawTransaction(
            AccountAddress sender,
            long sequenceNumber,
            TransactionPayload payload,
            long expirationTimestampSecs,
            long chainId
    ) {
        this.sender = sender;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.maxGasAmount = DEFAULT_MAX_GAS;
        this.gasUnitPrice = DEFAULT_GAS_PRICE;
        this.expirationTimestampSecs = expirationTimestampSecs;
        this.chainId = chainId;
    }

    /**
     * Serializes this RawTransaction using BCS (Binary Canonical Serialization).
     *
     * <p>The serialization format follows the Aptos specification:
     * <ol>
     *   <li>Sender address (32 bytes)</li>
     *   <li>Sequence number (8 bytes, little-endian)</li>
     *   <li>Transaction payload (variable length)</li>
     *   <li>Max gas amount (8 bytes, little-endian)</li>
     *   <li>Gas unit price (8 bytes, little-endian)</li>
     *   <li>Expiration timestamp (8 bytes, little-endian)</li>
     *   <li>Chain ID (1 byte)</li>
     * </ol>
     *
     * <p>This serialized form is what gets signed by the sender's private key.</p>
     *
     * @param serializer the BCS serializer to write to
     * @throws IOException if serialization fails
     */
    @Override
    public void serialize(Serializer serializer) throws IOException {
        serializer.serializeAccountAddress(sender);
        serializer.serializeU64(sequenceNumber);
        payload.serialize(serializer);
        serializer.serializeU64(maxGasAmount);
        serializer.serializeU64(gasUnitPrice);
        serializer.serializeU64(expirationTimestampSecs);
        serializer.serializeU8((byte) chainId);
    }

    /**
     * Returns the account address that initiated this transaction.
     *
     * @return the sender's account address
     */
    public AccountAddress getSender() {
        return sender;
    }

    /**
     * Returns the sequence number for this transaction.
     *
     * <p>The sequence number prevents replay attacks and ensures transaction
     * ordering. It must match the sender's current sequence number on-chain.</p>
     *
     * @return the transaction sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Returns the transaction payload containing the actual transaction content.
     *
     * <p>The payload can be a script, module deployment, or entry function call
     * that defines what this transaction will execute on the blockchain.</p>
     *
     * @return the transaction payload
     * @see TransactionPayload
     */
    public TransactionPayload getPayload() {
        return payload;
    }

    /**
     * Returns the maximum amount of gas units this transaction is allowed to consume.
     *
     * <p>If the transaction execution requires more gas than this limit,
     * the transaction will fail and be aborted.</p>
     *
     * @return the maximum gas amount in gas units
     */
    public long getMaxGasAmount() {
        return maxGasAmount;
    }

    /**
     * Returns the price per gas unit for this transaction.
     *
     * <p>The gas unit price is specified in octas (the smallest unit of APT).
     * Higher prices may result in faster transaction processing.</p>
     *
     * @return the gas unit price in octas
     */
    public long getGasUnitPrice() {
        return gasUnitPrice;
    }

    /**
     * Returns the expiration timestamp for this transaction.
     *
     * <p>The transaction will be rejected if it's submitted after this
     * Unix timestamp (in seconds). This prevents old transactions from
     * being executed unexpectedly.</p>
     *
     * @return the expiration timestamp in Unix seconds
     */
    public long getExpirationTimestampSecs() {
        return expirationTimestampSecs;
    }

    /**
     * Returns the chain ID for this transaction.
     *
     * <p>The chain ID identifies the target blockchain (e.g., 1 for mainnet,
     * 2 for testnet). This prevents transactions from being replayed on
     * different networks.</p>
     *
     * @return the chain ID
     */
    public long getChainId() {
        return chainId;
    }

    @Override
    public String toString() {
        return "RawTransaction{" +
                "sender=" + sender +
                ", sequenceNumber=" + sequenceNumber +
                ", payload=" + payload +
                ", maxGasAmount=" + maxGasAmount +
                ", gasUnitPrice=" + gasUnitPrice +
                ", expirationTimestampSecs=" + expirationTimestampSecs +
                ", chainId=" + chainId +
                '}';
    }
} 