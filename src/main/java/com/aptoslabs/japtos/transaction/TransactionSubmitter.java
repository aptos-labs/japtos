package com.aptoslabs.japtos.transaction;

import com.aptoslabs.japtos.client.dto.PendingTransaction;

/**
 * Interface for pluggable transaction submission.
 * Implementations can override the default transaction submission behavior.
 */
public interface TransactionSubmitter {
    /**
     * Submits a signed transaction to the blockchain.
     *
     * @param signedTransaction the signed transaction to submit
     * @return a pending transaction response containing the transaction hash
     * @throws Exception if the submission fails
     */
    PendingTransaction submitTransaction(SignedTransaction signedTransaction) throws Exception;
}
