package com.aptoslabs.japtos.transaction.authenticator;

import com.aptoslabs.japtos.bcs.Serializable;
import com.aptoslabs.japtos.bcs.Serializer;

import java.io.IOException;

/**
 * Represents a single sender authenticator for transactions that require a single signer.
 * This class is responsible for managing the authentication of a transaction initiated by a single sender.

 */
public class TransactionAuthenticatorSingleSender implements Serializable {
    private final AccountAuthenticator sender;
    
    public TransactionAuthenticatorSingleSender(AccountAuthenticator sender) {
        this.sender = sender;
    }
    
    @Override
    public void serialize(Serializer serializer) throws IOException {
        // Serialize the transaction authenticator variant (0 for Ed25519) as U8
        // The node seems to expect Ed25519 variant directly
        serializer.serializeU8((byte) 0);
        
        // Serialize the sender's account authenticator
        sender.serialize(serializer);
    }
    
    public AccountAuthenticator getSender() {
        return sender;
    }
    
    @Override
    public String toString() {
        return "TransactionAuthenticatorSingleSender{" +
            "sender=" + sender +
            '}';
    }
}
