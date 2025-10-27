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
    private User user; // ✅ FOREIGN KEY (references user.id), @JoinColumn(name = "user_id"): Creates a column in wallet table called user_id → foreign key to user.id.

    // --- Daily limit tracking ---
    private Double dailySpent = 0.0;  // How much user has spent today
    private Boolean frozen = false;    // Is wallet frozen for outgoing transactions
    private LocalDate lastTransactionDate; // To reset dailySpent on a new day

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

    // --- Helper method to reset dailySpent if a new day starts ---
    /*
    Runs when user tries a new transaction.
    Resets dailySpent automatically if the day has changed.
    Keeps wallet consistent with time.
     */
    public void resetDailyIfNewDay() {
        LocalDate today = LocalDate.now();
        if (lastTransactionDate == null || !lastTransactionDate.equals(today)) {
            this.dailySpent = 0.0;
            this.frozen = false;  // Unfreeze at start of new day
            this.lastTransactionDate = today;
        }
    }
}