package com.example.digitalWalletApp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wallet.transaction")
public class WalletProperties {

    private double minAmount;
    private double maxAmount;

    public double getMinAmount() { return minAmount; }
    public void setMinAmount(double minAmount) { this.minAmount = minAmount; }

    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }
}
