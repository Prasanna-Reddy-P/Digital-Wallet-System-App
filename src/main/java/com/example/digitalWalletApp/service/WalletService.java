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
import org.springframework.stereotype.Service;
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

    public Wallet getWallet(User user) {
        logger.debug("Fetching wallet for user {}", user.getEmail());
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    logger.info("No wallet found for user {}. Creating new wallet.", user.getEmail());
                    Wallet newWallet = new Wallet(user);
                    newWallet.setBalance(0.0);
                    newWallet.setDailySpent(0.0);
                    newWallet.setFrozen(false);
                    newWallet.setLastTransactionDate(LocalDate.now());
                    Wallet saved = walletRepository.save(newWallet);
                    logger.info("Created wallet ID {} for user {}", saved.getId(), user.getEmail());
                    return saved;
                });
    }

    private void validateAmount(double amount, String operation) {
        if (amount <= 0) {
            logger.warn("Invalid {} amount {} requested", operation, amount);
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount < walletProperties.getMinAmount() || amount > walletProperties.getMaxAmount()) {
            logger.warn("{} amount {} is outside allowed range {}-{}", operation, amount,
                    walletProperties.getMinAmount(), walletProperties.getMaxAmount());
            throw new IllegalArgumentException(operation + " amount must be between "
                    + walletProperties.getMinAmount() + " and " + walletProperties.getMaxAmount());
        }
        logger.debug("{} amount {} validated successfully", operation, amount);
    }

    private void resetDailyIfNewDay(Wallet wallet) {
        LocalDate today = LocalDate.now();
        if (wallet.getLastTransactionDate() == null || !wallet.getLastTransactionDate().equals(today)) {
            logger.info("Resetting daily spent and unfreezing wallet for user {}", wallet.getUser().getEmail());
            wallet.setDailySpent(0.0);
            wallet.setFrozen(false);
            wallet.setLastTransactionDate(today);
        }
    }

    // ---------------- Load Money ----------------
    @Transactional
    public LoadMoneyResponse loadMoney(User user, double amount) {
        logger.info("Initiating wallet load for user {}: amount={}", user.getEmail(), amount);

        validateAmount(amount, "Load");

        Wallet wallet = getWallet(user);
        resetDailyIfNewDay(wallet);

        double remainingLimit = walletProperties.getDailyLimit() - wallet.getDailySpent();
        if (amount > remainingLimit) {
            logger.warn("Load failed: Requested amount {} exceeds daily limit {}", amount, remainingLimit);
            throw new IllegalArgumentException("Load failed: You can only load up to ₹" + remainingLimit + " today");
        }

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setDailySpent(wallet.getDailySpent() + amount);

        if (wallet.getDailySpent() >= walletProperties.getDailyLimit()) {
            wallet.setFrozen(true);
            logger.info("User {} reached daily limit {}. Wallet frozen until next day.", user.getEmail(), walletProperties.getDailyLimit());
        }

        walletRepository.save(wallet);
        transactionRepository.save(new Transaction(user, amount, "SELF_CREDITED"));

        logger.info("Wallet loaded successfully for user {}. New balance: {}", user.getEmail(), wallet.getBalance());

        LoadMoneyResponse response = walletMapper.toLoadMoneyResponse(wallet);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        response.setMessage("Wallet loaded successfully");
        return response;
    }

    // ---------------- Transfer Money ----------------
    @Transactional
    public TransferResponse transferAmount(User sender, Long recipientId, double amount) {
        logger.info("Initiating transfer from {} to recipientId {} amount {}", sender.getEmail(), recipientId, amount);

        validateAmount(amount, "Transfer");

        Wallet senderWallet = getWallet(sender);
        resetDailyIfNewDay(senderWallet);

        double remainingLimit = walletProperties.getDailyLimit() - senderWallet.getDailySpent();
        if (amount > remainingLimit) {
            logger.warn("Transfer failed: Requested amount {} exceeds remaining daily limit {}", amount, remainingLimit);
            throw new IllegalArgumentException("Transfer failed: You can only transfer up to ₹" + remainingLimit + " today");
        }

        if (senderWallet.getFrozen()) {
            logger.warn("Sender wallet frozen. Cannot transfer.");
            throw new IllegalArgumentException("Wallet frozen due to daily limit reached. Cannot transfer.");
        }

        if (senderWallet.getBalance() < amount) {
            logger.warn("Insufficient balance for sender {}. Balance={}, requested={}", sender.getEmail(), senderWallet.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient balance");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("Recipient not found with ID " + recipientId));

        Wallet recipientWallet = getWallet(recipient);
        resetDailyIfNewDay(recipientWallet);

        // Update balances
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        senderWallet.setDailySpent(senderWallet.getDailySpent() + amount);
        if (senderWallet.getDailySpent() >= walletProperties.getDailyLimit()) {
            senderWallet.setFrozen(true);
            logger.info("Sender {} reached daily limit. Wallet frozen.", sender.getEmail());
        }

        recipientWallet.setBalance(recipientWallet.getBalance() + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        transactionRepository.save(new Transaction(sender, amount, "DEBIT"));
        transactionRepository.save(new Transaction(recipient, amount, "CREDIT"));

        logger.info("Transfer successful: sender {} newBalance={}, recipient {} newBalance={}",
                sender.getEmail(), senderWallet.getBalance(), recipient.getEmail(), recipientWallet.getBalance());

        TransferResponse response = walletMapper.toTransferResponse(senderWallet);
        response.setAmountTransferred(amount);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - senderWallet.getDailySpent());
        response.setFrozen(senderWallet.getFrozen());
        response.setMessage("Transfer successful");

        return response;
    }

    // ---------------- Helper Methods ----------------
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public List<TransactionDTO> getTransactions(User user) {
        logger.info("Fetching transactions for user {}", user.getEmail());
        List<Transaction> transactions = transactionRepository.findByUser(user);
        List<TransactionDTO> dtos = transactions.stream().map(transactionMapper::toDTO).collect(Collectors.toList());
        logger.info("Fetched {} transactions for user {}", dtos.size(), user.getEmail());
        return dtos;
    }

    public LoadMoneyResponse toLoadMoneyResponse(Wallet wallet) {
        LoadMoneyResponse response = walletMapper.toLoadMoneyResponse(wallet);
        response.setRemainingDailyLimit(walletProperties.getDailyLimit() - wallet.getDailySpent());
        response.setFrozen(wallet.getFrozen());
        return response;
    }

    public WalletProperties getWalletProperties() {
        return walletProperties;
    }
}