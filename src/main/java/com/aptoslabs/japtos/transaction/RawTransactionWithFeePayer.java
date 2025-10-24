package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.TransactionPayload;

import java.io.IOException;

/**
 * Represents a raw transaction with a fee payer (sponsor) for gas fees.
 * This transaction type allows a third party to pay for the transaction's gas fees.
 */
public class RawTransactionWithFeePayer implements Serializable {
    private final RawTransaction rawTransaction;
    private final AccountAddress feePayerAddress;

    public RawTransactionWithFeePayer(RawTransaction rawTransaction, AccountAddress feePayerAddress) {
        this.rawTransaction = rawTransaction;
        this.feePayerAddress = feePayerAddress;
    }

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public AccountAddress getFeePayerAddress() {
        return feePayerAddress;
    }

    public AccountAddress getSender() {
        return rawTransaction.getSender();
    }

    public long getSequenceNumber() {
        return rawTransaction.getSequenceNumber();
    }

    public TransactionPayload getPayload() {
        return rawTransaction.getPayload();
    }

    public long getMaxGasAmount() {
        return rawTransaction.getMaxGasAmount();
    }

    public long getGasUnitPrice() {
        return rawTransaction.getGasUnitPrice();
    }

    public long getExpirationTimestampSecs() {
        return rawTransaction.getExpirationTimestampSecs();
    }

    public long getChainId() {
        return rawTransaction.getChainId();
    }

    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize as SimpleTransaction (matches TypeScript SDK format)
        // 1. Serialize the raw transaction
        rawTransaction.serialize(serializer);
        // 2. Serialize boolean indicating fee payer is present
        serializer.serializeBool(true);
        // 3. Serialize the fee payer address
        serializer.serializeAccountAddress(feePayerAddress);
    }

    public byte[] bcsToBytes() {
        try {
            Serializer serializer = new Serializer();
            serialize(serializer);
            return serializer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}

