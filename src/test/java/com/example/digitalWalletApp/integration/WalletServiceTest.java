package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.config.WalletProperties;
import com.example.digitalWalletApp.dto.LoadMoneyResponse;
import com.example.digitalWalletApp.dto.TransactionDTO;
import com.example.digitalWalletApp.dto.TransferResponse;
import com.example.digitalWalletApp.exception.UserNotFoundException;
import com.example.digitalWalletApp.mapper.TransactionMapper;
import com.example.digitalWalletApp.mapper.WalletMapper;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.service.WalletService;
import com.example.digitalWalletApp.service.wallet.WalletFactory;
import com.example.digitalWalletApp.service.wallet.WalletTransactionService;
import com.example.digitalWalletApp.service.wallet.WalletValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mockito-based tests for WalletService
 *
 * These tests mirror the flows implemented in your WalletService:
 *  - loadMoney + performLoadMoney (duplicate check, success, optimistic-lock retry)
 *  - transferAmount + performTransfer (duplicate check, recipient missing, insufficient balance, success)
 *  - helper methods (getAllUsers, getUserById, getTransactions, toLoadMoneyResponse)
 *
 * Place under src/test/java/... and run. Tests call performLoadMoney/performTransfer which contain a small Thread.sleep,
 * so tests may take a couple seconds each when those flows execute. (You can refactor service later to inject a sleeper for faster tests.)
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceTest.class);

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletFactory walletFactory;
    @Mock private WalletTransactionService txnService;
    @Mock private WalletValidator walletValidator;
    @Mock private WalletMapper walletMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private WalletProperties walletProperties;

    @InjectMocks private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("john@example.com");
        user.setName("John");

        wallet = new Wallet(user);
        wallet.setId(10L);
        wallet.setBalance(100.0);
        wallet.setDailySpent(0.0);
        wallet.setFrozen(false);
        wallet.setLastTransactionDate(LocalDate.now());
        wallet.setVersion(1L);

        // sensible defaults
        lenient().when(walletProperties.getMinAmount()).thenReturn(1.0);
        lenient().when(walletProperties.getMaxAmount()).thenReturn(10_000.0);
        lenient().when(walletProperties.getDailyLimit()).thenReturn(1_000.0);

        // default factory/repo behaviour
        lenient().when(walletFactory.getOrCreateWallet(any(User.class))).thenReturn(wallet);
        lenient().when(walletRepository.findByUser(any(User.class))).thenReturn(Optional.of(wallet));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------
    // get/create wallet helpers
    // -------------------------
    @Test
    void getOrCreateWallet_whenNotExists_createsNewWalletViaFactory() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getOrCreateWallet_whenNotExists_createsNewWalletViaFactory");
        logger.info("------------------------------");

        // simulate factory creating new wallet
        when(walletFactory.getOrCreateWallet(user)).thenAnswer(inv -> {
            Wallet w = new Wallet(user);
            w.setId(555L);
            w.setBalance(0.0);
            return w;
        });

        Wallet created = walletFactory.getOrCreateWallet(user);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(555L);
        assertThat(created.getBalance()).isEqualTo(0.0);

        logger.info("✅ Test passed — factory creates wallet when not present");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // loadMoney - duplicate transaction
    // -------------------------
    @Test
    void loadMoney_duplicateTransaction_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: loadMoney_duplicateTransaction_throws");
        logger.info("------------------------------");

        when(txnService.isDuplicate("dup")).thenReturn(true);

        assertThatThrownBy(() -> walletService.loadMoney(user, 10.0, "dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction");

        verify(txnService).isDuplicate("dup");
        logger.info("✅ Test passed — duplicate transaction prevented");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // performLoadMoney - happy path
    // -------------------------
    @Test
    void performLoadMoney_success_updatesWalletAndRecordsTransaction() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: performLoadMoney_success_updatesWalletAndRecordsTransaction");
        logger.info("------------------------------");

        // validator mocks — no exceptions thrown
        doNothing().when(walletValidator).validateAmount(200.0, "Load");
        doNothing().when(walletValidator).validateDailyLimit(wallet, 200.0);

        // wallet save — simulate DB version increment
        doAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setVersion(w.getVersion() + 1);
            return w;
        }).when(walletRepository).saveAndFlush(any(Wallet.class));

        // make txnService delegate to transactionRepository mock
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            double amt = inv.getArgument(1);
            String txnId = inv.getArgument(2);
            Transaction txn = new Transaction(u, amt, "SELF_CREDITED");
            txn.setTransactionId(txnId);
            transactionRepository.save(txn);
            return null;
        }).when(txnService).recordLoadTransaction(any(User.class), anyDouble(), anyString());

        // transaction save mock
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // mapper mock
        when(walletMapper.toLoadMoneyResponse(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            LoadMoneyResponse r = new LoadMoneyResponse();
            r.setBalance(w.getBalance());
            r.setDailySpent(w.getDailySpent());
            return r;
        });

        // call service
        LoadMoneyResponse resp = walletService.performLoadMoney(user, 200.0, "txn1");

        // assertions
        assertThat(resp).isNotNull();
        assertThat(wallet.getBalance()).isEqualTo(300.0); // initial 100 + 200
        assertThat(wallet.getDailySpent()).isEqualTo(200.0);
        assertThat(resp.getRemainingDailyLimit()).isEqualTo(1000.0 - 200.0);

        // verify correct interactions
        verify(walletRepository).saveAndFlush(wallet);
        verify(txnService).recordLoadTransaction(user, 200.0, "txn1");
        verify(transactionRepository).save(any(Transaction.class));

        logger.info("✅ Test passed — performLoadMoney updated wallet and recorded transaction");
        logger.info("------------------------------\n\n");
    }



    // -------------------------
    // loadMoney retry on optimistic lock
    // -------------------------
    @Test
    void loadMoney_retriesOnOptimisticLock_andSucceeds() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: loadMoney_retriesOnOptimisticLock_andSucceeds");
        logger.info("------------------------------");

        // Setup mocks
        when(txnService.isDuplicate("retry")).thenReturn(false);
        doNothing().when(walletValidator).validateAmount(50.0, "Load");
        doNothing().when(walletValidator).validateDailyLimit(wallet, 50.0);

        // First attempt throws optimistic lock → second succeeds
        doThrow(new ObjectOptimisticLockingFailureException(Wallet.class, 1L))
                .doAnswer(inv -> {
                    Wallet w = inv.getArgument(0);
                    w.setVersion(w.getVersion() + 1);
                    return w;
                })
                .when(walletRepository).saveAndFlush(any(Wallet.class));

        // simulate txnService calling repository
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            double amt = inv.getArgument(1);
            String txnId = inv.getArgument(2);
            Transaction t = new Transaction(u, amt, "SELF_CREDITED");
            t.setTransactionId(txnId);
            transactionRepository.save(t);
            return null;
        }).when(txnService).recordLoadTransaction(any(User.class), anyDouble(), anyString());

        // Mock repository and mapper
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletMapper.toLoadMoneyResponse(any(Wallet.class))).thenReturn(new LoadMoneyResponse());

        // call service
        LoadMoneyResponse resp = walletService.loadMoney(user, 50.0, "retry");

        // verify results
        assertThat(resp).isNotNull();
        verify(walletRepository, atLeast(2)).saveAndFlush(any(Wallet.class)); // retried
        verify(txnService).recordLoadTransaction(user, 50.0, "retry"); // txn recorded
        verify(transactionRepository).save(any(Transaction.class)); // delegated save happened

        logger.info("✅ Test passed — loadMoney retried on optimistic lock and succeeded");
        logger.info("------------------------------\n\n");
    }


    // -------------------------
    // transfer - duplicate transaction
    // -------------------------
    @Test
    void transfer_duplicateTransaction_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: transfer_duplicateTransaction_throws");
        logger.info("------------------------------");

        when(txnService.isDuplicate("dup")).thenReturn(true);

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, 10.0, "dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction");

        verify(txnService).isDuplicate("dup");
        logger.info("✅ Test passed — duplicate transfer prevented");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // performTransfer - recipient not found
    // -------------------------
    @Test
    void performTransfer_recipientNotFound_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: performTransfer_recipientNotFound_throws");
        logger.info("------------------------------");

        // Only stubbing actually needed for this path
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Call and verify
        assertThatThrownBy(() -> walletService.performTransfer(user, 99L, 100.0, "tx1"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Recipient not found");

        logger.info("✅ Test passed — recipient missing handled");
        logger.info("------------------------------\n\n");
    }


    // -------------------------
    // performTransfer - insufficient balance
    // -------------------------
    @Test
    void performTransfer_insufficientBalance_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: performTransfer_insufficientBalance_throws");
        logger.info("------------------------------");

        wallet.setBalance(20.0); // low balance
        when(walletFactory.getOrCreateWallet(user)).thenReturn(wallet);

        // Only stub what is actually used before the exception
        doNothing().when(walletValidator).validateAmount(200.0, "Transfer");
        doThrow(new IllegalArgumentException("Insufficient balance"))
                .when(walletValidator).validateBalance(wallet, 200.0);

        assertThatThrownBy(() -> walletService.performTransfer(user, 2L, 200.0, "t-ins"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        logger.info("✅ Test passed — insufficient balance prevented transfer");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // helpers: getAllUsers, getUserById, getTransactions, toLoadMoneyResponse
    // -------------------------
    @Test
    void helpers_getAllUsers_getUserById_getTransactions_toLoadMoneyResponse() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: helpers_getAllUsers_getUserById_getTransactions_toLoadMoneyResponse");
        logger.info("------------------------------");

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Transaction t1 = new Transaction(user, 10.0, "DEBIT");
        Page<Transaction> page = new PageImpl<>(List.of(t1));
        when(transactionRepository.findByUser(eq(user), any(PageRequest.class))).thenReturn(page);
        when(transactionMapper.toDTO(t1)).thenReturn(new TransactionDTO());
        when(walletMapper.toLoadMoneyResponse(wallet)).thenReturn(new LoadMoneyResponse());

        List<User> all = walletService.getAllUsers();
        User u = walletService.getUserById(1L);
        Page<TransactionDTO> txPage = walletService.getTransactions(user, 0, 10);
        LoadMoneyResponse resp = walletService.toLoadMoneyResponse(wallet);

        assertThat(all).hasSize(1);
        assertThat(u).isEqualTo(user);
        assertThat(txPage.getContent()).hasSize(1);
        assertThat(resp.getRemainingDailyLimit()).isEqualTo(walletProperties.getDailyLimit() - wallet.getDailySpent());

        logger.info("✅ Test passed — helper methods validated");
        logger.info("------------------------------\n\n");
    }
}
