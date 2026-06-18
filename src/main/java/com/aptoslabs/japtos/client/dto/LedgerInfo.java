package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents ledger information from the Aptos API
 */
public class LedgerInfo {
    @SerializedName("chain_id")
    private final int chainId;

    @SerializedName("ledger_version")
    private final String ledgerVersion;

    @SerializedName("ledger_timestamp")
    private final String ledgerTimestamp;

    @SerializedName("epoch")
    private final String epoch;

    @SerializedName("oldest_block_height")
    private final String oldestBlockHeight;

    @SerializedName("oldest_block_timestamp")
    private final String oldestBlockTimestamp;

    @SerializedName("node_role")
    private final String nodeRole;

    @SerializedName("oldest_ledger_timestamp")
    private final String oldestLedgerTimestamp;

    @SerializedName("block_height")
    private final String blockHeight;

    public LedgerInfo(
            int chainId,
            String ledgerVersion,
            String ledgerTimestamp,
            String epoch,
            String oldestBlockHeight,
            String oldestBlockTimestamp,
            String nodeRole,
            String oldestLedgerTimestamp,
            String blockHeight
    ) {
        this.chainId = chainId;
        this.ledgerVersion = ledgerVersion;
        this.ledgerTimestamp = ledgerTimestamp;
        this.epoch = epoch;
        this.oldestBlockHeight = oldestBlockHeight;
        this.oldestBlockTimestamp = oldestBlockTimestamp;
        this.nodeRole = nodeRole;
        this.oldestLedgerTimestamp = oldestLedgerTimestamp;
        this.blockHeight = blockHeight;
    }

    public int getChainId() {
        return chainId;
    }

    public long getLedgerVersion() {
        return Long.parseLong(ledgerVersion);
    }

    public long getLedgerTimestamp() {
        return Long.parseLong(ledgerTimestamp);
    }

    public long getEpoch() {
        return Long.parseLong(epoch);
    }

    public long getOldestBlockHeight() {
        return Long.parseLong(oldestBlockHeight);
    }

    public long getOldestBlockTimestamp() {
        return Long.parseLong(oldestBlockTimestamp);
    }

    public String getNodeRole() {
        return nodeRole;
    }

    public long getOldestLedgerTimestamp() {
        return Long.parseLong(oldestLedgerTimestamp);
    }

    public long getBlockHeight() {
        return Long.parseLong(blockHeight);
    }
} 