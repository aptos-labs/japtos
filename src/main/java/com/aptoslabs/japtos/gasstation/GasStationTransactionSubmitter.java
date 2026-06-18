package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransactionWithFeePayer;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.TransactionSubmitter;

/**
 * Implementation of TransactionSubmitter that uses the Gas Station API.
 * This allows transactions to be sponsored by a gas station fee payer.
 */
public class GasStationTransactionSubmitter implements TransactionSubmitter {
    private final GasStationClient client;
    private final AccountAddress feePayerAddress;

    /**
     * Constructs a GasStationTransactionSubmitter with a GasStationClient.
     *
     * @param client          the gas station client to use
     * @param feePayerAddress the address of the fee payer (gas sponsor)
     */
    public GasStationTransactionSubmitter(GasStationClient client, AccountAddress feePayerAddress) {
        this.client = client;
        this.feePayerAddress = feePayerAddress;
    }

    /**
     * Constructs a GasStationTransactionSubmitter with GasStationClientOptions.
     *
     * @param options         the options for creating the gas station client
     * @param feePayerAddress the address of the fee payer (gas sponsor)
     */
    public GasStationTransactionSubmitter(GasStationClientOptions options, AccountAddress feePayerAddress) {
        this.client = new GasStationClient(options);
        this.feePayerAddress = feePayerAddress;
    }

    /**
     * Submits a signed transaction to the gas station for sponsorship.
     * The gas station will sponsor the transaction fees.
     *
     * @param signedTransaction the signed transaction to submit
     * @return a pending transaction response containing the transaction hash
     * @throws Exception if the submission fails
     */
    @Override
    public PendingTransaction submitTransaction(SignedTransaction signedTransaction) throws Exception {
        // Wrap the transaction with fee payer information for serialization
        RawTransactionWithFeePayer txWithFeePayer = new RawTransactionWithFeePayer(
                signedTransaction.getRawTransaction(),
                feePayerAddress
        );

        // Submit the fee payer transaction
        GasStationClient.GasStationResponse response = client.signAndSubmitTransaction(
                txWithFeePayer,
                signedTransaction.getAuthenticator(),  // get the sender authenticator
                null,  // no secondary authenticators for now
                null   // no recaptcha token
        );

        // Build PendingTransaction response from the gas station response
        PendingTransaction pendingTransaction = new PendingTransaction(response.getTransactionHash());

        return pendingTransaction;
    }

    /**
     * Gets the underlying GasStationClient.
     *
     * @return the gas station client
     */
    public GasStationClient getClient() {
        return client;
    }
}
