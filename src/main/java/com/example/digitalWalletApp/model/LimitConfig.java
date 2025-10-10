package com.example.digitalWalletApp.model;

import jakarta.persistence.*;

@Entity
public class LimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double dailyLimit = 100000;   // default
    private double monthlyLimit = 500000; // default

    // getters & setters
    public double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(double dailyLimit) { this.dailyLimit = dailyLimit; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
