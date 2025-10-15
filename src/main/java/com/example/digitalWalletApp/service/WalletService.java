package com.example.digitalWalletApp.service;

import com.example.digitalWalletApp.config.WalletProperties;
import com.example.digitalWalletApp.dto.LoadMoneyResponse;
import com.example.digitalWalletApp.dto.TransactionDTO;
import com.example.digitalWalletApp.dto.TransferResponse;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletProperties walletProperties;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         WalletProperties walletProperties) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletProperties = walletProperties;
    }

    public WalletProperties getWalletProperties() {
        return walletProperties;
    }

    // Get wallet; create new if missing
    public Wallet getWallet(User user) {
        logger.debug("Fetching wallet for user: {}", user.getEmail());
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("No wallet found for user {}. Creating new wallet.", user.getEmail());
                    Wallet newWallet = new Wallet(user);
                    newWallet.setBalance(0.0);
                    Wallet savedWallet = walletRepository.save(newWallet);
                    logger.info("New wallet created for user {} with ID {}", user.getEmail(), savedWallet.getId());
                    return savedWallet;
                });
    }

    // Validate transaction amount
    private void validateAmount(double amount, String operation) {
        if (amount <= 0) {
            logger.warn("Invalid {} amount {} requested", operation, amount);
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount < walletProperties.getMinAmount() || amount > walletProperties.getMaxAmount()) {
            logger.warn("Invalid {} amount {}. Allowed range: {} - {}",
                    operation, amount, walletProperties.getMinAmount(), walletProperties.getMaxAmount());
            throw new IllegalArgumentException(
                    "Transaction amount must be between " + walletProperties.getMinAmount()
                            + " and " + walletProperties.getMaxAmount());
        }
    }

    // Reset daily spent if new day
    private void resetDailyIfNewDay(Wallet wallet) {
        LocalDate today = LocalDate.now();
        if (wallet.getLastTransactionDate() == null || !wallet.getLastTransactionDate().equals(today)) {
            wallet.setDailySpent(0.0);
            wallet.setFrozen(false);
            wallet.setLastTransactionDate(today);
            logger.info("Daily reset for wallet of user {}. dailySpent=0, frozen=false", wallet.getUser().getEmail());
        }
    }

    // Load money into wallet (SELF_CREDITED)
    @Transactional
    public LoadMoneyResponse loadMoney(User user, double amount) {
        logger.info("Wallet load request initiated for user {}: amount={}", user.getEmail(), amount);

        validateAmount(amount, "load");

        Wallet wallet = getWallet(user);
        resetDailyIfNewDay(wallet);

        double remainingLimit = walletProperties.getDailyLimit() - wallet.getDailySpent();
        if (amount > remainingLimit) {
            logger.warn("Load failed: Requested amount ₹{} exceeds remaining daily limit ₹{} for user {}",
                    amount, remainingLimit, user.getEmail());
            throw new IllegalArgumentException("Load failed: You can only load up to ₹" + remainingLimit + " today");
        }

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setDailySpent(wallet.getDailySpent() + amount);

        if (wallet.getDailySpent() >= walletProperties.getDailyLimit()) {
            wallet.setFrozen(true);
            logger.info("User {} reached daily limit ₹{}. Wallet frozen until next day.", user.getEmail(), walletProperties.getDailyLimit());
        }

        walletRepository.save(wallet);

        Transaction transaction = new Transaction(user, amount, "SELF_CREDITED");
        transactionRepository.save(transaction);

        logger.info("Wallet loaded successfully for user {}: newBalance={}", user.getEmail(), wallet.getBalance());

        LoadMoneyResponse response = new LoadMoneyResponse();
        response.setBalance(wallet.getBalance());
        response.setDailySpent(wallet.getDailySpent());
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Wallet loaded successfully");
        return response;
    }

    // Transfer amount to another user
    @Transactional
    public TransferResponse transferAmount(User sender, Long recipientId, double amount) {
        logger.info("Transfer request initiated: sender={}, recipientId={}, amount={}", sender.getEmail(), recipientId, amount);

        validateAmount(amount, "transfer");

        Wallet senderWallet = getWallet(sender);
        resetDailyIfNewDay(senderWallet);

        double remainingLimit = walletProperties.getDailyLimit() - senderWallet.getDailySpent();
        if (amount > remainingLimit) {
            logger.warn("Transfer failed: Requested amount ₹{} exceeds remaining daily limit ₹{} for user {}",
                    amount, remainingLimit, sender.getEmail());
            throw new IllegalArgumentException("Transfer failed: You can only transfer up to ₹" + remainingLimit + " today");
        }

        if (senderWallet.getFrozen()) {
            logger.warn("Sender {} wallet frozen due to daily limit. Cannot send money.", sender.getEmail());
            throw new IllegalArgumentException("Wallet frozen due to daily limit reached. Cannot transfer.");
        }

        if (senderWallet.getBalance() < amount) {
            logger.warn("Insufficient balance for sender {}. Balance={}, requested={}", sender.getEmail(), senderWallet.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient balance");
        }

        Optional<User> recipientOpt = userRepository.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            logger.error("Recipient with ID {} not found", recipientId);
            throw new IllegalArgumentException("Recipient not found");
        }
        User recipient = recipientOpt.get();
        Wallet recipientWallet = getWallet(recipient);
        resetDailyIfNewDay(recipientWallet);

        // Update balances
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        senderWallet.setDailySpent(senderWallet.getDailySpent() + amount);
        if (senderWallet.getDailySpent() >= walletProperties.getDailyLimit()) {
            senderWallet.setFrozen(true);
            logger.info("Sender {} reached daily limit ₹{}. Wallet frozen until next day.", sender.getEmail(), walletProperties.getDailyLimit());
        }

        recipientWallet.setBalance(recipientWallet.getBalance() + amount);
        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        // Save transactions
        Transaction debit = new Transaction(sender, amount, "DEBIT");
        Transaction credit = new Transaction(recipient, amount, "CREDIT");
        transactionRepository.save(debit);
        transactionRepository.save(credit);

        logger.info("Transfer successful: sender {} newBalance={}, recipient {} newBalance={}", sender.getEmail(), senderWallet.getBalance(), recipient.getEmail(), recipientWallet.getBalance());

        TransferResponse response = new TransferResponse();
        response.setAmountTransferred(amount);
        response.setSenderBalance(senderWallet.getBalance());
        response.setRecipientBalance(recipientWallet.getBalance());
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - senderWallet.getDailySpent());
        response.setFrozen(senderWallet.getFrozen());
        response.setMessage("Transfer successful");
        return response;
    }

    // Fetch all transactions as DTOs
    public List<TransactionDTO> getTransactions(User user) {
        logger.info("Fetching transactions for user {}", user.getEmail());
        List<Transaction> transactions = transactionRepository.findByUser(user);
        List<TransactionDTO> dtos = transactions.stream()
                .map(t -> {
                    TransactionDTO dto = new TransactionDTO();
                    dto.setId(t.getId());
                    dto.setAmount(t.getAmount());
                    dto.setType(t.getType());
                    dto.setTimestamp(t.getTimestamp());
                    dto.setUserEmail(t.getUser().getEmail()); // optional
                    return dto;
                })
                .collect(Collectors.toList());
        logger.info("Fetched {} transactions for user {}", dtos.size(), user.getEmail());
        return dtos;
    }
}
