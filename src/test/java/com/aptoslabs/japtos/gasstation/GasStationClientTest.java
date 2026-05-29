package com.aptoslabs.japtos.gasstation;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.core.AccountAddress;
import com.aptoslabs.japtos.testutil.RecordingHttpClient;
import com.aptoslabs.japtos.transaction.RawTransaction;
import com.aptoslabs.japtos.transaction.SignedTransaction;
import com.aptoslabs.japtos.transaction.authenticator.AccountAuthenticator;
import com.aptoslabs.japtos.types.EntryFunctionPayload;
import com.aptoslabs.japtos.types.Identifier;
import com.aptoslabs.japtos.types.ModuleId;
import com.aptoslabs.japtos.types.TransactionArgument;
import com.aptoslabs.japtos.types.TransactionPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GasStationClient} and {@link GasStationTransactionSubmitter}
 * using a recording {@link RecordingHttpClient} instead of a live gas station.
 */
class GasStationClientTest {

    private SignedTransaction signedTransaction() throws Exception {
        Ed25519Account account = Account.generate();
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"),
                List.of(),
                List.of(new TransactionArgument.U64(1L)));
        RawTransaction raw = new RawTransaction(account.getAccountAddress(), 0L, payload,
                100000L, 100L, 1700000000L, 4L);
        AccountAuthenticator auth = account.signTransactionWithAuthenticator(raw);
        return new SignedTransaction(raw, auth);
    }

    @Test
    @DisplayName("signAndSubmitTransaction posts to the resolved URL and returns the hash")
    void signAndSubmitSuccess() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET)
                .apiKey("secret")
                .build();
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"transactionHash\":\"0xdeadbeef\"}");
        GasStationClient client = new GasStationClient(options, http);

        SignedTransaction signed = signedTransaction();
        GasStationClient.GasStationResponse response = client.signAndSubmitTransaction(
                signed, signed.getAuthenticator(), null, null);

        assertEquals("0xdeadbeef", response.getTransactionHash());
        RecordingHttpClient.Request req = http.lastRequest();
        assertEquals("POST", req.method);
        // Default env "prod" -> https://api.testnet.aptoslabs.com/gs/v1/api/transaction/signAndSubmit
        assertTrue(req.url.contains("api.testnet.aptoslabs.com/gs/v1/api/transaction/signAndSubmit"), req.url);
        assertEquals("Bearer secret", req.headers.get("Authorization"));
        assertEquals("application/json", req.headers.get("Content-Type"));
    }

    @Test
    @DisplayName("A custom baseUrl overrides the network-derived URL and includes secondary signers")
    void baseUrlOverrideAndSecondaryAuth() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .apiKey("secret")
                .baseUrl("https://my-gas-station.example")
                .build();
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"transactionHash\":\"0xabc\"}");
        GasStationClient client = new GasStationClient(options, http);

        SignedTransaction signed = signedTransaction();
        client.signAndSubmitTransaction(signed, signed.getAuthenticator(),
                List.of(signed.getAuthenticator()), "recaptcha-token");

        RecordingHttpClient.Request req = http.lastRequest();
        assertTrue(req.url.startsWith("https://my-gas-station.example/api/transaction/signAndSubmit"), req.url);
        String body = new String(req.byteBody);
        assertTrue(body.contains("additionalSignersAuth"));
        assertTrue(body.contains("recaptchaToken"));
    }

    @Test
    @DisplayName("Non-2xx responses raise a GasStationClientException")
    void httpErrorThrows() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").build();
        RecordingHttpClient http = new RecordingHttpClient().enqueue(401, "Unauthorized");
        GasStationClient client = new GasStationClient(options, http);

        SignedTransaction signed = signedTransaction();
        assertThrows(GasStationClient.GasStationClientException.class,
                () -> client.signAndSubmitTransaction(signed, signed.getAuthenticator(), null, null));
    }

    @Test
    @DisplayName("A response without a transaction hash is rejected")
    void missingHashThrows() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").build();
        RecordingHttpClient http = new RecordingHttpClient().enqueue(200, "{}");
        GasStationClient client = new GasStationClient(options, http);

        SignedTransaction signed = signedTransaction();
        assertThrows(GasStationClient.GasStationClientException.class,
                () -> client.signAndSubmitTransaction(signed, signed.getAuthenticator(), null, null));
    }

    @Test
    @DisplayName("GasStationTransactionSubmitter wraps the txn with a fee payer and returns a PendingTransaction")
    void transactionSubmitter() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").build();
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"transactionHash\":\"0xfeed\"}");
        GasStationClient client = new GasStationClient(options, http);
        AccountAddress feePayer = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000002");

        GasStationTransactionSubmitter submitter = new GasStationTransactionSubmitter(client, feePayer);
        assertSame(client, submitter.getClient());

        PendingTransaction pending = submitter.submitTransaction(signedTransaction());
        assertEquals("0xfeed", pending.getHash());

        // The options-based constructor builds its own client.
        GasStationTransactionSubmitter viaOptions =
                new GasStationTransactionSubmitter(options, feePayer);
        assertNotNull(viaOptions.getClient());
    }

    @Test
    @DisplayName("A non-prod env is woven into the derived host name")
    void nonProdEnvUrl() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").env("staging").build();
        RecordingHttpClient http = new RecordingHttpClient().enqueue(200, "{\"transactionHash\":\"0x1\"}");
        GasStationClient client = new GasStationClient(options, http);
        SignedTransaction signed = signedTransaction();
        client.signAndSubmitTransaction(signed, signed.getAuthenticator(), null, null);
        assertTrue(http.lastRequest().url.contains("api.testnet.staging.aptoslabs.com"), http.lastRequest().url);
    }

    @Test
    @DisplayName("An object lacking bcsToBytes is rejected before any HTTP call")
    void transactionWithoutBcsBytes() throws Exception {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").build();
        RecordingHttpClient http = new RecordingHttpClient();
        GasStationClient client = new GasStationClient(options, http);
        SignedTransaction signed = signedTransaction();
        assertThrows(GasStationClient.GasStationClientException.class,
                () -> client.signAndSubmitTransaction("not-a-transaction", signed.getAuthenticator(), null, null));
        assertEquals(0, http.requestCount());
    }

    @Test
    @DisplayName("The default (HttpClientImpl-backed) constructor is available")
    void defaultConstructor() {
        GasStationClientOptions options = new GasStationClientOptions.Builder()
                .network(AptosConfig.Network.TESTNET).apiKey("secret").build();
        assertNotNull(new GasStationClient(options));
    }

    @Test
    @DisplayName("The GasStationResponse setter/getter pair round-trips")
    void responseAccessor() {
        GasStationClient.GasStationResponse response = new GasStationClient.GasStationResponse();
        response.setTransactionHash("0x123");
        assertEquals("0x123", response.getTransactionHash());
    }

    @Test
    @DisplayName("GasStationClientException supports message and message+cause")
    void exceptionConstructors() {
        GasStationClient.GasStationClientException msg =
                new GasStationClient.GasStationClientException("m");
        assertEquals("m", msg.getMessage());
        Throwable cause = new RuntimeException("c");
        GasStationClient.GasStationClientException withCause =
                new GasStationClient.GasStationClientException("m", cause);
        assertSame(cause, withCause.getCause());
    }
}
