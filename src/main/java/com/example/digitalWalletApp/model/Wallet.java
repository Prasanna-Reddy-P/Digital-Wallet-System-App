package com.example.digitalWalletApp.model;

import jakarta.persistence.*;

@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double balance;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private double dailyLimit;   // will be set from LimitConfig
    private double monthlyLimit; // will be set from LimitConfig

    @Column(nullable = false)
    private boolean frozen = false; // ✅ add this

    public Wallet() {}

    public Wallet(User user) {
        this.user = user;
        this.balance = 0.0;
        this.frozen = false; // ✅ ensure default
    }

    // ---------------- Getters & Setters ----------------
    public Long getId() { return id; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(double dailyLimit) { this.dailyLimit = dailyLimit; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
}
