# Japtos - Aptos Java SDK

A comprehensive Java SDK for interacting with the Aptos blockchain, featuring advanced cryptography, multi-signature support, and hierarchical deterministic wallet capabilities.

## 🚀 Features

- **🔐 Advanced Cryptography**: Ed25519 and MultiEd25519 signature schemes
- **🌱 Hierarchical Deterministic Wallets**: BIP39/BIP44 support with mnemonic phrases
- **👥 Multi-Signature Accounts**: Threshold-based multi-signature transactions
- **📦 BCS Serialization**: Binary Canonical Serialization for Aptos transactions
- **🌐 HTTP Client**: Robust REST client for Aptos API interactions
- **🧪 Comprehensive Testing**: Extensive test suite covering all functionality

## 📦 Maven Integration

### Repository Configuration


### Dependency

Add the Japtos SDK dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.aptos-labs</groupId>
  <artifactId>japtos</artifactId>
  <version>1.1.4</version>
</dependency>
```

## 🔧 Manual Installation

If you prefer to build from source, add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- HTTP Client -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
    
    <!-- Cryptography -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.77</version>
    </dependency>
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.77</version>
    </dependency>
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcutil-jdk18on</artifactId>
        <version>1.77</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

## 🏗️ Quick Start

### Initialize Client

```java
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.api.AptosConfig;

// Connect to different networks
AptosConfig config = AptosConfig.builder()
        .network(AptosConfig.Network.MAINNET)  // or TESTNET, DEVNET, LOCALNET
        .build();
        AptosClient client = new AptosClient(config);
```

## 📚 Usage Examples

### 🔑 Basic Account Management

**Generate a new account:**

```java
import com.aptoslabs.japtos.account.Ed25519Account;

// Generate a new Ed25519 account
Ed25519Account account = Ed25519Account.generate();
System.out.

        println("Address: "+account.getAccountAddress());
        System.out.

        println("Public Key: "+account.getPublicKey());
        System.out.

        println("Private Key: "+account.getPrivateKey());
```

**Create account from private key:**

```java
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;

// Create account from existing private key
Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromHex("your_private_key_hex");
Ed25519Account account = new Ed25519Account(privateKey, null);
```

### 🌱 Hierarchical Deterministic Wallets

**Derive account from mnemonic phrase:**

```java
import com.aptoslabs.japtos.account.Account;

// Derive account using BIP44 path
String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
String derivationPath = "m/44'/637'/0'/0'/0'";  // Aptos coin type: 637
Ed25519Account account = Account.fromDerivationPath(derivationPath, mnemonic);
```

**Convert entropy to mnemonic:**

```java
import com.aptoslabs.japtos.utils.Bip39Utils;

// Convert UUID/entropy to mnemonic phrase
String entropy = "9b4c9e83-a06e-4704-bc5f-b6a55d0dbb89";
String mnemonic = Bip39Utils.entropyToMnemonic(entropy);
// Result: "defense balance boat index fatal book remain champion cushion city escape huge"
```

### 👥 Multi-Signature Accounts

**Create 1-of-2 multi-signature account:**

```java
import com.aptoslabs.japtos.account.MultiEd25519Account;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;

// Create two accounts
Ed25519Account account1 = Ed25519Account.generate();
Ed25519Account account2 = Ed25519Account.generate();

// Create multi-signature account (1-of-2 threshold)
List<Ed25519PrivateKey> privateKeys = Arrays.asList(
    account1.getPrivateKey(),
    account2.getPrivateKey()
);
MultiEd25519Account multiAccount = MultiEd25519Account.fromPrivateKeys(privateKeys, 1);
```

**Create multi-signature with specific public keys:**

```java
// Create multi-signature with specific public keys and signers
List<Account> signers = Arrays.asList(account1);
List<Ed25519PublicKey> publicKeys = Arrays.asList(
    account1.getPublicKey(),
    account2.getPublicKey(),
    account3.getPublicKey()
);
MultiEd25519Account multiAccount = MultiEd25519Account.from(signers, publicKeys, 1);
```

**MultiKey with mixed key types (Ed25519 + Keyless):**

```java
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.core.crypto.KeylessPublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;

// Keyless public key from OAuth/passkey authentication
String keylessHex = "1b68747470733a2f2f6163636f756e74732e676f6f676c652e636f6d20...";
KeylessPublicKey keylessKey = KeylessPublicKey.fromHexString(keylessHex);

// Traditional Ed25519 account
Ed25519Account passWallet = Ed25519Account.generate();

// Create MultiKey with mixed types (threshold 1-of-2)
List<PublicKey> publicKeys = Arrays.asList(keylessKey, passWallet.getPublicKey());
List<Account> signers = Arrays.asList(passWallet);
MultiKeyAccount multiKey = MultiKeyAccount.fromPublicKeysAndSigners(publicKeys, signers, 1);

System.out.println("MultiKey address: " + multiKey.getAccountAddress());
```

### 💰 Transaction Management

**Simple APT transfer:**

```java
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.types.*;

// Build transfer payload
ModuleId moduleId = new ModuleId(
    AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
    new Identifier("coin")
);
TransactionPayload payload = new EntryFunctionPayload(
    moduleId,
    new Identifier("transfer"),
    Arrays.asList(new TypeTag.Struct(new StructTag(
        AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
        new Identifier("aptos_coin"),
        new Identifier("AptosCoin"),
        Arrays.asList()
    ))),
    Arrays.asList(
        new TransactionArgument.AccountAddress(recipientAddress),
        new TransactionArgument.U64(1000000L)  // 1 APT
    )
);

// Build and sign transaction
RawTransaction rawTx = new RawTransaction(
    account.getAccountAddress(),
    sequenceNumber,
    payload,
    1000000L,  // maxGasAmount
    100L,      // gasUnitPrice
    System.currentTimeMillis() / 1000 + 3600,  // expiration
    chainId
);

SignedTransaction signedTx = new SignedTransaction(rawTx, account.signTransactionWithAuthenticator(rawTx));
```

**Submit and wait for transaction:**

```java
// Submit transaction
PendingTransaction pendingTx = client.submitTransaction(signedTx);
System.out.println("Transaction Hash: " + pendingTx.getHash());

// Wait for transaction to be committed
Transaction tx = client.waitForTransaction(pendingTx.getHash());
System.out.println("Success: " + tx.isSuccess());
```

### 🔐 Message Signing

**Sign and verify messages:**

```java
// Sign a message
String message = "Hello, Aptos!";
byte[] messageBytes = message.getBytes();
Signature signature = account.sign(messageBytes);

// Verify signature
boolean isValid = account.verifySignature(messageBytes, signature);
System.out.println("Signature Valid: " + isValid);
```

## 🧪 Testing Scenarios

The SDK includes comprehensive tests demonstrating all functionality:

### Basic Account Tests
- Account generation and key management
- Private/public key operations
- Address derivation

### Multi-Signature Tests
- 1-of-2 and 1-of-3 multi-signature setups
- Complex multi-signature configurations
- Transaction signing with multiple signers

### Hierarchical Deterministic Wallet Tests
- BIP39 mnemonic generation from entropy
- BIP44 path derivation
- UUID to mnemonic conversion

### Transaction Tests
- APT transfers with single and multi-signature accounts
- Balance checking and validation
- Transaction serialization and submission

### Integration Tests
- End-to-end workflows from account creation to transaction execution
- Funding and balance management
- Real blockchain interactions

## ⚠️ Important Notes

### Network Support

**Funding is only available for:**
- **Devnet**: `https://fullnode.devnet.aptoslabs.com`
- **Localnet**: `http://127.0.0.1:8080`

For mainnet and testnet, you'll need to fund accounts through other means (exchanges, faucets, etc.).

### Network Configuration

```java
// Available networks
AptosConfig.Network.MAINNET    // Production network
AptosConfig.Network.TESTNET    // Test network
AptosConfig.Network.DEVNET     // Development network (funding available)
AptosConfig.Network.LOCALNET   // Local network (funding available)
```

## 🏃‍♂️ Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MultiKeyTests

# Run specific test method
mvn test -Dtest=MultiKeyTests#testMultikeyPathDerivation
```

## 📖 API Reference

### Account Classes

- `Ed25519Account`: Single-key Ed25519 account
- `MultiEd25519Account`: Multi-signature account
- `Account`: Abstract base class

### Utility Classes

- `Bip39Utils`: BIP39 mnemonic operations
- `Bip44Utils`: BIP44 derivation utilities
- `HexUtils`: Hexadecimal encoding/decoding

### Transaction Classes

- `RawTransaction`: Unsigned transaction
- `SignedTransaction`: Signed transaction
- `TransactionPayload`: Transaction payload types

### Client Classes

- `AptosClient`: Main client for API interactions
- `HttpClient`: HTTP client interface

### ⛽ Gas Station (Sponsored Transactions)

The SDK supports gas-sponsored transactions where a third party pays for transaction fees. This is useful for onboarding users without requiring them to hold APT for gas.

**Setup:**

```java
import com.aptoslabs.japtos.gasstation.*;

// Option 1: Using GasStationSettings with AptosConfig
GasStationSettings settings = GasStationSettings.builder()
    .apiKey("your_api_key_here")
    .endpoint("https://gas-station.testnet.aptoslabs.com")
    .build();

AptosConfig config = AptosConfig.builder()
    .network(AptosConfig.Network.TESTNET)
    .plugin(settings)
    .build();

// Option 2: Direct client creation
GasStationClientOptions options = new GasStationClientOptions.Builder()
    .network(AptosConfig.Network.TESTNET)
    .apiKey("your_api_key_here")
    .build();

AccountAddress feePayerAddress = AccountAddress.fromHex("0x...");
GasStationTransactionSubmitter gasStation = new GasStationTransactionSubmitter(options, feePayerAddress);

AptosConfig config = AptosConfig.builder()
    .network(AptosConfig.Network.TESTNET)
    .transactionSubmitter(gasStation)
    .build();

AptosClient client = new AptosClient(config);
```

**Signing for fee payer transactions:**

When using gas station, you must sign with the fee payer context:

```java
// Build your transaction
RawTransaction rawTx = new RawTransaction(...);

// Create FeePayerRawTransaction for signing
FeePayerRawTransaction feePayerTxn = new FeePayerRawTransaction(
    rawTx,
    List.of(), // secondary signers
    feePayerAddress
);

// Sign with fee payer salt
byte[] feePayerBytes = feePayerTxn.bcsToBytes();
byte[] domain = "APTOS::RawTransactionWithData".getBytes();
byte[] prefixHash = CryptoUtils.sha3_256(domain);
byte[] signingMessage = new byte[prefixHash.length + feePayerBytes.length];
System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
System.arraycopy(feePayerBytes, 0, signingMessage, prefixHash.length, feePayerBytes.length);

// Sign and create transaction
Signature signature = account.sign(signingMessage);
AccountAuthenticator auth = new Ed25519Authenticator(account.getPublicKey(), signature);
SignedTransaction signedTx = new SignedTransaction(rawTx, auth);

// Submit - gas will be paid by the sponsor
PendingTransaction pending = client.submitTransaction(signedTx);
```

The transaction will be submitted with the fee payer covering gas costs. Your account's APT balance remains unchanged.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add comprehensive tests
5. Submit a pull request

## 📄 License

This project is licensed under the Apache License 2.0.

## 🔗 References

- [Aptos Documentation](https://aptos.dev/)
- [Aptos REST API](https://fullnode.mainnet.aptoslabs.com/v1/spec#/)
- [BIP39 Specification](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [BIP44 Specification](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki)
