package com.example.digitalWalletApp.service;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletProperties walletProperties;
    private final TransactionMapper transactionMapper;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         WalletProperties walletProperties,
                         TransactionMapper transactionMapper,
                         WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletProperties = walletProperties;
        this.transactionMapper = transactionMapper;
        this.walletMapper = walletMapper;
    }

    // --------------------------------------------------------------------
    // GET OR CREATE WALLET
    // --------------------------------------------------------------------
    public Wallet getWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("🪙 Creating wallet for new user {}", user.getEmail());
                    Wallet wallet = new Wallet(user);
                    wallet.setBalance(0.0);
                    wallet.setDailySpent(0.0);
                    wallet.setFrozen(false);
                    wallet.setLastTransactionDate(LocalDate.now());
                    return walletRepository.save(wallet);
                });
    }

    private void validateAmount(double amount, String operation) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");
        if (amount < walletProperties.getMinAmount() || amount > walletProperties.getMaxAmount())
            throw new IllegalArgumentException(operation + " amount must be between "
                    + walletProperties.getMinAmount() + " and " + walletProperties.getMaxAmount());
    }

    private void resetDailyIfNewDay(Wallet wallet) {
        LocalDate today = LocalDate.now();
        if (wallet.getLastTransactionDate() == null || !wallet.getLastTransactionDate().equals(today)) {
            wallet.setDailySpent(0.0);
            wallet.setFrozen(false);
            wallet.setLastTransactionDate(today);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /*
    Simulates artificial delay (used for concurrency testing).
    Helps you observe optimistic locking in action when multiple threads update same wallet.
     */
    // --------------------------------------------------------------------
// LOAD MONEY (with retries + optimistic locking)
// --------------------------------------------------------------------
    public LoadMoneyResponse loadMoney(User user, double amount, String transactionId) {
        String thread = Thread.currentThread().getName();
        logger.info("🚀 [LOAD][{}] Start loadMoney | user={} | txnId={} | amount={}",
                thread, user.getEmail(), transactionId, amount);

        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            logger.warn("⚠️ [LOAD][{}] Duplicate txnId={} for user={} — already processed",
                    thread, transactionId, user.getEmail());
            throw new IllegalArgumentException("Duplicate transaction — already processed.");
        }

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("🔁 [LOAD][{}] Attempt {}/{}", thread, attempt, maxRetries);
                return performLoadMoney(user, amount, transactionId);
            } catch (ObjectOptimisticLockingFailureException e) {
                logger.warn("🔒 [LOAD][{}] Version conflict detected (OptimisticLock) — retrying...",
                        thread);
                if (attempt == maxRetries)
                    throw new RuntimeException("Load failed after retries", e);
                sleep(500); // wait and retry
            }
        }
        throw new RuntimeException("Unexpected loadMoney failure");
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.REPEATABLE_READ,
            rollbackFor = Exception.class
    )
    public LoadMoneyResponse performLoadMoney(User user, double amount, String transactionId) {
        String thread = Thread.currentThread().getName();

        validateAmount(amount, "Load");

        // Fetch wallet with current version
        Wallet wallet = getWallet(user);
        resetDailyIfNewDay(wallet);

        double oldBalance = wallet.getBalance();
        long oldVersion = wallet.getVersion();

        logger.info("👀 [{}] Read wallet → balance={} | version={}", thread, oldBalance, oldVersion);

        double remainingLimit = walletProperties.getDailyLimit() - wallet.getDailySpent();
        if (amount > remainingLimit) {
            throw new IllegalArgumentException("Daily limit exceeded");
        }

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setDailySpent(wallet.getDailySpent() + amount);

        if (wallet.getDailySpent() >= walletProperties.getDailyLimit()) {
            wallet.setFrozen(true);
        }

        // Simulate delay (to allow other threads to overlap to trigger the optimistic locking)
        logger.info("⏳ [{}] Simulating delay (3s)...", thread);
        sleep(3000);

        try {
            // 🔥 Critical: force Hibernate to issue an UPDATE immediately
            walletRepository.saveAndFlush(wallet);
            logger.info("💾 [{}] Update success → newBalance={} | newVersion={} ✅",
                    thread, wallet.getBalance(), wallet.getVersion());
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("💥 [{}] OptimisticLockException → version conflict (oldVersion={})", thread, oldVersion);
            throw e; // bubble up to trigger retry
        }

        // Record transaction only if wallet update succeeded
        Transaction txn = new Transaction(user, amount, "SELF_CREDITED");
        txn.setTransactionId(transactionId);
        transactionRepository.save(txn);

        LoadMoneyResponse response = walletMapper.toLoadMoneyResponse(wallet);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Wallet loaded successfully ✅");

        logger.info("✅ [{}] SUCCESS | txnId={} | finalBalance={} | version={}",
                thread, transactionId, wallet.getBalance(), wallet.getVersion());

        return response;
    }


// TRANSFER MONEY (with retries + optimistic locking)
// --------------------------------------------------------------------
    public TransferResponse transferAmount(User sender, Long recipientId, double amount, String transactionId) {
        String thread = Thread.currentThread().getName();
        logger.info("🚀 [TRANSFER][{}] Start | txnId={} | from={} → to={} | amount={}",
                thread, transactionId, sender.getEmail(), recipientId, amount);

        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException("Duplicate transaction — already processed.");
        }

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("🔁 [TRANSFER][{}] Attempt {}/{}", thread, attempt, maxRetries);
                return performTransfer(sender, recipientId, amount, transactionId);
            } catch (ObjectOptimisticLockingFailureException e) {
                logger.warn("🔒 [TRANSFER][{}] Version conflict detected (OptimisticLock) — retrying...",
                        thread);
                if (attempt == maxRetries)
                    throw new RuntimeException("Transfer failed after retries", e);
                sleep(500);
            }
        }
        throw new RuntimeException("Unexpected transfer failure");
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.REPEATABLE_READ,
            rollbackFor = Exception.class
    )
    public TransferResponse performTransfer(User sender, Long recipientId, double amount, String transactionId) {
        String thread = Thread.currentThread().getName();

        validateAmount(amount, "Transfer");

        Wallet senderWallet = getWallet(sender);
        resetDailyIfNewDay(senderWallet);

        if (senderWallet.getFrozen())
            throw new IllegalArgumentException("Wallet frozen. Cannot transfer.");
        if (senderWallet.getBalance() < amount)
            throw new IllegalArgumentException("Insufficient balance");

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("Recipient not found"));
        Wallet recipientWallet = getWallet(recipient);
        resetDailyIfNewDay(recipientWallet);

        double senderOld = senderWallet.getBalance();
        double receiverOld = recipientWallet.getBalance();

        logger.info("👀 [TRANSFER][{}] Read wallets | senderBal={} (v={}) | recvBal={} (v={})",
                thread, senderOld, senderWallet.getVersion(), receiverOld, recipientWallet.getVersion());

        // --- Update balances ---
        senderWallet.setBalance(senderOld - amount);
        senderWallet.setDailySpent(senderWallet.getDailySpent() + amount);
        if (senderWallet.getDailySpent() >= walletProperties.getDailyLimit())
            senderWallet.setFrozen(true);

        recipientWallet.setBalance(receiverOld + amount);

        logger.info("⏳ [TRANSFER][{}] Simulating delay (3s) — holding before commit...", thread);
        sleep(3000);

        // ✅ Force Hibernate to immediately check optimistic lock version
        walletRepository.saveAndFlush(senderWallet);
        walletRepository.saveAndFlush(recipientWallet);

        logger.info("💾 [TRANSFER][{}] Updated | sender={}→{} | receiver={}→{} | vS={}→{} | vR={}→{} ✅",
                thread,
                senderOld, senderWallet.getBalance(),
                receiverOld, recipientWallet.getBalance(),
                senderWallet.getVersion() - 1, senderWallet.getVersion(),
                recipientWallet.getVersion() - 1, recipientWallet.getVersion());

        // --- Create transactions ---
        Transaction debitTxn = new Transaction(sender, amount, "DEBIT");
        debitTxn.setTransactionId(transactionId);
        transactionRepository.save(debitTxn);

        Transaction creditTxn = new Transaction(recipient, amount, "CREDIT");
        creditTxn.setTransactionId(transactionId + "-CREDIT");
        transactionRepository.save(creditTxn);

        // --- Prepare response ---
        TransferResponse response = walletMapper.toTransferResponse(senderWallet);
        response.setAmountTransferred(amount);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - senderWallet.getDailySpent());
        response.setFrozen(senderWallet.getFrozen());
        response.setMessage("Transfer successful ✅");

        logger.info("✅ [TRANSFER][{}] SUCCESS | txnId={} | Final senderBal={} | receiverBal={}",
                thread, transactionId, senderWallet.getBalance(), recipientWallet.getBalance());

        return response;
    }



    // --------------------------------------------------------------------
    // HELPER / FETCH METHODS
    // --------------------------------------------------------------------
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public Page<TransactionDTO> getTransactions(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByUser(user, pageable);

        return transactionPage.map(transactionMapper::toDTO);
    }


    public LoadMoneyResponse toLoadMoneyResponse(Wallet wallet) {
        LoadMoneyResponse response = walletMapper.toLoadMoneyResponse(wallet);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Balance fetched successfully 🥳");
        return response;
    }

    public WalletProperties getWalletProperties() {
        return walletProperties;
    }
}

/*
| Time | Request 1                 | Request 2                     |
| ---- | ------------------------- | ----------------------------- |
| T0   | enters controller         | still waiting in Tomcat queue |
| T1   | reads wallet (v=5)        | —                             |
| T2   | sleeps (3s)               | starts now                    |
| T3   | updates (new version v=6) | reads wallet (already v=6!)   |
| T4   | Request 1 commits         | Request 2 updates cleanly     |

 */