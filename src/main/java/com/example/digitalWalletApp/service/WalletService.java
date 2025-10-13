package com.example.digitalWalletApp.service;

import com.example.digitalWalletApp.config.WalletProperties;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

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

    public WalletProperties getWalletProperties() {  // <-- add getter
        return walletProperties;
    }

    // Get wallet; create new if not exists
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

    // --- Helper method to validate transaction amount ---
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

    // Load money into wallet
    @Transactional
    public Map<String, Object> loadMoney(User user, double amount) {
        logger.info("Initiating wallet load request for user {} with amount {}", user.getEmail(), amount);

        validateAmount(amount, "load"); // ✅ validate dynamically

        Wallet wallet = getWallet(user);
        double oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance + amount);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction(user, amount, "CREDIT");
        transactionRepository.save(transaction);

        logger.info("Wallet load successful for user {}: oldBalance={}, newBalance={}, transactionId={}",
                user.getEmail(), oldBalance, wallet.getBalance(), transaction.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Wallet loaded successfully");
        response.put("balance", wallet.getBalance());
        return response;
    }

    // Transfer amount to another user
    @Transactional
    public Map<String, Object> transferAmount(User sender, Long recipientId, double amount) {
        logger.info("Transfer initiated: sender={}, recipientId={}, amount={}",
                sender.getEmail(), recipientId, amount);

        validateAmount(amount, "transfer"); // ✅ validate dynamically

        Optional<User> recipientOpt = userRepository.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            logger.error("Transfer failed: recipient with ID {} not found", recipientId);
            throw new IllegalArgumentException("Recipient not found");
        }

        Wallet senderWallet = getWallet(sender);
        if (senderWallet.getBalance() < amount) {
            logger.warn("Transfer failed: Insufficient balance for user {}. Balance={}, requested={}",
                    sender.getEmail(), senderWallet.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient balance");
        }

        User recipient = recipientOpt.get();
        Wallet recipientWallet = getWallet(recipient);

        double senderOldBalance = senderWallet.getBalance();
        double recipientOldBalance = recipientWallet.getBalance();

        senderWallet.setBalance(senderOldBalance - amount);
        recipientWallet.setBalance(recipientOldBalance + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        Transaction debit = new Transaction(sender, amount, "DEBIT");
        Transaction credit = new Transaction(recipient, amount, "CREDIT");
        transactionRepository.save(debit);
        transactionRepository.save(credit);

        logger.info("Transfer successful: sender={} ({} -> {}), recipient={} ({} -> {}), amount={}, debitTxId={}, creditTxId={}",
                sender.getEmail(), senderOldBalance, senderWallet.getBalance(),
                recipient.getEmail(), recipientOldBalance, recipientWallet.getBalance(),
                amount, debit.getId(), credit.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transfer successful");
        response.put("senderBalance", senderWallet.getBalance());
        response.put("recipientBalance", recipientWallet.getBalance());
        return response;
    }

    // Get all transactions for user
    public List<Transaction> getTransactions(User user) {
        logger.info("Fetching all transactions for user {}", user.getEmail());
        List<Transaction> transactions = transactionRepository.findByUser(user);
        logger.debug("Fetched {} transactions for user {}", transactions.size(), user.getEmail());
        return transactions;
    }
}
