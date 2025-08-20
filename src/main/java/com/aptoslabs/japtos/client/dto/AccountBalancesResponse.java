package com.aptoslabs.japtos.client.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for account balances query
 */
public class AccountBalancesResponse {
    @SerializedName("current_fungible_asset_balances")
    private final List<AccountBalance> balances;
    
    public AccountBalancesResponse(List<AccountBalance> balances) {
        this.balances = balances;
    }
    
    public List<AccountBalance> getBalances() {
        return balances;
    }
}
