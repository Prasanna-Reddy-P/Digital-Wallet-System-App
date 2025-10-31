package com.example.digitalWalletApp.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "`wallet`")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary Key

    private Double balance;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double dailySpent = 0.0;
    private Boolean frozen = false;
    private LocalDate lastTransactionDate;

    // ✅ Enable Hibernate Optimistic Locking
    @Version
    @Column(nullable = false)
    private Long version = 0L; // initialize to avoid null

    public Wallet() {}

    public Wallet(User user) {
        this.user = user;
        this.balance = 0.0;
        this.dailySpent = 0.0;
        this.frozen = false;
        this.lastTransactionDate = LocalDate.now();
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getDailySpent() {
        return dailySpent;
    }

    public void setDailySpent(Double dailySpent) {
        this.dailySpent = dailySpent;
    }

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

    public LocalDate getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDate lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // --- Helper ---
    public void resetDailyIfNewDay() {
        LocalDate today = LocalDate.now();
        if (lastTransactionDate == null || !lastTransactionDate.equals(today)) {
            this.dailySpent = 0.0;
            this.frozen = false;
            this.lastTransactionDate = today;
        }
    }
}