package com.aptoslabs.japtos.utils;

import com.aptoslabs.japtos.api.AptosConfig;

/**
 * Configuration for Aptos network tests
 */
public class TestConfig {
    
    // Default network (can be changed for testing)
    public static final AptosConfig.Network DEFAULT_NETWORK = AptosConfig.Network.TESTNET;
    
    // Test amounts
    public static final String FUND_AMOUNT = "100000000"; // 100 APT
    public static final long TRANSFER_AMOUNT = 1000000L;  // 1 APT
    public static final long SMALL_TRANSFER = 1000000L;    // 0.1 APT
    
    // Gas settings
    public static final long DEFAULT_MAX_GAS = 1000000L;
    public static final long DEFAULT_GAS_PRICE = 100L;
    public static final long HIGH_GAS_LIMIT = 2000000L;
    public static final long LOW_GAS_LIMIT = 100000L;
    
    // Timeouts
    public static final int FUNDING_DELAY_MS = 10000;
    
    // Aptos framework address
    public static final String APTOS_FRAMEWORK_ADDRESS = "0x0000000000000000000000000000000000000000000000000000000000000001";
} 