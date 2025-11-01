package com.example.digitalWalletApp.service.wallet;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.model.Wallet;
import com.example.digitalWalletApp.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WalletFactory {
    private static final Logger log = LoggerFactory.getLogger(WalletFactory.class);
    private final WalletRepository walletRepository;

    public WalletFactory(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> {
            log.info("🪙 Creating wallet for new user {}", user.getEmail());
            Wallet wallet = new Wallet(user);
            wallet.setBalance(0.0);
            wallet.setDailySpent(0.0);
            wallet.setFrozen(false);
            wallet.setLastTransactionDate(LocalDate.now());
            return walletRepository.save(wallet);
        });
    }
}
