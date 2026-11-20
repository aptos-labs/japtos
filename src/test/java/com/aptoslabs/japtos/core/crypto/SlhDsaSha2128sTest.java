package com.aptoslabs.japtos.core.crypto;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.account.SlhDsaSha2128sAccount;
import com.aptoslabs.japtos.bcs.Deserializer;
import com.aptoslabs.japtos.bcs.Serializer;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.MultiKeyAuthenticator;
import com.aptoslabs.japtos.transaction.authenticator.SingleKeyAuthenticator;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.utils.Bip44Utils;
import com.aptoslabs.japtos.utils.CryptoUtils;
import com.aptoslabs.japtos.utils.HexUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SLH-DSA-SHA2-128s post-quantum signatures (AIP-137), porting the
 * TypeScript SDK's slhDsaSha2128s and HD-key test vectors (aptos-ts-sdk PR #802).
 *
 * <p>On-chain AIP-137 wire constants: pubkey 32 bytes, private key material 48 bytes
 * (three seeds), signature 7,856 bytes, AnyPublicKey variant 5, AnySignature variant 4,
 * SingleKey AccountAuthenticator variant 2, SingleSender TransactionAuthenticator variant 4.</p>
 */
class SlhDsaSha2128sTest {
    // Seed 0x00..0x2f expanded with @noble/post-quantum slh_dsa_sha2_128s.keygen(seed).
    private static final String SEED_48 =
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f";
    private static final String EXPECTED_PUB =
            "202122232425262728292a2b2c2d2e2f990ce6298792b128846a8e4a3a68954c";
    // SHA3-256(BCS(AnyPublicKey(EXPECTED_PUB)) || 0x02)
    private static final String EXPECTED_AUTH_KEY =
            "3b1d2b05c33c7769bc4728a66ef23ed5ac3f50a08166a6bbecf73cc2856b2b22";
    private static final String TEST_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    private static SlhDsaSha2128sPrivateKey fixedKey() {
        return SlhDsaSha2128sPrivateKey.fromHex(SEED_48);
    }

    private static RawTransaction sampleRawTransaction(AccountAddress sender) {
        EntryFunctionPayload payload = new EntryFunctionPayload(
                new ModuleId(
                        AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"),
                        new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
        return new RawTransaction(sender, 0L, payload, 100000L, 100L, 1700000000L, 4L);
    }

    @Test
    @DisplayName("Keygen from 48-byte three-seeds matches @noble/post-quantum public key")
    void nobleKeygenVector() {
        SlhDsaSha2128sPrivateKey priv = fixedKey();
        assertEquals(SlhDsaSha2128sPrivateKey.LENGTH, priv.toBytes().length);
        assertEquals(SEED_48, priv.toHexString());
        assertEquals(EXPECTED_PUB, priv.publicKey().toHexString());
        assertEquals("0x" + EXPECTED_PUB, priv.publicKey().toString());
        assertEquals(SlhDsaSha2128sPublicKey.LENGTH, priv.publicKey().toBytes().length);
        assertEquals(priv.publicKey(), SlhDsaSha2128sPublicKey.fromHex("0x" + EXPECTED_PUB));
        assertEquals(priv.publicKey(), SlhDsaSha2128sPublicKey.fromHex(EXPECTED_PUB));
    }

    @Test
    @DisplayName("Cross-implementation @noble/post-quantum signature and own signature verify")
    void nobleSignatureVerifiesAndOwnSignatureVerifies() throws Exception {
        String nobleSigHex;
        try (InputStream in = getClass().getResourceAsStream("/slh-dsa/noble-sha2-128s-hello-aptos-sig.hex")) {
            assertNotNull(in);
            nobleSigHex = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        byte[] message = "hello aptos".getBytes(StandardCharsets.UTF_8);
        SlhDsaSha2128sPublicKey pub = fixedKey().publicKey();

        SlhDsaSha2128sSignature nobleSig = SlhDsaSha2128sSignature.fromHex(nobleSigHex);
        assertTrue(pub.verifySignature(message, nobleSig));

        SlhDsaSha2128sSignature ownSig = fixedKey().sign(message);
        assertEquals(SlhDsaSha2128sSignature.LENGTH, ownSig.toBytes().length);
        assertTrue(pub.verifySignature(message, ownSig));
        // Deterministic FIPS 205 signing produces identical signatures for identical inputs.
        assertEquals(ownSig, fixedKey().sign(message));

        // Negative verification cases
        assertFalse(pub.verifySignature("wrong message".getBytes(StandardCharsets.UTF_8), ownSig));
        assertFalse(pub.verifySignature(null, ownSig));
        assertFalse(pub.verifySignature(message, null));
        assertFalse(pub.verifySignature(message, Signature.fromBytes(new byte[Signature.LENGTH])));
    }

    @Test
    @DisplayName("Invalid lengths and null inputs throw IllegalArgumentException")
    void invalidLengthsAndNullsThrow() {
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPublicKey.fromBytes(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPublicKey.fromBytes(null));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPublicKey.fromHex(null));

        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPrivateKey.fromBytes(new byte[47]));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPrivateKey.fromBytes(null));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPrivateKey.fromHex(null));
        assertThrows(IllegalArgumentException.class, () -> fixedKey().sign(null));

        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sSignature.fromBytes(new byte[7855]));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sSignature.fromBytes(null));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sSignature.fromHex(null));
    }

    @Test
    @DisplayName("AIP-80 formatting and strict/non-strict parsing")
    void aip80RoundTripAndStrictMode() {
        SlhDsaSha2128sPrivateKey priv = fixedKey();
        String aip80 = priv.toAIP80String();
        assertEquals("slh-dsa-sha2-128s-priv-0x" + SEED_48, aip80);
        assertEquals(aip80, priv.toString());

        assertEquals(priv, SlhDsaSha2128sPrivateKey.fromHex(aip80));
        assertEquals(priv, SlhDsaSha2128sPrivateKey.fromHex(aip80, true));
        assertEquals(priv, SlhDsaSha2128sPrivateKey.fromHex("  " + aip80 + "  ", true));
        assertEquals(priv, SlhDsaSha2128sPrivateKey.fromHex("0x" + SEED_48));
        assertEquals(priv, SlhDsaSha2128sPrivateKey.fromHex(SEED_48));

        // Strict mode rejects raw hex, 0x-only hex, missing 0x after prefix, and wrong scheme prefix.
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPrivateKey.fromHex(SEED_48, true));
        assertThrows(IllegalArgumentException.class, () -> SlhDsaSha2128sPrivateKey.fromHex("0x" + SEED_48, true));
        assertThrows(IllegalArgumentException.class,
                () -> SlhDsaSha2128sPrivateKey.fromHex("slh-dsa-sha2-128s-priv-" + SEED_48, true));
        assertThrows(IllegalArgumentException.class,
                () -> SlhDsaSha2128sPrivateKey.fromHex("ed25519-priv-0x" + SEED_48, true));
    }

    @Test
    @DisplayName("BCS serialize/deserialize round-trips for public key, private key, and signature")
    void bcsRoundTrips() throws IOException {
        SlhDsaSha2128sPrivateKey priv = SlhDsaSha2128sPrivateKey.generate();
        byte[] pkBytes = priv.publicKey().bcsToBytes();
        assertEquals(1 + SlhDsaSha2128sPublicKey.LENGTH, pkBytes.length);
        assertEquals(priv.publicKey(), SlhDsaSha2128sPublicKey.deserialize(new Deserializer(pkBytes)));

        byte[] skBytes = priv.bcsToBytes();
        assertEquals(1 + SlhDsaSha2128sPrivateKey.LENGTH, skBytes.length);
        assertEquals(priv, SlhDsaSha2128sPrivateKey.deserialize(new Deserializer(skBytes)));

        SlhDsaSha2128sSignature sig = priv.sign("hello aptos".getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = sig.bcsToBytes();
        // ULEB128(7856) is 2 bytes (0xb0 0x3d) + 7856 signature bytes
        assertEquals(2 + SlhDsaSha2128sSignature.LENGTH, sigBytes.length);
        assertEquals(sig, SlhDsaSha2128sSignature.deserialize(new Deserializer(sigBytes)));
    }

    @Test
    @DisplayName("AnyPublicKey variant 5 and AnySignature variants 0 and 4 serialize/deserialize")
    void anyPublicKeyAndAnySignatureVariants() throws IOException {
        SlhDsaSha2128sPrivateKey priv = fixedKey();
        AnyPublicKey anyPk = new AnyPublicKey(priv.publicKey());
        assertEquals(AnyPublicKey.VARIANT_SLH_DSA_SHA2_128S, anyPk.getVariant());
        assertEquals("0520" + EXPECTED_PUB, anyPk.bcsToHex());
        AnyPublicKey decodedPk = AnyPublicKey.deserialize(new Deserializer(anyPk.bcsToBytes()));
        assertEquals(AnyPublicKey.VARIANT_SLH_DSA_SHA2_128S, decodedPk.getVariant());
        assertEquals(priv.publicKey(), decodedPk.getPublicKey());
        assertThrows(IllegalArgumentException.class, () -> new AnyPublicKey(null));

        byte[] msg = "hello aptos".getBytes(StandardCharsets.UTF_8);
        SlhDsaSha2128sSignature slhSig = priv.sign(msg);
        assertTrue(anyPk.verifySignature(msg, slhSig));

        AnySignature anySlhSig = new AnySignature(slhSig);
        assertEquals(AnySignature.VARIANT_SLH_DSA_SHA2_128S, anySlhSig.getVariant());
        byte[] slhSigWire = anySlhSig.bcsToBytes();
        assertEquals(AnySignature.VARIANT_SLH_DSA_SHA2_128S, slhSigWire[0]);
        AnySignature decodedSlhSig = AnySignature.deserialize(new Deserializer(slhSigWire));
        assertEquals(anySlhSig, decodedSlhSig);
        assertEquals(anySlhSig.hashCode(), decodedSlhSig.hashCode());
        assertTrue(anySlhSig.toString().contains("variant=4"));

        // Ed25519 signature in AnySignature (variant 0)
        Signature edSig = Ed25519PrivateKey.generate().sign(msg);
        AnySignature anyEdSig = new AnySignature(edSig);
        assertEquals(AnySignature.VARIANT_ED25519, anyEdSig.getVariant());
        AnySignature decodedEdSig = AnySignature.deserialize(new Deserializer(anyEdSig.bcsToBytes()));
        assertEquals(anyEdSig, decodedEdSig);
        assertNotEquals(anyEdSig, anySlhSig);
        assertNotEquals(anyEdSig, "not-a-sig");
        assertEquals(anyEdSig, anyEdSig);

        // Error branches on AnySignature
        assertThrows(IllegalArgumentException.class, () -> new AnySignature(null));
        Signature customSig = new Signature(new byte[Signature.LENGTH]) {};
        assertThrows(IllegalArgumentException.class, () -> new AnySignature(customSig));
        assertThrows(IOException.class, () -> AnySignature.deserialize(new Deserializer(new byte[]{99})));
    }

    @Test
    @DisplayName("SingleKey authKey and accountAddress match TypeScript SDK derivation")
    void authKeyAndAddressDerivation() {
        SlhDsaSha2128sPublicKey pub = fixedKey().publicKey();
        assertEquals(EXPECTED_AUTH_KEY, pub.authKey().toHexString());
        assertEquals(EXPECTED_AUTH_KEY, pub.accountAddress().toHexString());
        assertEquals(pub.authKey(), new AnyPublicKey(pub).authKey());
        assertEquals(pub.accountAddress(), new AnyPublicKey(pub).accountAddress());
    }

    /** HD vectors ported verbatim from aptos-ts-sdk tests/unit/hdKey.test.ts (PR #802). */
    @Test
    @DisplayName("HD derivation matches all six TypeScript SDK SLIP-0010 vectors")
    void hdDerivationVectorsFromTsSdk() {
        byte[] seed = HexUtils.hexToBytes("000102030405060708090a0b0c0d0e0f");
        String[][] vectors = {
                {"m", "53c14b2681fcca8600d3ac7ce3459d6ceece7abc0a3b4c0376fc845c1b6503d66c5107d9484171c02f9eb0409849fceb"},
                {"m/0'", "473049c70d5414379363b94f52de6b194c9f1651cb6a11b80cfee19f6ca67975201cdbec137ff57e2e9cd434fca0680d"},
                {"m/0'/1'", "b609bf21df9baae65045e2bb8c200e97fbf86201f58e4187197a60bfc827158e6ba452ffe50f57849c022b24d01ac123"},
                {"m/0'/1'/2'", "60b55f07c62916f7f27a96be371a9e87adedb9acabd84d25c41e8aa5c8c326e2e64024132f8833181bc2ab008541ef2b"},
                {"m/0'/1'/2'/2'", "fb7242320fc3bc620587cfdfa54c2abed7d034f6c616ade3a9ad1780a099b268ed7a5bea0297711e997ff199b24bb82b"},
                {"m/0'/1'/2'/2'/1000000000'", "c72d4e3fb352a785aa375be20e0767ae20edbce1e18de90e0ea6e9d1677d17e84a410bb833bf091655dfabde17cc2011"},
        };
        for (String[] v : vectors) {
            assertEquals(v[1], SlhDsaSha2128sPrivateKey.fromDerivationPathInner(v[0], seed).toHexString(),
                    "path " + v[0]);
        }

        assertTrue(Bip44Utils.isValidGenericHardenedPath("m"));
        assertTrue(Bip44Utils.isValidGenericHardenedPath("m/44'/637'/0'/0'/0'"));
        assertFalse(Bip44Utils.isValidGenericHardenedPath(null));
        assertFalse(Bip44Utils.isValidGenericHardenedPath("m/44/637/0/0/0"));
        assertThrows(IllegalArgumentException.class,
                () -> SlhDsaSha2128sPrivateKey.fromDerivationPath("m/44/637/0/0/0", TEST_MNEMONIC));
        assertThrows(IllegalArgumentException.class,
                () -> Bip44Utils.deriveSlhDsaSha2128sThreeSeedsFromSeed("invalid", seed));
    }

    @Test
    @DisplayName("SingleKeyAuthenticator, SlhDsaSha2128sAccount, and SignedTransaction wire format")
    void accountSingleKeyAuthenticatorAndSignedTransaction() throws Exception {
        SlhDsaSha2128sAccount account = SlhDsaSha2128sAccount.fromPrivateKey(fixedKey());
        assertEquals(EXPECTED_AUTH_KEY, account.getAccountAddress().toHexString());
        assertEquals(Account.SigningScheme.SLH_DSA_SHA2_128S, account.getSigningScheme());
        assertEquals(fixedKey(), account.getPrivateKey());
        assertEquals(fixedKey().toAIP80String(), account.getPrivateKeyHex());
        assertEquals(fixedKey().publicKey().toString(), account.getPublicKeyHex());
        assertTrue(account.toString().contains("SlhDsaSha2128sAccount"));

        // fromPrivateKeyHex + custom rotated address
        assertEquals(account.getAccountAddress(),
                SlhDsaSha2128sAccount.fromPrivateKeyHex(fixedKey().toAIP80String()).getAccountAddress());
        AccountAddress rotated = AccountAddress.fromHex(
                "0x0000000000000000000000000000000000000000000000000000000000000042");
        SlhDsaSha2128sAccount rotatedAccount = SlhDsaSha2128sAccount.fromPrivateKey(fixedKey(), rotated);
        assertEquals(rotated, rotatedAccount.getAccountAddress());
        assertThrows(IllegalArgumentException.class,
                () -> SlhDsaSha2128sAccount.fromPrivateKey((SlhDsaSha2128sPrivateKey) null));

        // Message signing and SingleKeyAuthenticator round-trip
        byte[] msg = "hello aptos".getBytes(StandardCharsets.UTF_8);
        AccountAuthenticator auth = account.signWithAuthenticator(msg);
        assertInstanceOf(SingleKeyAuthenticator.class, auth);
        SingleKeyAuthenticator singleAuth = (SingleKeyAuthenticator) auth;
        assertArrayEquals(account.getPublicKey().authKey().toBytes(), singleAuth.getAuthenticationKey());
        assertArrayEquals(account.getPublicKey().toBytes(), singleAuth.getPublicKey());
        assertEquals(SlhDsaSha2128sSignature.LENGTH, singleAuth.getSignature().length);
        assertEquals(account.getPublicKey(), singleAuth.getPublicKeyObject());
        assertTrue(account.verifySignature(msg, singleAuth.getSignatureObject()));
        assertTrue(singleAuth.toString().contains("SingleKeyAuthenticator"));

        byte[] authBytes = singleAuth.bcsToBytes();
        assertEquals(SingleKeyAuthenticator.VARIANT_SINGLE_KEY, authBytes[0]);
        assertEquals(AnyPublicKey.VARIANT_SLH_DSA_SHA2_128S, authBytes[1]);
        SingleKeyAuthenticator decodedAuth = SingleKeyAuthenticator.deserialize(new Deserializer(authBytes));
        assertEquals(account.getPublicKey(), decodedAuth.getPublicKeyObject());
        assertEquals(singleAuth.getSignatureObject(), decodedAuth.getSignatureObject());

        // SingleKeyAuthenticator accepts pre-wrapped AnyPublicKey and rejects bad variant on deserialize
        SingleKeyAuthenticator wrappedAuth = new SingleKeyAuthenticator(
                new AnyPublicKey(account.getPublicKey()), singleAuth.getSignatureObject());
        assertArrayEquals(authBytes, wrappedAuth.bcsToBytes());
        assertThrows(IOException.class,
                () -> SingleKeyAuthenticator.deserialize(new Deserializer(new byte[]{0})));

        // Transaction flow: SignedTransaction wraps SingleKey in SingleSender (variant 4).
        RawTransaction raw = sampleRawTransaction(account.getAccountAddress());
        AccountAuthenticator txnAuth = account.signTransactionWithAuthenticator(raw);
        SignedTransaction signed = new SignedTransaction(raw, txnAuth);
        byte[] signedBytes = signed.bcsToBytes();
        byte[] rawBytes = raw.bcsToBytes();
        assertEquals(4, signedBytes[rawBytes.length]); // TransactionAuthenticator SingleSender
        assertEquals(2, signedBytes[rawBytes.length + 1]); // AccountAuthenticator SingleKey
        assertEquals(5, signedBytes[rawBytes.length + 2]); // AnyPublicKey SLH-DSA-SHA2-128s
        assertInstanceOf(SingleKeyAuthenticator.class, signed.getAuthenticator());

        // Verify the raw transaction signature matches SHA3-256("APTOS::RawTransaction") || BCS(raw)
        byte[] prefixHash = CryptoUtils.sha3_256("APTOS::RawTransaction".getBytes(StandardCharsets.UTF_8));
        byte[] signingMessage = new byte[prefixHash.length + rawBytes.length];
        System.arraycopy(prefixHash, 0, signingMessage, 0, prefixHash.length);
        System.arraycopy(rawBytes, 0, signingMessage, prefixHash.length, rawBytes.length);
        assertTrue(account.verifySignature(signingMessage, ((SingleKeyAuthenticator) txnAuth).getSignatureObject()));
    }

    @Test
    @DisplayName("Account scheme-aware factories support SLH-DSA-SHA2-128s and Ed25519")
    void accountFactoryMethods() {
        Account generatedSlh = Account.generate(Account.SigningScheme.SLH_DSA_SHA2_128S);
        assertInstanceOf(SlhDsaSha2128sAccount.class, generatedSlh);
        byte[] msg = "hello aptos".getBytes(StandardCharsets.UTF_8);
        assertTrue(generatedSlh.verifySignature(msg, generatedSlh.sign(msg)));

        Account generatedEd = Account.generate(Account.SigningScheme.ED25519);
        assertInstanceOf(Ed25519Account.class, generatedEd);

        assertThrows(IllegalArgumentException.class, () -> Account.generate((Account.SigningScheme) null));
        assertThrows(IllegalArgumentException.class, () -> Account.generate(Account.SigningScheme.MULTI_KEY));

        SlhDsaSha2128sAccount fromKey = Account.fromPrivateKey(fixedKey());
        assertEquals(EXPECTED_AUTH_KEY, fromKey.getAccountAddress().toHexString());
        AccountAddress custom = AccountAddress.fromHex(
                "0x0000000000000000000000000000000000000000000000000000000000000007");
        assertEquals(custom, Account.fromPrivateKey(fixedKey(), custom).getAccountAddress());

        String path = "m/44'/637'/0'/0'/0'";
        Account derivedSlh = Account.fromDerivationPath(path, TEST_MNEMONIC, Account.SigningScheme.SLH_DSA_SHA2_128S);
        assertInstanceOf(SlhDsaSha2128sAccount.class, derivedSlh);
        assertEquals(
                SlhDsaSha2128sAccount.createFromDerivationPath(path, TEST_MNEMONIC).getAccountAddress(),
                derivedSlh.getAccountAddress());

        Account derivedEd = Account.fromDerivationPath(path, TEST_MNEMONIC, Account.SigningScheme.ED25519);
        assertInstanceOf(Ed25519Account.class, derivedEd);

        assertThrows(IllegalArgumentException.class,
                () -> Account.fromDerivationPath(path, TEST_MNEMONIC, null));
        assertThrows(IllegalArgumentException.class,
                () -> Account.fromDerivationPath(path, TEST_MNEMONIC, Account.SigningScheme.MULTI_ED25519));
    }

    @Test
    @DisplayName("MultiKeyAuthenticator serializes SLH-DSA public keys and signatures with variants 5 and 4")
    void multiKeyAuthenticatorSupportsSlhDsa() throws IOException {
        SlhDsaSha2128sAccount slhAccount = SlhDsaSha2128sAccount.fromPrivateKey(fixedKey());
        Ed25519Account edAccount = Ed25519Account.generate();
        byte[] msg = "multikey pq".getBytes(StandardCharsets.UTF_8);

        MultiKeyAuthenticator auth = new MultiKeyAuthenticator(
                List.of(edAccount.getPublicKey(), slhAccount.getPublicKey()),
                List.of(slhAccount.sign(msg)),
                1,
                List.of(1));

        Serializer serializer = new Serializer();
        auth.serialize(serializer);
        byte[] bytes = serializer.toByteArray();

        // Parse back the BCS wire layout: variant(3) | vec<AnyPublicKey>(2) | threshold(1) | vec<AnySignature>(1) | bitmap(4)
        Deserializer d = new Deserializer(bytes);
        assertEquals(3, d.deserializeUleb128AsU32());
        assertEquals(2, d.deserializeUleb128AsU32());
        // Skip Ed25519 AnyPublicKey (variant 0 + length-prefixed 32 bytes)
        assertEquals(AnyPublicKey.VARIANT_ED25519, d.deserializeUleb128AsU32());
        assertEquals(Ed25519PublicKey.LENGTH, d.deserializeBytes().length);
        // SLH-DSA AnyPublicKey (variant 5 + length-prefixed 32 bytes)
        AnyPublicKey decodedSlhPk = AnyPublicKey.deserialize(d);
        assertEquals(AnyPublicKey.VARIANT_SLH_DSA_SHA2_128S, decodedSlhPk.getVariant());
        assertEquals(slhAccount.getPublicKey(), decodedSlhPk.getPublicKey());
        assertEquals((byte) 1, d.deserializeU8());
        // Signatures: count 1, AnySignature variant 4
        assertEquals(1, d.deserializeUleb128AsU32());
        AnySignature decodedSlhSig = AnySignature.deserialize(d);
        assertEquals(AnySignature.VARIANT_SLH_DSA_SHA2_128S, decodedSlhSig.getVariant());
        assertTrue(slhAccount.verifySignature(msg, decodedSlhSig.getSignature()));
        // Bitmap: bit 1 set (MSB-first => 0x40 0x00 0x00 0x00)
        assertArrayEquals(new byte[]{0x40, 0x00, 0x00, 0x00}, d.deserializeBytes());
        assertEquals(0, d.remaining());
    }

    @Test
    @DisplayName("equals and hashCode contracts for SLH-DSA key and signature types")
    void equalsAndHashCode() {
        SlhDsaSha2128sPrivateKey privA = fixedKey();
        SlhDsaSha2128sPrivateKey privB = SlhDsaSha2128sPrivateKey.fromHex(SEED_48);
        SlhDsaSha2128sPrivateKey privC = SlhDsaSha2128sPrivateKey.generate();
        assertEquals(privA, privA);
        assertEquals(privA, privB);
        assertEquals(privA.hashCode(), privB.hashCode());
        assertNotEquals(privA, privC);
        assertNotEquals(privA, null);
        assertNotEquals(privA, "other");

        SlhDsaSha2128sPublicKey pubA = privA.publicKey();
        SlhDsaSha2128sPublicKey pubB = privB.publicKey();
        SlhDsaSha2128sPublicKey pubC = privC.publicKey();
        assertEquals(pubA, pubA);
        assertEquals(pubA, pubB);
        assertEquals(pubA.hashCode(), pubB.hashCode());
        assertNotEquals(pubA, pubC);
        assertNotEquals(pubA, null);
        assertNotEquals(pubA, "other");

        byte[] msg = "eq".getBytes(StandardCharsets.UTF_8);
        SlhDsaSha2128sSignature sigA = privA.sign(msg);
        SlhDsaSha2128sSignature sigB = privB.sign(msg);
        assertEquals(sigA, sigB);
        assertEquals(sigA.hashCode(), sigB.hashCode());
    }
}
