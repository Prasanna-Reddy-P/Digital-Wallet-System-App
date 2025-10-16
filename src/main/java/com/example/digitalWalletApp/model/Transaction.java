package com.example.digitalWalletApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "`transactions`")// good practice to use plural table name
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ✅ PRIMARY KEY

    private Double amount;

    private String type; // "DEBIT" or "CREDIT"

    private LocalDateTime timestamp;

    @ManyToOne // multiple transactions can happen per user (N : 1)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // ✅ FOREIGN KEY (references user.id)

    // --- Constructors ---
    public Transaction() {}

    public Transaction(User user, Double amount, String type) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // --- Lifecycle callback ---
    @PrePersist // @PrePersist: Called before saving this entity to the DB.
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}