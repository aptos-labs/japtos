package com.aptoslabs.japtos.account;

import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.core.AuthenticationKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.MultiKeyAuthenticator;
import com.aptoslabs.japtos.utils.CryptoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiKey account implementation that matches TypeScript SDK's MultiKeyAccount.
 * This uses authentication scheme 3 (MultiKey) and BCS serialization.
 */
public class MultiKeyAccount extends Account {
    private final List<Ed25519PrivateKey> privateKeys;
    private final List<Ed25519PublicKey> publicKeys;
    private final int threshold;
    private final AccountAddress accountAddress;
    private final List<Integer> signerIndices;

    private MultiKeyAccount(List<Ed25519PrivateKey> privateKeys, List<Ed25519PublicKey> publicKeys, int threshold, List<Integer> signerIndices) {
        this.privateKeys = privateKeys;
        this.publicKeys = publicKeys;
        this.threshold = threshold;
        this.signerIndices = signerIndices;
        this.accountAddress = deriveAccountAddress();
    }

    public static MultiKeyAccount fromPrivateKeys(List<Ed25519PrivateKey> privateKeys, int threshold) {
        List<Ed25519PublicKey> publicKeys = new ArrayList<>();
        for (Ed25519PrivateKey privateKey : privateKeys) {
            publicKeys.add(privateKey.publicKey());
        }
        // For fromPrivateKeys, assume indices are 0, 1, 2, etc.
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < privateKeys.size(); i++) {
            indices.add(i);
        }
        return new MultiKeyAccount(privateKeys, publicKeys, threshold, indices);
    }

    public static MultiKeyAccount from(List<Account> signers, List<Ed25519PublicKey> publicKeys, int threshold) {
        // Validate inputs
        if (signers == null || publicKeys == null) {
            throw new IllegalArgumentException("Signers and public keys cannot be null");
        }
        if (signers.size() > threshold) {
            throw new IllegalArgumentException("Number of signers cannot exceed threshold");
        }
        if (signers.size() == 0) {
            throw new IllegalArgumentException("At least one signer is required");
        }
        if (publicKeys.size() == 0) {
            throw new IllegalArgumentException("At least one public key is required");
        }

        // Extract private keys and find their indices in the public keys array
        List<Ed25519PrivateKey> privateKeys = new ArrayList<>();
        List<Integer> signerIndices = new ArrayList<>();

        for (Account signer : signers) {
            if (signer instanceof Ed25519Account) {
                Ed25519Account ed25519Account = (Ed25519Account) signer;
                Ed25519PublicKey signerPublicKey = ed25519Account.getPublicKey();

                // Find the index of this signer's public key in the publicKeys array
                int index = findPublicKeyIndex(signerPublicKey, publicKeys);
                if (index == -1) {
                    throw new IllegalArgumentException(
                            "Signer's public key " + signerPublicKey.toString() +
                                    " not found in the provided public keys array"
                    );
                }

                privateKeys.add(ed25519Account.getPrivateKey());
                signerIndices.add(index);
            } else {
                throw new IllegalArgumentException("All signers must be Ed25519Account instances");
            }
        }

        // Sort signers by their indices (ascending order) - this is critical for proper signing
        List<Object[]> signersAndIndices = new ArrayList<>();
        for (int i = 0; i < privateKeys.size(); i++) {
            signersAndIndices.add(new Object[]{privateKeys.get(i), signerIndices.get(i)});
        }

        // Sort by index (ascending)
        signersAndIndices.sort((a, b) -> Integer.compare((Integer) a[1], (Integer) b[1]));

        // Extract sorted private keys and indices
        List<Ed25519PrivateKey> sortedPrivateKeys = new ArrayList<>();
        List<Integer> sortedSignerIndices = new ArrayList<>();
        for (Object[] pair : signersAndIndices) {
            sortedPrivateKeys.add((Ed25519PrivateKey) pair[0]);
            sortedSignerIndices.add((Integer) pair[1]);
        }

        return new MultiKeyAccount(sortedPrivateKeys, publicKeys, threshold, sortedSignerIndices);
    }

    /**
     * Finds the index of a public key in the public keys array.
     *
     * @param targetKey  the public key to find
     * @param publicKeys the array of public keys to search in
     * @return the index of the target key, or -1 if not found
     */
    private static int findPublicKeyIndex(Ed25519PublicKey targetKey, List<Ed25519PublicKey> publicKeys) {
        for (int i = 0; i < publicKeys.size(); i++) {
            if (publicKeys.get(i).toString().equals(targetKey.toString())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Ed25519PublicKey getPublicKey() {
        // Return the first public key as the primary one
        return publicKeys.get(0);
    }

    @Override
    public AccountAddress getAccountAddress() {
        return accountAddress;
    }

    @Override
    public SigningScheme getSigningScheme() {
        return SigningScheme.MULTI_KEY;
    }

    @Override
    public AccountAuthenticator signWithAuthenticator(byte[] message) {
        // For simplicity, sign with the first private key
        Signature signature = privateKeys.get(0).sign(message);
        return new MultiKeyAuthenticator(publicKeys, signature, threshold, signerIndices);
    }

    @Override
    public AccountAuthenticator signTransactionWithAuthenticator(RawTransaction transaction) throws Exception {
        // For simplicity, sign with the first private key
        Signature signature = signTransaction(transaction);
        return new MultiKeyAuthenticator(publicKeys, signature, threshold, signerIndices);
    }

    @Override
    public Signature sign(byte[] message) {
        // For simplicity, sign with the first private key
        return privateKeys.get(0).sign(message);
    }

    @Override
    public Signature signTransaction(RawTransaction transaction) throws Exception {
        // Match Ed25519Account.signTransaction: sha3("APTOS::RawTransaction") || BCS(RawTransaction)
        byte[] domain = "APTOS::RawTransaction".getBytes();
        byte[] prefixHash = CryptoUtils.sha3_256(domain);
        byte[] txnBytes = transaction.bcsToBytes();
        byte[] signingMessage = new byte[prefixHash.length + txnBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(txnBytes, 0, signingMessage, prefixHash.length, txnBytes.length);
        return privateKeys.get(0).sign(signingMessage);
    }

    private AccountAddress deriveAccountAddress() {
        // Derive auth key for MultiKey using BCS serialization (matching TypeScript SDK)
        // MultiKey serializes as: vector<AnyPublicKey> + u8 threshold
        // AnyPublicKey serializes as: uleb128(variant) + public_key_bytes
        // For Ed25519: variant=0, so it's: 0x00 + 32 bytes

        try {
            // Create BCS serializer
            Serializer serializer = new Serializer();

            // Serialize vector of public keys
            serializer.serializeU32AsUleb128(publicKeys.size()); // Vector length
            for (Ed25519PublicKey pk : publicKeys) {
                serializer.serializeU32AsUleb128(0); // Ed25519 variant = 0
                serializer.serializeBytes(pk.toBytes()); // 32 bytes
            }

            // Serialize threshold
            serializer.serializeU8((byte) threshold);

            // Get the serialized bytes
            byte[] multiKeyBytes = serializer.toByteArray();

            // Create authentication key with scheme 3 (MultiKey)
            AuthenticationKey ak = AuthenticationKey.fromSchemeAndBytes((byte) 3, multiKeyBytes);
            return ak.accountAddress();

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive account address", e);
        }
    }

    public List<Ed25519PrivateKey> getPrivateKeys() {
        return privateKeys;
    }

    public List<Ed25519PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public int getThreshold() {
        return threshold;
    }

    public List<Integer> getSignerIndices() {
        return signerIndices;
    }
}
