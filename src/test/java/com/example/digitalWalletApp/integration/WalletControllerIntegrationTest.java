package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.service.WalletService;
import com.example.digitalWalletApp.config.WalletProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
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

    @Mock
    WalletProperties walletProperties;

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
        wallet.setDailySpent(0.0);                    // ensure dailySpent starts at 0
        wallet.setLastTransactionDate(LocalDate.now()); // prevent resetDailyIfNewDay from resetting

        // Mock repository behavior
        lenient().when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(walletProperties.getMinAmount()).thenReturn(1.0);
        lenient().when(walletProperties.getMaxAmount()).thenReturn(10000.0);
        lenient().when(walletProperties.getDailyLimit()).thenReturn(1000.0); // adjust if your service uses it
    }


    @Test
    void loadMoney_withValidAmount_increasesBalance() {
        double loadAmount = 50.0;
        log.info("=== TEST: Load money {} into wallet for user {} ===", loadAmount, user.getEmail());

        Map<String, Object> response = walletService.loadMoney(user, loadAmount);

        assertThat(wallet.getBalance()).isEqualTo(150.0);
        assertThat(response.get("message")).isEqualTo("Wallet loaded successfully");
        verify(transactionRepository).save(any(Transaction.class));

        log.info("[PASS] loadMoney_withValidAmount_increasesBalance ✅\n");
    }

    @Test
    void loadMoney_withNegativeAmount_throwsException() {
        double negativeAmount = -10;
        log.info("=== TEST: Load negative amount {} into wallet for user {} ===", negativeAmount, user.getEmail());

        assertThatThrownBy(() -> walletService.loadMoney(user, negativeAmount))
                .as("Loading negative amount should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        log.info("[PASS] Loading {} failed as expected: amount is negative ✅", negativeAmount);
    }

    @Test
    void loadMoney_withLargeAmount_throwsException() {
        double largeAmount = 1_000_000.0; // above 10000
        log.info("=== TEST: Load large amount {} into wallet for user {} ===", largeAmount, user.getEmail());

        assertThatThrownBy(() -> walletService.loadMoney(user, largeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction amount must be between 1.0 and 10000.0");

        log.info("[PASS] Loading large amount failed as expected ✅\n");
    }


    @Test
    void loadMoney_withZeroAmount_throwsException() {
        double zeroAmount = 0;
        log.info("=== TEST: Load zero amount into wallet for user {} ===", user.getEmail());

        assertThatThrownBy(() -> walletService.loadMoney(user, zeroAmount))
                .as("Loading zero amount should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        log.info("[PASS] Loading zero amount failed as expected ✅");
    }

    @Test
    void getWallet_whenWalletExists_returnsExistingWallet() {
        Wallet result = walletService.getWallet(user);

        assertThat(result.getBalance()).isEqualTo(100.0);
        verify(walletRepository, never()).save(any());

        log.info("[PASS] Existing wallet fetched successfully ✅\n");
    }

    @Test
    void getWallet_whenWalletNotExists_createsAndSavesNewWallet() {
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        Wallet result = walletService.getWallet(user);

        assertThat(result.getBalance()).isZero();
        verify(walletRepository).save(any(Wallet.class));

        log.info("[PASS] New wallet created successfully ✅\n");
    }

    @Test
    void transferAmount_withNegativeAmount_throwsException() {
        double negativeAmount = -50;
        log.info("=== TEST: Transfer negative amount {} should fail ===", negativeAmount);

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, negativeAmount))
                .as("Negative transfer should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        log.info("[PASS] Transfer failed as expected because amount is negative ✅\n");
    }

    @Test
    void transferAmount_withZeroAmount_throwsException() {
        double zeroAmount = 0;
        log.info("=== TEST: Transfer zero amount should fail ===");

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, zeroAmount))
                .as("Zero transfer should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        log.info("[PASS] Transfer failed as expected because amount is zero ✅\n");
    }

    @Test
    void getTransactions_returnsListFromRepository() {
        Transaction t1 = new Transaction();
        t1.setAmount(50.0);
        t1.setType("CREDIT");
        t1.setUser(user);

        when(transactionRepository.findByUser(user)).thenReturn(List.of(t1));

        List<Transaction> txs = walletService.getTransactions(user);

        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getAmount()).isEqualTo(50.0);

        log.info("[PASS] Transactions fetched successfully ✅\n");
    }
}
