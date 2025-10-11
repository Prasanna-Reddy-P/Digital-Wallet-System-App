package com.example.digitalWalletApp.service;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // Get or create wallet
    public Wallet getWallet(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> walletRepository.save(new Wallet(user)));
    }

    // Load money
    @Transactional
    public Map<String, Object> loadMoney(User user, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");

        Wallet wallet = getWallet(user);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new Transaction(user, amount, "SELF_CREDITED"));

        return Map.of(
                "message", "Wallet loaded successfully",
                "balance", wallet.getBalance()
        );
    }

    // Transfer money
    @Transactional
    public Map<String, Object> transferAmount(User sender, Long receiverId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        Wallet senderWallet = getWallet(sender);
        Wallet receiverWallet = getWallet(receiver);

        if (senderWallet.getBalance() < amount)
            throw new IllegalArgumentException("Insufficient balance");

        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        transactionRepository.save(new Transaction(sender, -amount, "DEBIT"));
        transactionRepository.save(new Transaction(receiver, amount, "CREDIT"));

        return Map.of(
                "message", "Transfer successful",
                "balance", senderWallet.getBalance()
        );
    }

    // Get transactions
    public List<Transaction> getTransactions(User user) {
        return transactionRepository.findByUser(user);
    }
}
