package com.example.digitalWalletApp.integration;
import com.example.digitalWalletApp.service.WalletService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/*
@ExtendWith(MockitoExtension.class)
What it does (plain):
Tells JUnit 5 to run this test class with the Mockito extension.
The extension initializes Mockito annotations (@Mock, @InjectMocks, etc.) before each test runs.

Why you need it:
Without it, your @Mock fields would be null — Mockito wouldn’t create the mock objects automatically.
 */
class WalletServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceTest.class);

    @Mock private WalletRepository walletRepository;
    /*
    👉 It tells Mockito to create a fake (mocked) version of WalletRepository.
    This is not the real repository — no DB connection, no JPA, nothing.
    It’s just an object that records calls and returns what you tell it to.

    Once you have a mock, you control what happens when it’s called.
    when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
    means:
“When someone calls walletRepository.findByUser(user) on this mock object,
don’t actually look in the DB — just immediately return Optional.of(wallet).”

🧠 Why We Do This

Because in a unit test, we want to test only the logic inside your service —
not the behavior of database, network, or other layers.

By mocking dependencies:

We control the input/output of each dependency.
We make tests predictable and fast.
We avoid real database or API calls.
     */
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletProperties walletProperties;
    @Mock private TransactionMapper transactionMapper;
    @Mock private WalletMapper walletMapper;

    @InjectMocks private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("john@example.com");
        user.setPassword("pwd");

        wallet = new Wallet(user);
        wallet.setId(10L);
        wallet.setBalance(100.0);
        wallet.setDailySpent(0.0);
        wallet.setFrozen(false);
        wallet.setLastTransactionDate(LocalDate.now());
        wallet.setVersion(1L);

        // sensible defaults for amounts/limits
        lenient().when(walletProperties.getMinAmount()).thenReturn(1.0);
        lenient().when(walletProperties.getMaxAmount()).thenReturn(10_000.0);
        lenient().when(walletProperties.getDailyLimit()).thenReturn(1_000.0);

        /*
        lenient(): This is a static method in the org.mockito.Mockito class.
        It is a wrapper for a stubbing that indicates to Mockito that this specific stub is allowed to be unused in a test
        without causing a failure

            Make this stub lenient because not all tests may call this method,
         */

        // default repository behavior
        lenient().when(walletRepository.findByUser(any(User.class))).thenReturn(Optional.of(wallet));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        /*
        lenient(): Again, this marks the stubbing as lenient to prevent UnnecessaryStubbingException errors.

        when(walletRepository.save(any(Wallet.class))): This sets up a stub for the save method,
        which is often used to save a new or updated entity in a repository.

        when(walletRepository.save(any(Wallet.class))): This is where your code snippet comes in. It sets up a "stub" so that whenever the save method on the walletRepository is called then
        the behaviour is written in .thenReturn()


        .thenAnswer(inv -> inv.getArgument(0)): This defines the behavior of the save method.
        thenAnswer(): This is used when the return value needs to be dynamically computed based on the arguments passed to the method.
        inv -> inv.getArgument(0): This is a Java 8 lambda expression for the Answer interface.
        It retrieves the first argument passed to the save method (inv.getArgument(0)) and returns it.
        For a repository's savee method, this is a very common stubbing pattern because the method is expected to return the same entity that was passed in.
         */

        // saveAndFlush will be configured per-test when needed
    }

    // -------------------------
    // getWallet
    // -------------------------
    @Test
    void getWallet_whenExists_returnsExisting() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getWallet_whenExists_returnsExisting");
        logger.info("------------------------------");

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        Wallet found = walletService.getWallet(user);

        assertThat(found).isSameAs(wallet);
        verify(walletRepository, never()).save(any());
        /*
        This checks that the save() method of the mock repository was never called.
        If the service tried to save a new wallet (which would be wrong in this test case),
        the test would fail.

        So this confirms:
        ✅ The wallet was found in the mock DB.
        ✅ No new wallet was created or saved.
         */
        logger.info("✅ Test passed — returned existing wallet");
        logger.info("------------------------------\n\n");
    }

    @Test
    void getWallet_whenNotExists_createsAndSaves() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getWallet_whenNotExists_createsAndSaves");
        logger.info("------------------------------");

        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());
        // capture saved wallet
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        /*
        An ArgumentCaptor is used to capture the exact argument passed into a mocked method,
        so that you can inspect it later.

        You’re saying:
        “Hey Mockito, when walletRepository.save() is called, remember what wallet object they tried to save.”
         */
        when(walletRepository.save(captor.capture())).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(555L);
            return w;
        });

        Wallet created = walletService.getWallet(user);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(555L);
        assertThat(created.getBalance()).isEqualTo(0.0);
        verify(walletRepository).save(any(Wallet.class));
        logger.info("✅ Test passed — created wallet for new user");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // loadMoney - duplicate txn
    // -------------------------
    @Test
    void loadMoney_duplicateTransaction_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: loadMoney_duplicateTransaction_throws");
        logger.info("------------------------------");

        when(transactionRepository.findByTransactionId("tx-dup")).thenReturn(Optional.of(new Transaction()));

        assertThatThrownBy(() -> walletService.loadMoney(user, 10.0, "tx-dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction");

        verify(transactionRepository).findByTransactionId("tx-dup");
        logger.info("✅ Test passed — duplicate transaction prevented");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // performLoadMoney - success
    // Note: this calls the real performLoadMoney which contains a 3s sleep.
    // Expect test to take a few seconds unless you refactor the service to inject a sleeper.
    // -------------------------
    @Test
    void performLoadMoney_success_updatesWalletAndSavesTransaction() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: performLoadMoney_success_updatesWalletAndSavesTransaction");
        logger.info("------------------------------");

        String txnId = "txn-success";
        lenient().when(transactionRepository.findByTransactionId(txnId)).thenReturn(Optional.empty());
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // simulate saveAndFlush increments version and returns wallet
        doAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setVersion(w.getVersion() + 1);
            return w;
        }).when(walletRepository).saveAndFlush(any(Wallet.class));

        // map to LoadMoneyResponse
        LoadMoneyResponse mapped = new LoadMoneyResponse();
        mapped.setBalance(110.0); // expected balance
        when(walletMapper.toLoadMoneyResponse(any(Wallet.class))).thenReturn(mapped);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // call performLoadMoney directly (this will call Thread.sleep(3000) inside your service)
        LoadMoneyResponse resp = walletService.performLoadMoney(user, 10.0, txnId);

        assertThat(resp).isNotNull();
        assertThat(wallet.getBalance()).isEqualTo(110.0);
        assertThat(wallet.getDailySpent()).isEqualTo(10.0);
        assertThat(resp.getRemainingDailyLimit()).isEqualTo(1000.0 - 10.0);
        verify(walletRepository).saveAndFlush(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));

        logger.info("✅ Test passed — performLoadMoney success flow validated");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // loadMoney retry on optimistic lock: first saveAndFlush throws, second succeeds
    // -------------------------
    @Test
    void loadMoney_retriesOnOptimisticLock_andSucceeds() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: loadMoney_retriesOnOptimisticLock_andSucceeds");
        logger.info("------------------------------");

        String txnId = "txn-retry";
        lenient().when(transactionRepository.findByTransactionId(txnId)).thenReturn(Optional.empty());
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // First call to saveAndFlush throws optimistic lock, second call succeeds
        doThrow(new ObjectOptimisticLockingFailureException(Wallet.class, 1L))
                .doAnswer(inv -> {
                    Wallet w = inv.getArgument(0);
                    w.setVersion(w.getVersion() + 1);
                    return w;
                })
                .when(walletRepository).saveAndFlush(any(Wallet.class));

        // map response
        when(walletMapper.toLoadMoneyResponse(any(Wallet.class))).thenReturn(new LoadMoneyResponse());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // call loadMoney (it will retry once and then succeed)
        LoadMoneyResponse resp = walletService.loadMoney(user, 5.0, txnId);

        assertThat(resp).isNotNull();
        verify(walletRepository, atLeast(2)).saveAndFlush(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));

        logger.info("✅ Test passed — loadMoney retried on optimistic lock and succeeded");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // transfer - duplicate txn
    // -------------------------
    @Test
    void transferAmount_duplicateTransaction_throws() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: transferAmount_duplicateTransaction_throws");
        logger.info("------------------------------");

        when(transactionRepository.findByTransactionId("dup")).thenReturn(Optional.of(new Transaction()));

        assertThatThrownBy(() -> walletService.transferAmount(user, 2L, 10.0, "dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction");

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

        String txnId = "t-rcp";
        lenient().when(transactionRepository.findByTransactionId(txnId)).thenReturn(Optional.empty());
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.performTransfer(user, 999L, 10.0, txnId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Recipient not found");

        logger.info("✅ Test passed — missing recipient handled");
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

        String txnId = "t-ins";

        // 💡 Don't stub unused repository calls
        wallet.setBalance(20.0);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // simulate recipient exists
        User recipient = new User();
        recipient.setId(2L);
        recipient.setName("Jane");
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));


        // assert insufficient balance exception
        assertThatThrownBy(() -> walletService.performTransfer(user, 2L, 200.0, txnId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        logger.info("✅ Test passed — insufficient balance prevented transfer");
        logger.info("------------------------------\n\n");
    }


    // -------------------------
    // performTransfer - success
    // -------------------------
    @Test
    void performTransfer_success_updatesBothWalletsAndCreatesTransactions() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: performTransfer_success_updatesBothWalletsAndCreatesTransactions");
        logger.info("------------------------------");

        String txnId = "txn-transfer";
        lenient().when(transactionRepository.findByTransactionId(txnId))
                .thenReturn(Optional.empty());


        // sender wallet (already set)
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // recipient
        User recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("bob@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Wallet recipientWallet = new Wallet(recipient);
        recipientWallet.setBalance(50.0);
        recipientWallet.setVersion(1L);
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));

        // simulate saveAndFlush increments version
        doAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setVersion(w.getVersion() + 1);
            return w;
        }).when(walletRepository).saveAndFlush(any(Wallet.class));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletMapper.toTransferResponse(any(Wallet.class))).thenReturn(new TransferResponse());

        TransferResponse resp = walletService.performTransfer(user, 2L, 25.0, txnId);

        // sender decreased, recipient increased
        assertThat(wallet.getBalance()).isEqualTo(75.0);
        assertThat(recipientWallet.getBalance()).isEqualTo(75.0);

        verify(walletRepository, times(2)).saveAndFlush(any(Wallet.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        assertThat(resp).isNotNull();
        assertThat(resp.getAmountTransferred()).isEqualTo(25.0);

        logger.info("✅ Test passed — transfer success validated");
        logger.info("------------------------------\n\n");
    }

    // -------------------------
    // helper methods: getAllUsers, getUserById, getTransactions, toLoadMoneyResponse
    // -------------------------
    @Test
    void helpers_getAllUsers_getUserById_getTransactions_toLoadMoneyResponse() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: helpers_getAllUsers_getUserById_getTransactions_toLoadMoneyResponse");
        logger.info("------------------------------");

        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Transaction t1 = new Transaction(user, 10.0, "DEBIT");
        when(transactionRepository.findByUser(user)).thenReturn(List.of(t1));
        TransactionDTO dto = new TransactionDTO();
        when(transactionMapper.toDTO(t1)).thenReturn(dto);

        List<User> all = walletService.getAllUsers();
        User u = walletService.getUserById(user.getId());
        List<TransactionDTO> txs = walletService.getTransactions(user);

        when(walletMapper.toLoadMoneyResponse(wallet)).thenReturn(new LoadMoneyResponse());
        wallet.setDailySpent(20.0);
        var resp = walletService.toLoadMoneyResponse(wallet);

        assertThat(all).hasSize(1);
        assertThat(u).isEqualTo(user);
        assertThat(txs).hasSize(1);
        assertThat(resp.getRemainingDailyLimit()).isEqualTo(walletProperties.getDailyLimit() - wallet.getDailySpent());

        logger.info("✅ Test passed — helper methods validated");
        logger.info("------------------------------\n\n");
    }
}
