package com.example.digitalWalletApp.service.wallet;

import com.example.digitalWalletApp.config.WalletProperties;
import com.example.digitalWalletApp.model.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletValidator {

    private final WalletProperties walletProperties;

    public WalletValidator(WalletProperties walletProperties) {
        this.walletProperties = walletProperties;
    }

    public void validateAmount(double amount, String operation) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");
        if (amount < walletProperties.getMinAmount() || amount > walletProperties.getMaxAmount())
            throw new IllegalArgumentException(operation + " amount must be between "
                    + walletProperties.getMinAmount() + " and " + walletProperties.getMaxAmount());
    }

    public void validateDailyLimit(Wallet wallet, double amount) {
        double remaining = walletProperties.getDailyLimit() - wallet.getDailySpent();
        if (amount > remaining)
            throw new IllegalArgumentException("Daily limit exceeded");
    }

    public void validateFrozen(Wallet wallet) {
        if (wallet.getFrozen())
            throw new IllegalArgumentException("Wallet frozen. Cannot proceed.");
    }

    public void validateBalance(Wallet wallet, double amount) {
        if (wallet.getBalance() < amount)
            throw new IllegalArgumentException("Insufficient balance");
    }
}
