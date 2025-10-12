package com.example.digitalWalletApp.service;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // Get wallet; create new if not exists
    public Wallet getWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(user);
                    newWallet.setBalance(0.0);
                    return walletRepository.save(newWallet);
                });
    }

    // Load money into wallet
    public Map<String, Object> loadMoney(User user, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        Wallet wallet = getWallet(user);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType("CREDIT");
        transactionRepository.save(transaction);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Wallet loaded successfully");
        response.put("balance", wallet.getBalance());
        return response;
    }

    // Transfer amount to another user
    public Map<String, Object> transferAmount(User sender, Long recipientId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        Optional<User> recipientOpt = userRepository.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            throw new IllegalArgumentException("Recipient not found");
        }

        Wallet senderWallet = getWallet(sender);
        if (senderWallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        User recipient = recipientOpt.get();
        Wallet recipientWallet = getWallet(recipient);

        // Update balances
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        recipientWallet.setBalance(recipientWallet.getBalance() + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        // Create transactions
        Transaction debit = new Transaction();
        debit.setUser(sender);
        debit.setAmount(amount);
        debit.setType("DEBIT");
        transactionRepository.save(debit);

        Transaction credit = new Transaction();
        credit.setUser(recipient);
        credit.setAmount(amount);
        credit.setType("CREDIT");
        transactionRepository.save(credit);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transfer successful");
        response.put("senderBalance", senderWallet.getBalance());
        response.put("recipientBalance", recipientWallet.getBalance());
        return response;
    }

    // Get all transactions for user
    public List<Transaction> getTransactions(User user) {
        return transactionRepository.findByUser(user);
    }
}
