package com.aptoslabs.japtos.client.dto;

import com.aptoslabs.japtos.utils.Logger;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a transaction from the Aptos API
 */
public class Transaction {
    @SerializedName("version")
    private final String version;

    @SerializedName("hash")
    private final String hash;

    @SerializedName("state_change_hash")
    private final String stateChangeHash;

    @SerializedName("event_root_hash")
    private final String eventRootHash;

    @SerializedName("state_checkpoint_hash")
    private final String stateCheckpointHash;

    @SerializedName("gas_used")
    private final String gasUsed;

    @SerializedName("success")
    private final boolean success;

    @SerializedName("vm_status")
    private final String vmStatus;

    @SerializedName("accumulator_root_hash")
    private final String accumulatorRootHash;

    @SerializedName("changes")
    private final Object[] changes;

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

    @SerializedName("events")
    private final Object[] events;

    @SerializedName("timestamp")
    private final String timestamp;

    @SerializedName("type")
    private final String type;

    public Transaction(
            String version,
            String hash,
            String stateChangeHash,
            String eventRootHash,
            String stateCheckpointHash,
            String gasUsed,
            boolean success,
            String vmStatus,
            String accumulatorRootHash,
            Object[] changes,
            String sender,
            String sequenceNumber,
            String maxGasAmount,
            String gasUnitPrice,
            String expirationTimestampSecs,
            Object payload,
            Object signature,
            Object[] events,
            String timestamp,
            String type
    ) {
        this.version = version;
        this.hash = hash;
        this.stateChangeHash = stateChangeHash;
        this.eventRootHash = eventRootHash;
        this.stateCheckpointHash = stateCheckpointHash;
        this.gasUsed = gasUsed;
        this.success = success;
        this.vmStatus = vmStatus;
        this.accumulatorRootHash = accumulatorRootHash;
        this.changes = changes;
        this.sender = sender;
        this.sequenceNumber = sequenceNumber;
        this.maxGasAmount = maxGasAmount;
        this.gasUnitPrice = gasUnitPrice;
        this.expirationTimestampSecs = expirationTimestampSecs;
        this.payload = payload;
        this.signature = signature;
        this.events = events;
        this.timestamp = timestamp;
        this.type = type;
    }

    public long getVersion() {
        return Long.parseLong(version);
    }

    public String getHash() {
        return hash;
    }

    public String getStateChangeHash() {
        return stateChangeHash;
    }

    public String getEventRootHash() {
        return eventRootHash;
    }

    public String getStateCheckpointHash() {
        return stateCheckpointHash;
    }

    public long getGasUsed() {
        if (gasUsed == null || gasUsed.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(gasUsed);
        } catch (NumberFormatException e) {
            Logger.debug("Failed to parse gas used: %s", gasUsed);
            return 0L;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public String getVmStatus() {
        return vmStatus;
    }

    public String getAccumulatorRootHash() {
        return accumulatorRootHash;
    }

    public Object[] getChanges() {
        return changes;
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

    public Object[] getEvents() {
        return events;
    }

    public long getTimestamp() {
        return Long.parseLong(timestamp);
    }

    public String getType() {
        return type;
    }
} 