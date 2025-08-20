package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents account information from the Aptos API
 */
public class AccountInfo {
    @SerializedName("sequence_number")
    private final String sequenceNumber;
    
    @SerializedName("authentication_key")
    private final String authenticationKey;
    
    public AccountInfo(String sequenceNumber, String authenticationKey) {
        this.sequenceNumber = sequenceNumber;
        this.authenticationKey = authenticationKey;
    }
    
    public long getSequenceNumber() {
        return Long.parseLong(sequenceNumber);
    }
    
    public String getAuthenticationKey() {
        return authenticationKey;
    }
} 