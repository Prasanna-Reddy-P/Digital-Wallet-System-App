package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(WalletControllerIntegrationTest.class);

    @Mock
    WalletRepository walletRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setName("Alice");

        wallet = new Wallet(user);
        wallet.setBalance(100.0);
    }

    @Test
    void loadMoney_withValidAmount_increasesBalance() {
        double loadAmount = 50.0;
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        log.info("=== TEST: Load money {} into wallet for user {} ===", loadAmount, user.getEmail());
        log.info("Initial wallet balance: {}", wallet.getBalance());

        Map<String, Object> response = walletService.loadMoney(user, loadAmount);

        log.info("Attempted to add: {}", loadAmount);
        log.info("Wallet balance after load: {}", wallet.getBalance());
        log.info("Transaction response: {}", response.get("message"));

        assertThat(wallet.getBalance()).isEqualTo(150.0);
        assertThat(response.get("message")).isEqualTo("Wallet loaded successfully");
        verify(transactionRepository).save(any(Transaction.class));

        log.info("[PASS] loadMoney_withValidAmount_increasesBalance ✅\n");
    }

    @Test
    void loadMoney_withNegativeAmount_throwsException() {
        double negativeAmount = -10;
        log.info("=== TEST: Load negative amount {} into wallet for user {} ===", negativeAmount, user.getEmail());
        log.info("Initial wallet balance: {}", wallet.getBalance());

        assertThatThrownBy(() -> walletService.loadMoney(user, negativeAmount))
                .as("Loading negative amount should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        log.info("[PASS] Loading {} failed as expected: amount is negative ✅", negativeAmount);
    }

    @Test
    void loadMoney_withLargeAmount_addsSuccessfully() {
        double largeAmount = 1_000_000_000.0; // 1 billion
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        log.info("=== TEST: Load large amount {} into wallet for user {} ===", largeAmount, user.getEmail());
        log.info("Initial wallet balance: {}", wallet.getBalance());

        Map<String, Object> response = walletService.loadMoney(user, largeAmount);

        log.info("Attempted to add: {}", largeAmount);
        log.info("Wallet balance after load: {}", wallet.getBalance());
        log.info("Transaction response: {}", response.get("message"));

        assertThat(wallet.getBalance()).isEqualTo(100.0 + largeAmount);
        assertThat(response.get("message")).isEqualTo("Wallet loaded successfully");
        verify(transactionRepository).save(any(Transaction.class));

        log.info("[PASS] loadMoney_withLargeAmount_addsSuccessfully ✅\n");
    }


    @Test
    void loadMoney_withZeroAmount_throwsException() {
        double zeroAmount = 0;
        log.info("=== TEST: Load zero amount into wallet for user {} ===", user.getEmail());
        log.info("Initial wallet balance: {}", wallet.getBalance());

        assertThatThrownBy(() -> walletService.loadMoney(user, zeroAmount))
                .as("Loading zero amount should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        log.info("[PASS] Loading zero amount failed as expected ✅");
    }

    @Test
    void getWallet_whenWalletExists_returnsExistingWallet() {
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        log.info("=== TEST: Fetch existing wallet for user {} ===", user.getEmail());
        log.info("Expected balance: {}", wallet.getBalance());

        Wallet result = walletService.getWallet(user);

        log.info("Fetched wallet balance: {}", result.getBalance());
        assertThat(result.getBalance()).isEqualTo(100.0);
        verify(walletRepository, never()).save(any());

        log.info("[PASS] Existing wallet fetched successfully ✅\n");
    }

    @Test
    void getWallet_whenWalletNotExists_createsAndSavesNewWallet() {
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        log.info("=== TEST: Wallet does not exist, create new wallet for user {} ===", user.getEmail());
        log.info("Initial balance should be 0.0");

        Wallet result = walletService.getWallet(user);

        log.info("New wallet balance: {}", result.getBalance());
        assertThat(result.getBalance()).isZero();
        verify(walletRepository).save(any(Wallet.class));

        log.info("[PASS] New wallet created successfully ✅\n");
    }

    @Test
    void transferAmount_withNegativeAmount_throwsException() {
        double negativeAmount = -50;
        log.info("=== TEST: Transfer negative amount {} should fail ===", negativeAmount);
        log.info("Wallet balance before transfer: {}", wallet.getBalance());

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, negativeAmount))
                .as("Negative transfer should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        log.info("[PASS] Transfer failed as expected because amount is negative ✅\n");
    }

    @Test
    void transferAmount_withZeroAmount_throwsException() {
        double zeroAmount = 0;
        log.info("=== TEST: Transfer zero amount should fail ===");
        log.info("Wallet balance before transfer: {}", wallet.getBalance());

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, zeroAmount))
                .as("Zero transfer should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        log.info("[PASS] Transfer failed as expected because amount is zero ✅\n");
    }

    @Test
    void getTransactions_returnsListFromRepository() {
        Transaction t1 = new Transaction();
        t1.setAmount(50.0);
        t1.setType("CREDIT");
        t1.setUser(user);

        when(transactionRepository.findByUser(user)).thenReturn(List.of(t1));

        log.info("=== TEST: Fetch transactions for user {} ===", user.getEmail());

        List<Transaction> txs = walletService.getTransactions(user);

        log.info("Number of transactions fetched: {}", txs.size());
        txs.forEach(tx -> log.info("Transaction - Type: {}, Amount: {}", tx.getType(), tx.getAmount()));

        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getAmount()).isEqualTo(50.0);

        log.info("[PASS] Transactions fetched successfully ✅\n");
    }
}
