# Japtos - Aptos Java SDK

[![CI](https://github.com/aptos-labs/japtos/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/aptos-labs/japtos/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/aptos-labs/japtos/branch/main/graph/badge.svg)](https://codecov.io/gh/aptos-labs/japtos)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.aptos-labs/japtos.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.aptos-labs/japtos)
[![javadoc](https://javadoc.io/badge2/io.github.aptos-labs/japtos/javadoc.svg)](https://javadoc.io/doc/io.github.aptos-labs/japtos)
[![License](https://img.shields.io/badge/License-Innovation--Enabling%20Source%20Code-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg)](#android-support)

A pure-Java SDK for building on the [Aptos](https://aptos.dev/) blockchain. Japtos has no
runtime dependency on the JVM-only parts of the platform, so the same artifact runs on the
server (Java 17+) and on **Android** (API 26+). It provides account management, Ed25519 and
multi-signature cryptography, BIP39/BIP44 hierarchical deterministic wallets, BCS
serialization, a REST client, and gas-sponsored (fee payer) transactions.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
  - [Maven](#maven)
  - [Gradle](#gradle)
- [Android Support](#android-support)
- [Quick Start](#quick-start)
- [Logging](#logging)
- [Usage Guide](#usage-guide)
  - [Account Management](#account-management)
  - [Hierarchical Deterministic Wallets](#hierarchical-deterministic-wallets)
  - [Multi-Signature and Multi-Key Accounts](#multi-signature-and-multi-key-accounts)
  - [Transactions](#transactions)
  - [Message Signing](#message-signing)
  - [Move `Option` Parameters](#move-option-parameters)
  - [Gas Station (Sponsored Transactions)](#gas-station-sponsored-transactions)
- [Networks](#networks)
- [API Reference](#api-reference)
- [Building from Source](#building-from-source)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
- [References](#references)

## Requirements

| Target  | Minimum                                                         |
| ------- | -------------------------------------------------------------- |
| JVM     | Java 17 (the library is compiled to Java 17 bytecode)         |
| Android | API level 26 (Android 8.0) or higher, with Android Gradle Plugin 8.0+ |
| Build   | Maven 3.6+ or Gradle 8+                                       |

> The published artifact is compiled with `maven.compiler.release=17`. On Android, AGP 8's
> D8/R8 toolchain handles the Java 17 class files; see [Android Support](#android-support) for
> the required `compileOptions` and packaging configuration.

## Installation

Japtos is published to [Maven Central](https://central.sonatype.com/artifact/io.github.aptos-labs/japtos)
under the coordinates `io.github.aptos-labs:japtos`. Use the version shown in the Maven Central
badge above as the latest release.

### Maven

```xml
<dependency>
  <groupId>io.github.aptos-labs</groupId>
  <artifactId>japtos</artifactId>
  <version>1.2.0</version>
</dependency>
```

Maven Central is enabled by default, so no extra `<repositories>` block is required.

### Gradle

Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("io.github.aptos-labs:japtos:1.2.0")
}
```

Groovy DSL (`build.gradle`):

```groovy
dependencies {
    implementation 'io.github.aptos-labs:japtos:1.2.0'
}
```

Make sure `mavenCentral()` is listed in your `repositories {}` block (it is by default in
projects created with recent Gradle/Android tooling).

## Android Support

Japtos is designed to run on Android (the Maven `description` is literally "Aptos Java SDK for
Android"). Its dependencies — OkHttp, Gson, and the BouncyCastle `jdk18on` artifacts — are all
Android compatible. A few project settings are required to integrate it cleanly.

### 1. Minimum SDK and Java 17 desugaring

Set `minSdk` to 26 or higher and enable Java 17 source/target compatibility. With AGP 8+ the
D8/R8 toolchain compiles the library's Java 17 bytecode; enabling core library desugaring
also back-ports `java.time`, `java.util.Optional`, and other APIs the SDK relies on to older
Android runtimes.

```kotlin
android {
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("io.github.aptos-labs:japtos:1.2.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}
```

### 2. BouncyCastle on Android

Android ships its own stripped-down copy of BouncyCastle. To avoid clashes with the bundled
provider, register the full `jdk18on` provider explicitly before performing cryptographic
operations and let it take precedence:

```java
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

// Run once during app startup (e.g. in Application.onCreate()).
Security.removeProvider("BC");
Security.insertProviderAt(new BouncyCastleProvider(), 1);
```

### 3. Packaging conflicts

The BouncyCastle artifacts include duplicate `META-INF` files that can break the APK/AAB
packaging step. Exclude them:

```kotlin
android {
    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
        }
    }
}
```

### 4. Networking and threading

- The SDK uses OkHttp, which requires the `android.permission.INTERNET` permission in your
  `AndroidManifest.xml`.
- All client calls are blocking. Never invoke `AptosClient` methods on the main/UI thread; run
  them on a background thread via a Kotlin coroutine (`Dispatchers.IO`), an `ExecutorService`,
  or `WorkManager`.

### 5. R8 / ProGuard

If you enable code shrinking, keep BouncyCastle and the SDK's reflection-accessed classes:

```proguard
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class com.aptoslabs.japtos.** { *; }
```

## Quick Start

```java
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.api.AptosConfig;

// Connect to a network: MAINNET, TESTNET, DEVNET, or LOCALNET.
AptosConfig config = AptosConfig.builder()
        .network(AptosConfig.Network.DEVNET)
        .build();

AptosClient client = new AptosClient(config);
```

## Logging

Japtos includes a configurable logging facade. Set the level when building the config, or
change it at runtime.

```java
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.utils.LogLevel;

AptosConfig config = AptosConfig.builder()
        .network(AptosConfig.Network.DEVNET)
        .logLevel(LogLevel.INFO) // DEBUG, INFO, WARN, or ERROR
        .build();

AptosClient client = new AptosClient(config);
```

Available levels:

| Level             | Description                                |
| ----------------- | ------------------------------------------ |
| `LogLevel.DEBUG`  | Detailed diagnostics (default)             |
| `LogLevel.INFO`   | General informational messages             |
| `LogLevel.WARN`   | Warnings only                              |
| `LogLevel.ERROR`  | Errors only                                |

Change the level after initialization:

```java
import com.aptoslabs.japtos.utils.Logger;
import com.aptoslabs.japtos.utils.LogLevel;

Logger.setLogLevel(LogLevel.WARN);
LogLevel current = Logger.getLogLevel();
```

## Usage Guide

### Account Management

Generate a new Ed25519 account:

```java
import com.aptoslabs.japtos.account.Ed25519Account;

Ed25519Account account = Ed25519Account.generate();
System.out.println("Address: " + account.getAccountAddress());
System.out.println("Public Key: " + account.getPublicKey());
System.out.println("Private Key: " + account.getPrivateKey());
```

Create an account from an existing private key:

```java
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;

Ed25519PrivateKey privateKey = Ed25519PrivateKey.fromHex("your_private_key_hex");
Ed25519Account account = Ed25519Account.fromPrivateKey(privateKey);

// Or directly from a hex-encoded private key:
Ed25519Account sameAccount = Ed25519Account.fromPrivateKeyHex("your_private_key_hex");
```

### Hierarchical Deterministic Wallets

Derive an account from a BIP39 mnemonic using a BIP44 path (Aptos coin type is `637`):

```java
import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;

String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
String derivationPath = "m/44'/637'/0'/0'/0'";
Ed25519Account account = Account.fromDerivationPath(derivationPath, mnemonic);
```

Convert entropy (such as a UUID) into a mnemonic phrase:

```java
import com.aptoslabs.japtos.utils.Bip39Utils;

String entropy = "9b4c9e83-a06e-4704-bc5f-b6a55d0dbb89";
String mnemonic = Bip39Utils.entropyToMnemonic(entropy);
// "defense balance boat index fatal book remain champion cushion city escape huge"
```

### Multi-Signature and Multi-Key Accounts

Create a 1-of-2 MultiEd25519 account from private keys:

```java
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiEd25519Account;
import com.aptoslabs.japtos.core.crypto.Ed25519PrivateKey;
import java.util.Arrays;
import java.util.List;

Ed25519Account account1 = Ed25519Account.generate();
Ed25519Account account2 = Ed25519Account.generate();

List<Ed25519PrivateKey> privateKeys = Arrays.asList(
        account1.getPrivateKey(),
        account2.getPrivateKey());

// Threshold of 1 signature out of 2 keys.
MultiEd25519Account multiAccount = MultiEd25519Account.fromPrivateKeys(privateKeys, 1);
```

Create a MultiEd25519 account from explicit public keys and signers:

```java
import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiEd25519Account;
import com.aptoslabs.japtos.core.crypto.Ed25519PublicKey;
import java.util.Arrays;
import java.util.List;

Ed25519Account account1 = Ed25519Account.generate();
Ed25519Account account2 = Ed25519Account.generate();
Ed25519Account account3 = Ed25519Account.generate();

// Only account1 actually signs; the threshold is 1-of-3.
List<Account> signers = Arrays.asList(account1);
List<Ed25519PublicKey> publicKeys = Arrays.asList(
        account1.getPublicKey(),
        account2.getPublicKey(),
        account3.getPublicKey());

MultiEd25519Account multiAccount = MultiEd25519Account.from(signers, publicKeys, 1);
```

Build a MultiKey account mixing key types (for example Ed25519 + Keyless):

```java
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.MultiKeyAccount;
import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.core.crypto.KeylessPublicKey;
import com.aptoslabs.japtos.core.crypto.PublicKey;
import java.util.Arrays;
import java.util.List;

// Keyless public key from OAuth/passkey authentication.
String keylessHex = "1b68747470733a2f2f6163636f756e74732e676f6f676c652e636f6d20...";
KeylessPublicKey keylessKey = KeylessPublicKey.fromHexString(keylessHex);

Ed25519Account passkeyWallet = Ed25519Account.generate();

List<PublicKey> publicKeys = Arrays.asList(keylessKey, passkeyWallet.getPublicKey());
List<Account> signers = Arrays.asList(passkeyWallet);

// Threshold of 1 signature out of 2 keys.
MultiKeyAccount multiKey = MultiKeyAccount.fromPublicKeysAndSigners(publicKeys, signers, 1);
System.out.println("MultiKey address: " + multiKey.getAccountAddress());
```

### Transactions

Build, sign, and submit a simple APT transfer:

```java
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.types.*;
import java.util.Arrays;

// fromHex requires a fully padded 32-byte (64 hex character) address.
AccountAddress aptosFramework = AccountAddress.fromHex(
        "0x0000000000000000000000000000000000000000000000000000000000000001");

ModuleId moduleId = new ModuleId(
        aptosFramework,
        new Identifier("coin"));

TransactionPayload payload = new EntryFunctionPayload(
        moduleId,
        new Identifier("transfer"),
        Arrays.asList(new TypeTag.Struct(new StructTag(
                aptosFramework,
                new Identifier("aptos_coin"),
                new Identifier("AptosCoin"),
                Arrays.asList()))),
        Arrays.asList(
                new TransactionArgument.AccountAddress(recipientAddress),
                new TransactionArgument.U64(1_000_000L))); // 0.01 APT (octas)

RawTransaction rawTx = new RawTransaction(
        account.getAccountAddress(),
        sequenceNumber,
        payload,
        1_000_000L,                                   // maxGasAmount
        100L,                                         // gasUnitPrice
        System.currentTimeMillis() / 1000 + 3600,     // expiration (epoch seconds)
        chainId);

SignedTransaction signedTx = new SignedTransaction(
        rawTx, account.signTransactionWithAuthenticator(rawTx));
```

Submit and wait for the transaction to commit:

```java
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.client.dto.Transaction;

PendingTransaction pendingTx = client.submitTransaction(signedTx);
System.out.println("Transaction Hash: " + pendingTx.getHash());

Transaction tx = client.waitForTransaction(pendingTx.getHash());
System.out.println("Success: " + tx.isSuccess());
```

### Message Signing

```java
import com.aptoslabs.japtos.core.crypto.Signature;
import java.nio.charset.StandardCharsets;

String message = "Hello, Aptos!";
byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

Signature signature = account.sign(messageBytes);
boolean isValid = account.verifySignature(messageBytes, signature);
System.out.println("Signature Valid: " + isValid);
```

### Move `Option` Parameters

`MoveOption` builds Aptos `std::option::Option` arguments. A `null` value produces `None`.

Example Move module:

```move
module example::optional_params {
    use std::option::{Self, Option};
    use std::string::String;

    public entry fun test_mixed(
        account: &signer,
        required_u64: u64,
        optional_string: Option<String>,
        required_bool: bool,
        optional_u64: Option<u64>
    ) {
        // Mix of required and optional parameters.
    }
}
```

Calling it from Java:

```java
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.types.MoveOption;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.types.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

List<TransactionArgument> args = Arrays.asList(
        new TransactionArgument.U64(456L),    // required u64
        MoveOption.string("optional value"),  // Some("optional value")
        new TransactionArgument.Bool(false),  // required bool
        MoveOption.u64(null));                // None

// Address of the account that published the Move module (fully padded 32-byte hex).
AccountAddress moduleAddress = AccountAddress.fromHex(
        "0x0000000000000000000000000000000000000000000000000000000000000abc");
ModuleId moduleId = new ModuleId(moduleAddress, new Identifier("optional_params"));
EntryFunctionPayload payload = new EntryFunctionPayload(
        moduleId,
        new Identifier("test_mixed"),
        Collections.emptyList(),
        args);
```

Factory methods are available for every supported type:

```java
import java.math.BigInteger;

MoveOption.u8((byte) 1);
MoveOption.u16((short) 100);
MoveOption.u32(1000);
MoveOption.u64(10_000L);
MoveOption.u128(BigInteger.valueOf(12345));
MoveOption.u256(new BigInteger("999999"));
MoveOption.bool(true);
MoveOption.string("hello");
MoveOption.address(accountAddress);
MoveOption.u8Vector(new byte[]{1, 2, 3});
```

Inspecting a `MoveOption`:

```java
MoveOption<TransactionArgument.U64> amount = MoveOption.u64(1000L);
if (amount.isSome()) {
    TransactionArgument.U64 value = amount.unwrap();
    System.out.println("Amount: " + value.getValue());
}

java.util.Optional<TransactionArgument.U64> javaOpt = amount.toOptional();
```

### Gas Station (Sponsored Transactions)

The SDK supports gas-sponsored transactions, where a third party (the fee payer) pays the
transaction fees. This is useful for onboarding users who do not yet hold APT.

Configure a gas station:

```java
import com.aptoslabs.japtos.gasstation.*;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.AptosClient;
import com.aptoslabs.japtos.core.AccountAddress;

// Option 1: GasStationSettings as an AptosConfig plugin.
GasStationSettings settings = GasStationSettings.builder()
        .apiKey("your_api_key_here")
        .endpoint("https://gas-station.testnet.aptoslabs.com")
        .build();

AptosConfig pluginConfig = AptosConfig.builder()
        .network(AptosConfig.Network.TESTNET)
        .plugin(settings)
        .build();

AptosClient client = new AptosClient(pluginConfig);

// Option 2: an explicit transaction submitter.
GasStationClientOptions options = new GasStationClientOptions.Builder()
        .network(AptosConfig.Network.TESTNET)
        .apiKey("your_api_key_here")
        .build();

AccountAddress feePayerAddress = AccountAddress.fromHex(
        "0x0000000000000000000000000000000000000000000000000000000000000abc");
GasStationTransactionSubmitter gasStation =
        new GasStationTransactionSubmitter(options, feePayerAddress);

AptosConfig submitterConfig = AptosConfig.builder()
        .network(AptosConfig.Network.TESTNET)
        .transactionSubmitter(gasStation)
        .build();

AptosClient submitterClient = new AptosClient(submitterConfig);
```

When using a gas station you must sign with the fee payer context:

```java
import com.aptoslabs.japtos.transaction.FeePayerRawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.Ed25519Authenticator;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.core.crypto.Signature;
import com.aptoslabs.japtos.utils.CryptoUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

// rawTx, account, and feePayerAddress come from the transaction and gas-station
// setup examples above; submitterClient is the gas-station-backed client.
FeePayerRawTransaction feePayerTxn = new FeePayerRawTransaction(
        rawTx,
        List.of(),        // secondary signers
        feePayerAddress);

byte[] feePayerBytes = feePayerTxn.bcsToBytes();
byte[] domain = "APTOS::RawTransactionWithData".getBytes(StandardCharsets.UTF_8);
byte[] prefixHash = CryptoUtils.sha3_256(domain);

byte[] signingMessage = new byte[prefixHash.length + feePayerBytes.length];
System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
System.arraycopy(feePayerBytes, 0, signingMessage, prefixHash.length, feePayerBytes.length);

Signature signature = account.sign(signingMessage);
AccountAuthenticator auth = new Ed25519Authenticator(account.getPublicKey(), signature);
SignedTransaction signedTx = new SignedTransaction(rawTx, auth);

// Submit through the gas-station-backed client; the fee payer covers gas, so the
// signer's APT balance is unchanged.
PendingTransaction pending = submitterClient.submitTransaction(signedTx);
```

## Networks

```java
AptosConfig.Network.MAINNET    // Production network
AptosConfig.Network.TESTNET    // Test network
AptosConfig.Network.DEVNET     // Development network (faucet funding available)
AptosConfig.Network.LOCALNET   // Local network (faucet funding available)
```

The SDK's built-in faucet funding helper only supports:

- **Devnet**: `https://fullnode.devnet.aptoslabs.com`
- **Localnet**: `http://127.0.0.1:8080`

For **testnet**, fund accounts through the official Aptos faucet at
[aptos.dev/network/faucet](https://aptos.dev/network/faucet). On **mainnet**, accounts must be
funded by other means (exchanges, transfers).

You can also point the client at a custom fullnode:

```java
AptosConfig config = AptosConfig.builder()
        .fullnode("https://custom.fullnode.example.com")
        .build();
```

## API Reference

Full Javadoc is published at
[javadoc.io/doc/io.github.aptos-labs/japtos](https://javadoc.io/doc/io.github.aptos-labs/japtos).

Key types:

| Category    | Classes                                                                 |
| ----------- | ----------------------------------------------------------------------- |
| Accounts    | `Account`, `Ed25519Account`, `MultiEd25519Account`, `MultiKeyAccount`   |
| Crypto      | `Ed25519PrivateKey`, `Ed25519PublicKey`, `KeylessPublicKey`, `Signature`|
| Transactions| `RawTransaction`, `SignedTransaction`, `TransactionPayload`, `EntryFunctionPayload`, `FeePayerRawTransaction` |
| Types       | `AccountAddress`, `ModuleId`, `Identifier`, `TypeTag`, `StructTag`, `TransactionArgument`, `MoveOption` |
| Client      | `AptosClient`, `AptosConfig`                                            |
| Utilities   | `Bip39Utils`, `Bip44Utils`, `HexUtils`, `CryptoUtils`, `Logger`         |
| Gas Station | `GasStationSettings`, `GasStationClientOptions`, `GasStationTransactionSubmitter` |

## Building from Source

```bash
git clone https://github.com/aptos-labs/japtos.git
cd japtos

mvn compile   # compile
mvn package   # build the JAR
```

## Testing

Unit tests (the default, fast suite) run without a network:

```bash
mvn verify -P unit-tests
```

Integration tests require a running Aptos localnet and are tagged `integration`:

```bash
mvn verify -P integration-tests
```

Run a single test class or method:

```bash
mvn test -Dtest=MultiKeyTests
mvn test -Dtest=MultiKeyTests#testMultikeyPathDerivation
```

Both profiles produce a JaCoCo coverage report at `target/site/jacoco/jacoco.xml`, which CI
uploads to [Codecov](https://codecov.io/gh/aptos-labs/japtos).

## Troubleshooting

**Transaction submission fails with "sequence number too old"**

- Use the latest sequence number from the blockchain via `client.getNextSequenceNumber(address)`.
- Ensure no other transactions are pending for the same account.

**"Account not found" errors**

- New accounts must be funded before they can send transactions.
- Use the faucet on devnet/localnet to fund accounts.
- Verify the address is correctly formatted (a 32-byte hex string).

**BCS serialization errors**

- Ensure transaction arguments match the expected Move types.
- Wrap optional parameters with `MoveOption`.
- Verify addresses are proper `AccountAddress` objects.

**Gas station transaction failures**

- Confirm the API key is valid and funded.
- Confirm the fee payer address matches the gas station configuration.
- Sign with the `APTOS::RawTransactionWithData` domain prefix for fee payer transactions.

**Network connection issues**

- Verify the fullnode URL is correct and reachable.
- Check connectivity and firewall settings.
- For custom networks, confirm the endpoint exposes the Aptos REST API.

**Android-specific issues**

- See [Android Support](#android-support) for desugaring, BouncyCastle provider, packaging,
  and R8/ProGuard configuration.
- `NoSuchAlgorithmException` / provider errors usually mean the bundled Android BouncyCastle is
  shadowing the SDK's. Re-register the provider as shown above.
- `Duplicate files copied in APK META-INF/...` means you need the `packaging { resources { excludes } }` block.
- `NetworkOnMainThreadException` means a client call ran on the UI thread; move it to a
  background dispatcher.

### Getting Help

- **GitHub Issues**: [Report bugs or request features](https://github.com/aptos-labs/japtos/issues)
- **Documentation**: [Aptos Documentation](https://aptos.dev/)
- **Community**: [Aptos Discord](https://discord.gg/aptoslabs)

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for development
setup, code style, and the pull request process. In short:

1. Fork the repository and create a feature branch from `main`.
2. Make your changes, following the existing code style.
3. Add tests and ensure `mvn verify -P unit-tests` passes.
4. Update documentation (README, Javadoc, CHANGELOG) as needed.
5. Open a pull request with a clear description.

## License

Licensed under the Aptos Innovation-Enabling Source Code License. See [LICENSE](LICENSE) for
the full terms. The license permits internal, non-production, and non-commercial use, with an
additional grant for production use of applications built exclusively on the Aptos protocol;
each released version automatically converts to the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
four years after it is published.

## References

- [Aptos Documentation](https://aptos.dev/)
- [Aptos REST API](https://fullnode.mainnet.aptoslabs.com/v1/spec#/)
- [BIP39 Specification](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [BIP44 Specification](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki)
