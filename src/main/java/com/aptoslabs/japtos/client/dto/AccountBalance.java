package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single token balance for an account
 */
public class AccountBalance {
    @SerializedName("amount")
    private final String amount;
    
    @SerializedName("asset_type")
    private final String assetType;
    
    @SerializedName("is_frozen")
    private final boolean isFrozen;
    
    @SerializedName("is_primary")
    private final boolean isPrimary;
    
    @SerializedName("last_transaction_timestamp")
    private final String lastTransactionTimestamp;
    
    @SerializedName("last_transaction_version")
    private final String lastTransactionVersion;
    
    @SerializedName("owner_address")
    private final String ownerAddress;
    
    @SerializedName("storage_id")
    private final String storageId;
    
    @SerializedName("token_standard")
    private final String tokenStandard;
    
    public AccountBalance(String amount, String assetType, boolean isFrozen, boolean isPrimary,
                         String lastTransactionTimestamp, String lastTransactionVersion,
                         String ownerAddress, String storageId, String tokenStandard) {
        this.amount = amount;
        this.assetType = assetType;
        this.isFrozen = isFrozen;
        this.isPrimary = isPrimary;
        this.lastTransactionTimestamp = lastTransactionTimestamp;
        this.lastTransactionVersion = lastTransactionVersion;
        this.ownerAddress = ownerAddress;
        this.storageId = storageId;
        this.tokenStandard = tokenStandard;
    }
    
    public long getAmount() {
        return Long.parseLong(amount);
    }
    
    public String getAssetType() {
        return assetType;
    }
    
    public boolean isFrozen() {
        return isFrozen;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public String getLastTransactionTimestamp() {
        return lastTransactionTimestamp;
    }
    
    public String getLastTransactionVersion() {
        return lastTransactionVersion;
    }
    
    public String getOwnerAddress() {
        return ownerAddress;
    }
    
    public String getStorageId() {
        return storageId;
    }
    
    public String getTokenStandard() {
        return tokenStandard;
    }
    
    @Override
    public String toString() {
        return "AccountBalance{" +
                "amount='" + amount + '\'' +
                ", assetType='" + assetType + '\'' +
                ", isFrozen=" + isFrozen +
                ", isPrimary=" + isPrimary +
                ", tokenStandard='" + tokenStandard + '\'' +
                '}';
    }
}
