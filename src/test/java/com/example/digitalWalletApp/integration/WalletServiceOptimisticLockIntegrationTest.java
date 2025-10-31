package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletServiceOptimisticLockIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        user = new User();
        user.setName("John");
        user.setEmail("john@example.com");
        user.setPassword("password");

        // ✅ Save user first
        user = userRepository.saveAndFlush(user);

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(100.0);

        // ✅ Then save wallet
        wallet = walletRepository.saveAndFlush(wallet);
    }

    // --------------------------------------------------------------------
    // ✅ TEST: Optimistic Locking for loadMoney()
    // --------------------------------------------------------------------
    @Test
    void testOptimisticLockingRetryMechanism() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> task1 = () -> {
            walletService.loadMoney(user, 10.0, UUID.randomUUID().toString());
            return null;
        };

        Callable<Void> task2 = () -> {
            walletService.loadMoney(user, 5.0, UUID.randomUUID().toString());
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);
        /*
        Submits both tasks to the executor service (thread pool).
        Each task runs asynchronously on a separate thread.
        Returns a Future, which represents the pending result of each computation.
        🧩 The threads now start racing to modify the same wallet record.
         */

        try {
            f1.get();
            f2.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                System.out.println("⚠️ One transaction failed due to version conflict (expected).");
            } else {
                throw e;
            }
        }

        executor.shutdown();

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();

        System.out.println("✅ Final Balance (loadMoney): " + updatedWallet.getBalance());
        System.out.println("✅ Final Version: " + updatedWallet.getVersion());

        assertThat(updatedWallet.getBalance()).isEqualTo(115.0);
        assertThat(updatedWallet.getVersion()).isGreaterThan(1L);
    }

    // --------------------------------------------------------------------
    // ✅ TEST: Optimistic Locking for transferAmount()
    // --------------------------------------------------------------------
    @Test
    void testOptimisticLockingDuringTransfer() throws Exception {
        // Sender
        User sender = new User();
        sender.setName("Alice");
        sender.setEmail("alice@example.com");
        sender.setPassword("pass");
        sender = userRepository.saveAndFlush(sender);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(sender);
        senderWallet.setBalance(200.0);
        senderWallet = walletRepository.saveAndFlush(senderWallet);

        // Recipient
        User recipient = new User();
        recipient.setName("Bob");
        recipient.setEmail("bob@example.com");
        recipient.setPassword("pass");
        recipient = userRepository.saveAndFlush(recipient);

        Wallet recipientWallet = new Wallet();
        recipientWallet.setUser(recipient);
        recipientWallet.setBalance(50.0);
        recipientWallet = walletRepository.saveAndFlush(recipientWallet);

        // ✅ Make variables effectively final for lambda use
        final User s = sender;
        final User r = recipient;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> task1 = () -> {
            walletService.transferAmount(s, r.getId(), 40.0, UUID.randomUUID().toString());
            return null;
        };

        Callable<Void> task2 = () -> {
            walletService.transferAmount(s, r.getId(), 30.0, UUID.randomUUID().toString());
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        try {
            f1.get();
            f2.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                System.out.println("⚠️ One transfer failed due to version conflict (expected).");
            } else {
                throw e;
            }
        }

        executor.shutdown();

        Wallet updatedSender = walletRepository.findById(senderWallet.getId()).orElseThrow();
        Wallet updatedRecipient = walletRepository.findById(recipientWallet.getId()).orElseThrow();

        System.out.println("✅ Sender Final Balance: " + updatedSender.getBalance());
        System.out.println("✅ Recipient Final Balance: " + updatedRecipient.getBalance());
        System.out.println("✅ Sender Final Version: " + updatedSender.getVersion());
        System.out.println("✅ Recipient Final Version: " + updatedRecipient.getVersion());

        // Check that total money in system is preserved (no loss/gain)
        double total = updatedSender.getBalance() + updatedRecipient.getBalance();
        assertThat(total).isEqualTo(250.0);

        // At least one transfer should succeed, so sender’s balance < 200
        assertThat(updatedSender.getBalance()).isLessThan(200.0);
        assertThat(updatedSender.getVersion()).isGreaterThan(1L);
    }

    // --------------------------------------------------------------------
// ✅ TEST: Rollback when exception occurs in performTransfer()
// --------------------------------------------------------------------
    @Test
    void testRollbackOnTransferException() {
        // Sender
        User sender = new User();
        sender.setName("RollbackSender");
        sender.setEmail("rollback-sender@example.com");
        sender.setPassword("pass");
        sender = userRepository.saveAndFlush(sender);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(sender);
        senderWallet.setBalance(100.0);
        senderWallet = walletRepository.saveAndFlush(senderWallet);

        // Recipient (nonexistent ID to trigger exception)
        Long invalidRecipientId = 9999L;

        String txnId = UUID.randomUUID().toString();

        System.out.println("\n🚨 Starting rollback test for invalid recipient...");

        try {
            walletService.transferAmount(sender, invalidRecipientId, 50.0, txnId);
        } catch (Exception e) {
            System.out.println("💥 Expected failure: " + e.getMessage());
        }

        Wallet afterWallet = walletRepository.findById(senderWallet.getId()).orElseThrow();

        System.out.println("💰 Sender Balance After Failed TXN: " + afterWallet.getBalance());
        System.out.println("🧾 Sender Version: " + afterWallet.getVersion());

        // ✅ Assert: No balance deduction should happen
        assertThat(afterWallet.getBalance()).isEqualTo(100.0);
        assertThat(transactionRepository.findByTransactionId(txnId)).isEmpty();
    }

    @Test
    void testRollbackWhenInsufficientBalance() {
        User sender = new User();
        sender.setName("PoorGuy");
        sender.setEmail("poor@example.com");
        sender.setPassword("pass");
        sender = userRepository.saveAndFlush(sender);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(sender);
        senderWallet.setBalance(20.0);
        senderWallet = walletRepository.saveAndFlush(senderWallet);

        User receiver = new User();
        receiver.setName("RichGuy");
        receiver.setEmail("rich@example.com");
        receiver.setPassword("pass");
        receiver = userRepository.saveAndFlush(receiver);

        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(100.0);
        receiverWallet = walletRepository.saveAndFlush(receiverWallet);

        System.out.println("\n🚨 Starting rollback test for insufficient funds...");

        try {
            walletService.transferAmount(sender, receiver.getId(), 200.0, UUID.randomUUID().toString());
        } catch (Exception e) {
            System.out.println("💥 Expected failure: " + e.getMessage());
        }

        Wallet senderAfter = walletRepository.findById(senderWallet.getId()).orElseThrow();
        Wallet receiverAfter = walletRepository.findById(receiverWallet.getId()).orElseThrow();

        System.out.println("💰 Sender After: " + senderAfter.getBalance());
        System.out.println("💰 Receiver After: " + receiverAfter.getBalance());

        // ✅ Assert rollback: no balance changes
        assertThat(senderAfter.getBalance()).isEqualTo(20.0);
        assertThat(receiverAfter.getBalance()).isEqualTo(100.0);
    }

    @Test
    void testDuplicateTransactionId() {
        // Setup user
        User user = new User();
        user.setName("DuplicateTxnUser");
        user.setEmail("dupe@example.com");
        user.setPassword("pass");
        user = userRepository.saveAndFlush(user);

        // Setup wallet
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(100.0);
        wallet = walletRepository.saveAndFlush(wallet);

        // ✅ Use same transaction ID for both calls
        String sameTxnId = UUID.randomUUID().toString();

        System.out.println("\n🧾 Testing duplicate transaction ID handling...");

        // First transaction — should succeed
        walletService.loadMoney(user, 20.0, sameTxnId);

        // Second transaction — should fail due to duplicate ID
        try {
            walletService.loadMoney(user, 20.0, sameTxnId);
            System.out.println("⚠️ Duplicate transaction was processed (unexpected)");
        } catch (Exception e) {
            System.out.println("✅ Duplicate transaction prevented: " + e.getMessage());
        }

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        System.out.println("💰 Final Balance: " + updatedWallet.getBalance());
        System.out.println("🧾 Version: " + updatedWallet.getVersion());

        // ✅ Assert: Only one transaction recorded for that ID
        assertThat(transactionRepository.findByTransactionId(sameTxnId)).isPresent();

        // ✅ Assert: Balance only updated once
        assertThat(updatedWallet.getBalance()).isEqualTo(120.0);
    }


}

/*
Even if both hit “at the same time”,
one still succeeds because the database itself executes operations sequentially — never truly simultaneously.

So whichever transaction reaches the database first to perform the UPDATE ... WHERE version = ? wins,
and the other fails because the version no longer matches.
 */