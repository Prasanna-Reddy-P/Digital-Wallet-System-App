package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.config.location=classpath:application-test.properties")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class WalletRepositoryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(WalletRepositoryIntegrationTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testUserWalletTransactionPersistence() {
        log.info("=== TEST: User, Wallet, Transaction Persistence ===");

        // --- Create & save User ---
        User user = new User("Alice", "alice@example.com", "password123");
        user = userRepository.save(user);
        log.info("Saved User: id={}, email={}", user.getId(), user.getEmail());

        // --- Create & save Wallet linked to User ---
        Wallet wallet = new Wallet(user);
        wallet.setBalance(500.0);
        walletRepository.save(wallet);
        log.info("Saved Wallet for User {}: balance={}", user.getEmail(), wallet.getBalance());

        // --- Create & save Transaction linked to User ---
        Transaction transaction = new Transaction(user, 100.0, "CREDIT");
        transactionRepository.save(transaction);
        log.info("Saved Transaction for User {}: amount={}, type={}", user.getEmail(), transaction.getAmount(), transaction.getType());

        // --- Verify User ---
        Optional<User> foundUser = userRepository.findById(user.getId());
        assertThat(foundUser).isPresent();
        log.info("Verified User exists with email: {}", foundUser.get().getEmail());

        // --- Verify Wallet ---
        Optional<Wallet> foundWallet = walletRepository.findByUser(user);
        assertThat(foundWallet).isPresent();
        log.info("Verified Wallet exists for User {}: balance={}", user.getEmail(), foundWallet.get().getBalance());

        // --- Verify Transaction ---
        List<Transaction> transactions = transactionRepository.findByUser(user);
        assertThat(transactions).hasSize(1);
        Transaction tx = transactions.get(0);
        log.info("Verified Transaction exists for User {}: amount={}, type={}", user.getEmail(), tx.getAmount(), tx.getType());
    }

    @Test
    void testMultipleTransactionsAndTimestampQuery() {
        log.info("=== TEST: Multiple Transactions & Timestamp Query ===");

        // --- Create user ---
        User user = userRepository.save(new User("Bob", "bob@example.com", "pass123"));
        log.info("Saved User: id={}, email={}", user.getId(), user.getEmail());

        // --- Create wallet ---
        Wallet wallet = walletRepository.save(new Wallet(user));
        log.info("Saved Wallet for User {}: balance={}", user.getEmail(), wallet.getBalance());

        // --- Create multiple transactions ---
        Transaction t1 = transactionRepository.save(new Transaction(user, 50.0, "CREDIT"));
        log.info("Saved Transaction 1 for User {}: amount={}, type={}, timestamp={}", user.getEmail(), t1.getAmount(), t1.getType(), t1.getTimestamp());

        Transaction t2 = transactionRepository.save(new Transaction(user, 20.0, "DEBIT"));
        log.info("Saved Transaction 2 for User {}: amount={}, type={}, timestamp={}", user.getEmail(), t2.getAmount(), t2.getType(), t2.getTimestamp());

        // --- Fetch transactions between timestamps ---
        LocalDateTime start = t1.getTimestamp().minusMinutes(1);
        LocalDateTime end = t2.getTimestamp().plusMinutes(1);

        List<Transaction> transactionsInRange = transactionRepository.findByUserAndTimestampBetween(user, start, end);
        log.info("Fetched {} transactions for User {} between {} and {}", transactionsInRange.size(), user.getEmail(), start, end);
        assertThat(transactionsInRange).hasSize(2);
    }

    @Test
    void testWalletBalanceUpdate() {
        log.info("=== TEST: Wallet Balance Update ===");

        User user = userRepository.save(new User("Charlie", "charlie@example.com", "pass"));
        log.info("Saved User: id={}, email={}", user.getId(), user.getEmail());

        Wallet wallet = walletRepository.save(new Wallet(user));
        log.info("Saved Wallet for User {}: balance={}", user.getEmail(), wallet.getBalance());

        // --- Update balance ---
        wallet.setBalance(1000.0);
        walletRepository.save(wallet);
        log.info("Updated Wallet balance for User {} to {}", user.getEmail(), wallet.getBalance());

        Wallet updatedWallet = walletRepository.findByUser(user).orElseThrow();
        log.info("Verified updated Wallet balance for User {}: {}", user.getEmail(), updatedWallet.getBalance());
        assertThat(updatedWallet.getBalance()).isEqualTo(1000.0);
    }
}
