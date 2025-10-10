package com.example.digitalWalletApp.service;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.model.LimitConfig;
import com.example.digitalWalletApp.repository.TransactionRepository;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.repository.WalletRepository;
import com.example.digitalWalletApp.repository.LimitConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LimitConfigRepository limitConfigRepository;

    // ---------------- Get Wallet ----------------
    public Wallet getWallet(User user) {
        LimitConfig globalLimits = limitConfigRepository.findTopByOrderByIdAsc()
                .orElse(new LimitConfig());

        Wallet wallet = walletRepository.findByUser(user).orElse(new Wallet(user));

        // Apply global limits
        boolean changed = false;
        if (wallet.getDailyLimit() != globalLimits.getDailyLimit()) {
            wallet.setDailyLimit(globalLimits.getDailyLimit());
            changed = true;
        }
        if (wallet.getMonthlyLimit() != globalLimits.getMonthlyLimit()) {
            wallet.setMonthlyLimit(globalLimits.getMonthlyLimit());
            changed = true;
        }
        if (changed || wallet.getId() == null) walletRepository.save(wallet);

        return wallet;
    }

    // ---------------- Balance & Transactions ----------------
    public double getBalance(User user) {
        return getWallet(user).getBalance();
    }

    public List<Transaction> getTransactions(User user) {
        return transactionRepository.findByUser(user);
    }

    // ---------------- Load Money ----------------
    public void loadMoney(User user, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");

        Wallet wallet = getWallet(user);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        double todayTotal = transactionRepository
                .findByUserAndTimestampBetween(user, startOfDay, endOfDay)
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(23, 59, 59);
        double monthTotal = transactionRepository
                .findByUserAndTimestampBetween(user, monthStart, monthEnd)
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        if (todayTotal + amount > wallet.getDailyLimit()) {
            throw new IllegalArgumentException("Daily limit exceeded!");
        }
        if (monthTotal + amount > wallet.getMonthlyLimit()) {
            throw new IllegalArgumentException("Monthly limit exceeded!");
        }

        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
        transactionRepository.save(new Transaction(user, amount, "SELF_CREDITED"));
    }

    // ---------------- Transfer Money ----------------
    public void transferAmount(User sender, Long receiverId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        Wallet senderWallet = getWallet(sender);
        Wallet receiverWallet = getWallet(receiver);

        if (senderWallet.getBalance() < amount)
            throw new IllegalArgumentException("Insufficient balance");

        // Daily and monthly total check for sender
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        double todayTotal = transactionRepository
                .findByUserAndTimestampBetween(sender, startOfDay, endOfDay)
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(23, 59, 59);
        double monthTotal = transactionRepository
                .findByUserAndTimestampBetween(sender, monthStart, monthEnd)
                .stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        if (todayTotal + amount > senderWallet.getDailyLimit()) {
            throw new IllegalArgumentException("Daily limit exceeded!");
        }
        if (monthTotal + amount > senderWallet.getMonthlyLimit()) {
            throw new IllegalArgumentException("Monthly limit exceeded!");
        }

        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        transactionRepository.save(new Transaction(sender, -amount, "DEBIT"));
        transactionRepository.save(new Transaction(receiver, amount, "CREDIT"));
    }

    // ---------------- Global Limits ----------------
    public void setGlobalLimits(double dailyLimit, double monthlyLimit) {
        LimitConfig globalLimits = limitConfigRepository.findTopByOrderByIdAsc()
                .orElse(new LimitConfig());

        globalLimits.setDailyLimit(dailyLimit);
        globalLimits.setMonthlyLimit(monthlyLimit);
        limitConfigRepository.save(globalLimits);

        // Apply global limits to all wallets
        List<Wallet> wallets = walletRepository.findAll();
        for (Wallet w : wallets) {
            w.setDailyLimit(dailyLimit);
            w.setMonthlyLimit(monthlyLimit);
        }
        walletRepository.saveAll(wallets);
    }

    public LimitConfig getGlobalLimits() {
        return limitConfigRepository.findTopByOrderByIdAsc()
                .orElse(new LimitConfig());
    }
}
