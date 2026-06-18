package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a pending transaction response
 */
public class PendingTransaction {
    @SerializedName("hash")
    private final String hash;

    @SerializedName("sender")
    private final String sender;

    @SerializedName("sequence_number")
    private final String sequenceNumber;

    @SerializedName("max_gas_amount")
    private final String maxGasAmount;

    @SerializedName("gas_unit_price")
    private final String gasUnitPrice;

    @SerializedName("expiration_timestamp_secs")
    private final String expirationTimestampSecs;

    @SerializedName("payload")
    private final Object payload;

    @SerializedName("signature")
    private final Object signature;

    /**
     * Constructor accepting only hash (useful for gas station responses).
     */
    public PendingTransaction(String hash) {
        this(hash, null, null, null, null, null, null, null);
    }

    public PendingTransaction(
            String hash,
            String sender,
            String sequenceNumber,
            String maxGasAmount,
            String gasUnitPrice,
            String expirationTimestampSecs,
            Object payload,
            Object signature
    ) {
        this.hash = hash;
        this.sender = sender;
        this.sequenceNumber = sequenceNumber;
        this.maxGasAmount = maxGasAmount;
        this.gasUnitPrice = gasUnitPrice;
        this.expirationTimestampSecs = expirationTimestampSecs;
        this.payload = payload;
        this.signature = signature;
    }

    public String getHash() {
        return hash;
    }

    public String getSender() {
        return sender;
    }

    public long getSequenceNumber() {
        return Long.parseLong(sequenceNumber);
    }

    public long getMaxGasAmount() {
        return Long.parseLong(maxGasAmount);
    }

    public long getGasUnitPrice() {
        return Long.parseLong(gasUnitPrice);
    }

    public long getExpirationTimestampSecs() {
        return Long.parseLong(expirationTimestampSecs);
    }

    public Object getPayload() {
        return payload;
    }

    public Object getSignature() {
        return signature;
    }
} 