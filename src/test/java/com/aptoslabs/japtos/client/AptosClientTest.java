package com.aptoslabs.japtos.client;

import com.aptoslabs.japtos.account.Account;
import com.aptoslabs.japtos.account.Ed25519Account;
import com.aptoslabs.japtos.api.AptosConfig;
import com.aptoslabs.japtos.client.dto.AccountInfo;
import com.aptoslabs.japtos.client.dto.LedgerInfo;
import com.aptoslabs.japtos.client.dto.PendingTransaction;
import com.aptoslabs.japtos.client.dto.Transaction;
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
 * Unit tests for {@link AptosClient} using a {@link RecordingHttpClient} so that all
 * request building, response parsing and error handling is exercised without a node.
 */
class AptosClientTest {

    private AptosClient clientWith(RecordingHttpClient http) {
        AptosConfig config = AptosConfig.builder()
                .fullnode("http://localhost:1234")
                .client(http)
                .build();
        return new AptosClient(config);
    }

    private SignedTransaction signedTransaction() throws Exception {
        Ed25519Account account = Account.generate();
        TransactionPayload payload = new EntryFunctionPayload(
                new ModuleId(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), new Identifier("coin")),
                new Identifier("transfer"), List.of(),
                List.of(new TransactionArgument.U64(1L)));
        RawTransaction raw = new RawTransaction(account.getAccountAddress(), 0L, payload,
                100000L, 100L, 1700000000L, 4L);
        AccountAuthenticator auth = account.signTransactionWithAuthenticator(raw);
        return new SignedTransaction(raw, auth);
    }

    @Test
    @DisplayName("getAccount issues a GET to /v1/accounts/<addr> and parses the response")
    void getAccount() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"sequence_number\":\"5\",\"authentication_key\":\"0xkey\"}");
        AccountInfo info = clientWith(http).getAccount(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"));
        assertEquals(5L, info.getSequenceNumber());
        assertTrue(http.lastRequest().url.contains("/v1/accounts/0x"));
    }

    @Test
    @DisplayName("getAccount surfaces non-2xx responses as AptosClientException")
    void getAccountError() {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(404, "not found");
        AptosClient client = clientWith(http);
        assertThrows(AptosClient.AptosClientException.class,
                () -> client.getAccount(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")));
    }

    @Test
    @DisplayName("getLedgerInfo parses chain metadata")
    void getLedgerInfo() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"chain_id\":4,\"ledger_version\":\"10\",\"ledger_timestamp\":\"1\"," +
                        "\"epoch\":\"1\",\"oldest_block_height\":\"0\",\"oldest_block_timestamp\":\"0\"," +
                        "\"node_role\":\"full_node\",\"oldest_ledger_timestamp\":\"0\",\"block_height\":\"3\"}");
        LedgerInfo info = clientWith(http).getLedgerInfo();
        assertEquals(4, info.getChainId());
        assertEquals(3L, info.getBlockHeight());
        assertTrue(http.lastRequest().url.endsWith("/v1"));
    }

    @Test
    @DisplayName("getLedgerInfo error path throws")
    void getLedgerInfoError() {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(500, "boom");
        AptosClient client = clientWith(http);
        assertThrows(AptosClient.AptosClientException.class, client::getLedgerInfo);
    }

    @Test
    @DisplayName("getAccountCoinAmount parses the raw balance body for default and explicit assets")
    void getAccountCoinAmount() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "987654321")
                .enqueue(200, "42");
        AptosClient client = clientWith(http);

        assertEquals(987654321L, client.getAccountCoinAmount(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")));
        assertTrue(http.lastRequest().url.contains("/balance/0x1::aptos_coin::AptosCoin"));

        assertEquals(42L, client.getAccountCoinAmount(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001"), "0x1::custom::Coin"));
        assertTrue(http.lastRequest().url.contains("/balance/0x1::custom::Coin"));
    }

    @Test
    @DisplayName("getAccountCoinAmount error path throws")
    void getAccountCoinAmountError() {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(400, "bad");
        AptosClient client = clientWith(http);
        assertThrows(AptosClient.AptosClientException.class,
                () -> client.getAccountCoinAmount(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")));
    }

    @Test
    @DisplayName("getNextSequenceNumber returns the account sequence number")
    void getNextSequenceNumber() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"sequence_number\":\"11\",\"authentication_key\":\"0xkey\"}");
        assertEquals(11L, clientWith(http).getNextSequenceNumber(AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")));
    }

    @Test
    @DisplayName("submitTransaction posts BCS bytes and returns the pending transaction")
    void submitTransactionDefault() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(202, "{\"hash\":\"0xpending\"}");
        AptosClient client = clientWith(http);
        PendingTransaction pending = client.submitTransaction(signedTransaction());
        assertEquals("0xpending", pending.getHash());

        RecordingHttpClient.Request req = http.lastRequest();
        assertEquals("POST", req.method);
        assertTrue(req.url.endsWith("/v1/transactions"));
        assertEquals("application/x.aptos.signed_transaction+bcs", req.headers.get("Content-Type"));
        assertNotNull(req.byteBody);
    }

    @Test
    @DisplayName("submitTransaction error path throws")
    void submitTransactionError() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(400, "rejected");
        AptosClient client = clientWith(http);
        SignedTransaction signed = signedTransaction();
        assertThrows(AptosClient.AptosClientException.class, () -> client.submitTransaction(signed));
    }

    @Test
    @DisplayName("submitTransaction delegates to a configured TransactionSubmitter")
    void submitTransactionViaSubmitter() throws Exception {
        AptosConfig config = AptosConfig.builder()
                .fullnode("http://localhost:1234")
                .transactionSubmitter(tx -> new PendingTransaction("0xsubmitter"))
                .build();
        AptosClient client = new AptosClient(config);
        assertEquals("0xsubmitter", client.submitTransaction(signedTransaction()).getHash());
    }

    @Test
    @DisplayName("getTransactionByHash parses a committed transaction")
    void getTransactionByHash() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"version\":\"1\",\"hash\":\"0xh\",\"success\":true," +
                        "\"vm_status\":\"Executed successfully\",\"type\":\"user_transaction\"," +
                        "\"sequence_number\":\"0\",\"max_gas_amount\":\"1\",\"gas_unit_price\":\"1\"," +
                        "\"expiration_timestamp_secs\":\"1\",\"timestamp\":\"1\",\"gas_used\":\"1\"}");
        Transaction txn = clientWith(http).getTransactionByHash("0xh");
        assertEquals("0xh", txn.getHash());
        assertTrue(http.lastRequest().url.contains("/v1/transactions/by_hash/0xh"));
    }

    @Test
    @DisplayName("getTransactionByHash error path throws")
    void getTransactionByHashError() {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(404, "missing");
        AptosClient client = clientWith(http);
        assertThrows(AptosClient.AptosClientException.class, () -> client.getTransactionByHash("0xh"));
    }

    @Test
    @DisplayName("waitForTransaction returns a successfully committed transaction")
    void waitForTransactionSuccess() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"hash\":\"0xh\",\"success\":true," +
                        "\"vm_status\":\"Executed successfully\",\"type\":\"user_transaction\"}");
        Transaction txn = clientWith(http).waitForTransaction("0xh");
        assertTrue(txn.isSuccess());
    }

    @Test
    @DisplayName("waitForTransaction throws when the VM reports failure")
    void waitForTransactionFailure() {
        RecordingHttpClient http = new RecordingHttpClient()
                .enqueue(200, "{\"hash\":\"0xh\",\"success\":false," +
                        "\"vm_status\":\"MOVE_ABORT\",\"type\":\"user_transaction\"}");
        AptosClient client = clientWith(http);
        AptosClient.AptosClientException ex = assertThrows(AptosClient.AptosClientException.class,
                () -> client.waitForTransaction("0xh"));
        assertTrue(ex.getMessage().contains("MOVE_ABORT"));
    }

    @Test
    @DisplayName("waitForTransaction surfaces a non-2xx lookup as an exception")
    void waitForTransactionHttpError() {
        RecordingHttpClient http = new RecordingHttpClient().enqueue(500, "err");
        AptosClient client = clientWith(http);
        assertThrows(AptosClient.AptosClientException.class, () -> client.waitForTransaction("0xh"));
    }

    @Test
    @DisplayName("Convenience constructors and getConfig wire up a usable client")
    void convenienceConstructors() {
        AptosClient byNetwork = new AptosClient(AptosConfig.Network.TESTNET);
        assertNotNull(byNetwork.getConfig());
        assertEquals(AptosConfig.Network.TESTNET, byNetwork.getConfig().getNetwork());

        AptosClient byUrl = new AptosClient("http://localhost:8080");
        assertEquals("http://localhost:8080", byUrl.getConfig().getFullnode());
    }

    @Test
    @DisplayName("AptosClientException carries message and optional cause")
    void exceptionConstructors() {
        AptosClient.AptosClientException withMsg = new AptosClient.AptosClientException("m");
        assertEquals("m", withMsg.getMessage());
        Throwable cause = new RuntimeException("c");
        AptosClient.AptosClientException withCause = new AptosClient.AptosClientException("m", cause);
        assertSame(cause, withCause.getCause());
    }
}
