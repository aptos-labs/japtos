package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.utils.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Represents a raw transaction with fee payer for signing purposes.
 * This structure is used to generate the signing message for fee payer transactions.
 * It matches the TypeScript SDK's FeePayerRawTransaction format.
 */
public class FeePayerRawTransaction implements Serializable {
    private static final int TRANSACTION_VARIANT_FEE_PAYER = 1;

    private final RawTransaction rawTransaction;
    private final List<AccountAddress> secondarySignerAddresses;
    private final AccountAddress feePayerAddress;

    public FeePayerRawTransaction(
            RawTransaction rawTransaction,
            List<AccountAddress> secondarySignerAddresses,
            AccountAddress feePayerAddress) {
        this.rawTransaction = rawTransaction;
        this.secondarySignerAddresses = secondarySignerAddresses;
        this.feePayerAddress = feePayerAddress;
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize variant tag for FeePayerTransaction
        serializer.serializeU32AsUleb128(TRANSACTION_VARIANT_FEE_PAYER);
        // Serialize the raw transaction
        rawTransaction.serialize(serializer);
        // Serialize secondary signer addresses (empty list in our case)
        serializer.serializeU32AsUleb128(secondarySignerAddresses.size());
        for (AccountAddress address : secondarySignerAddresses) {
            serializer.serializeAccountAddress(address);
        }
        // Serialize fee payer address
        serializer.serializeAccountAddress(feePayerAddress);
    }

    public byte[] bcsToBytes() {
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            byte[] result = serializer.toByteArray();
            return result;
        } catch (IOException e) {
            Logger.error("Failed to serialize FeePayerRawTransaction", e);
            throw new RuntimeException("Failed to serialize FeePayerRawTransaction", e);
        }
    }

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }
}
